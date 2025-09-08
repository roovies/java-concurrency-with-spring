package com.roovies.java_concurrency_with_spring.racecondition.databaselevel.solution.optimisticlock;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SafeDBLevelRaceConditionUsingOptimisticLockService {

    private final SafeDBLevelRaceConditionUsingOptimisticLockRepository optimisticLockRepository;

    /**
     * 낙관적 락을 적용하여 동시성 문제를 해결함
     * - 별도의 version 컬럼을 통해 저장하려는 시점의 값과 저장된 값을 비교하여 충돌 감지
     *    (Atomic의 compareAndSet()과 유사 => CAS(Compare-And-Swap))
     * - 충돌 발생 시
     *      -> JPA의 경우 OptimisticLockException 발생
     *      -> Spring Data JPA의 경우  JPA를 래핑하고, 예외를 스프링 예외로 변환해서 ObjectOptimisticLockingFailureException 발생
     */
    @Transactional
    public void decreaseStock(String productName, int amount) {
        // 1. 조회 시점에는 DB 락을 걸지 않고 단순 조회 (엔티티와 version 정보 가져옴)
        SafeDBLevelRaceConditionUsingOptimisticLockJpaEntity entity = optimisticLockRepository.findByProductName(productName)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productName));

        // 2. 재고 감소
        entity.decrease(amount);

        // 3. flush 시점에 UPDATE ... WHERE version=? 실행 → 다른 트랜잭션이 이미 수정했으면 OptimisticLockException 발생
        optimisticLockRepository.save(entity);
    }

    /**
     * Spring Retry를 적용하여 낙관적 락 재시도 로직을 수행
     * - @Retryable 어노테이션을 통해 재시도할 예외 타입을 정의 및 세부 설정을 적용할 수 있다.
     * - maxAttempts만큼 재시도를 수행했지만 실패할 경우(예외가 발생할 경우) 마지막에 발생한 예외를 그대로 호출자한테 던진다.
     * - 만약 최종 실패 후 콜백 메서드를 정의하고 싶다면 @Recover를 사용하면 된다.
     */
    @Retryable(
            retryFor = {
                    ObjectOptimisticLockingFailureException.class,  // Spring Data JPA 낙관적 락 예외
                    OptimisticLockException.class                   // JPA 표준 낙관적 락 예외
            },
            maxAttempts = 3,            // 최대 3번 시도 (첫 시도 + 재시도 2번)
            backoff = @Backoff(         // 백오프 전략 (재시도 간격 설정)
                    delay = 100,        // - 첫 재시도 전 100ms 대기 (기본 대기 시간)
                    multiplier = 1.5,   // - 재시도마다 대기시간 1.5배 증가 (100ms -> 150ms -> 225ms)
                    maxDelay = 1000,    // - 최대 1초까지만 대기
                    random = true       // - 대기시간에 랜덤 요소 추가 (동시 재시도 충돌 방지) => true를 권장함
            )
    )
    @Transactional
    public void decreaseStockWithSpringRetry(String productName, int amount) {
        // 별도로 try-catch를 사용하지 않아도 되지만,
        // 로깅을 위해 catch로 예외를 잡고, 로깅 후 throw로 다시 예외를 던져서 Spring Retry가 처리하도록 한다. (실무 권장)
        try {
            // 1. 조회 시점에는 DB 락을 걸지 않고 단순 조회 (엔티티와 version 정보 가져옴)
            SafeDBLevelRaceConditionUsingOptimisticLockJpaEntity entity = optimisticLockRepository.findByProductName(productName)
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productName));

            // 2. 재고 감소
            entity.decrease(amount);

            // 3. flush 시점에 UPDATE ... WHERE version=? 실행 → 다른 트랜잭션이 이미 수정했으면 OptimisticLockException 발생
            optimisticLockRepository.save(entity);

            // 4. 로깅
            log.info("재고 감소 성공 - 상품: {}, 감소량: {}", productName, amount);
        } catch (ObjectOptimisticLockingFailureException e) {
            // Spring Retry가 자동으로 재시도를 처리하므로, 여기서는 로깅만
            System.out.println("낙관적 락 충돌 발생 - 재시도 진행 중... 상품: " + productName);
            throw e; // 예외를 다시 던져서 Spring Retry가 처리하도록 함
        } catch (OptimisticLockException e) {
            System.out.println("JPA 낙관적 락 충돌 발생 - 재시도 진행 중... 상품: " + productName);
            throw e;
        }
    }

    /**
     * 모든 낙관적 락 관련 예외를 한번에 처리하는 복구 메서드
     *
     * @Recover 메서드 작성 규칙:
     * 1. 메서드명은 자유롭게 작성 가능
     * 2. 첫 번째 파라미터는 처리할 예외 타입이어야 함
     * 3. 나머지 파라미터는 원본 메서드와 동일한 순서와 타입이어야 함
     * 4. 반환 타입은 원본 메서드와 동일해야 함 (void면 void)
     */
    @Recover
    public void recoverDecreaseStock(Exception ex, String productName, int amount) {
        // Exception으로 받으면 ObjectOptimisticLockingFailureException과 OptimisticLockException 모두 처리
        // 또는 인자로 Exception 상위 타입이 아닌 세밀하게 핸들링하고 싶은 예외로 선언하는 경우도 있음
        log.error("낙관적 락 관련 재시도 최종 실패 - 상품: {}, 감소량: {}, 예외타입: {}, 에러: {}",
                productName, amount, ex.getClass().getSimpleName(), ex.getMessage());
        // -> 로직에 IllegalArgumentException도 있던데 처리 안하는가?
        //    - @Retry가 감지하는 예외만 재시도를 처리하고, 그 재시도가 최종 실패할 경우 콜백이 실행되므로 IllegalArgumentException는 대상이 아님

        // instanceof로 예외별 세부 처리가 필요한 경우 (선택사항)
        if (ex instanceof ObjectOptimisticLockingFailureException) {
            // Spring Data JPA 예외 - 추가 Spring 컨텍스트 정보 활용 가능
            log.debug("Spring Data JPA 낙관적 락 실패: {}", ex.getMessage());
        } else if (ex instanceof OptimisticLockException) {
            // JPA 표준 예외 - 표준 JPA 정보 활용
            log.debug("JPA 표준 낙관적 락 실패: {}", ex.getMessage());
        }

        // 복구 전략 옵션들:
        // 1. 단순히 실패를 기록하고 예외를 던지지 않음 (무시)
        // → 호출한 쪽에서는 정상 처리된 것으로 인식

        // 2. 다른 방식으로 처리 (예: 비동기 큐에 재처리 요청 등록)
        // asyncStockService.scheduleRetry(productName, amount);

        // 3. 사용자 정의 예외 발생 (비즈니스 예외로 변환)
        // throw new StockDecrementFailedException("재고 감소 처리에 실패했습니다: " + productName);

        // 4. 알림 시스템 연동
        // alertService.sendOptimisticLockFailureAlert(productName, amount, ex);

        // 현재는 로깅만 하고 정상 종료 (실무에서는 상황에 맞게 선택)
        System.out.println("낙관적 락 실패를 복구 처리했습니다. 관리자 확인 필요: " + productName);
    }



    /**
     * 재고 초기화 메서드
     */
    public void initializeStock(String productName, int quantity) {
        SafeDBLevelRaceConditionUsingOptimisticLockJpaEntity entity = new SafeDBLevelRaceConditionUsingOptimisticLockJpaEntity(productName, quantity);
        optimisticLockRepository.save(entity);
    }

    /**
     * 현재 재고 조회 메서드
     */
    @Transactional(readOnly = true)
    public int getCurrentQuantity(String productName) {
        return optimisticLockRepository.findByProductName(productName)
                .map(stock -> stock.getQuantity()) // Optional의 map: Optional 안의 값을 변환할 때 사용
                .orElse(0); // Empty일 경우 기본값을 반환 => 0
    }
}
