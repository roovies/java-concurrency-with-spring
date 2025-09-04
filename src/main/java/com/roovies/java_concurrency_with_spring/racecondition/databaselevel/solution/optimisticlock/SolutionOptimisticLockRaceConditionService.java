package com.roovies.java_concurrency_with_spring.racecondition.databaselevel.solution.optimisticlock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SolutionOptimisticLockRaceConditionService {

    private final SolutionOptimisticLockRaceConditionRepository optimisticLockRepository;

    /**
     * 낙관적 락을 적용하여 동시성 문제를 해결함
     * - 별도의 version 컬럼을 통해 저장하려는 시점의 값과 저장된 값을 비교하여 충돌 감지
     *    (Atomic의 compareAndSet()과 유사 => CAS(Compare-And-Swap))
     * - 충돌 발생 시
     *      -> JPA의 경우 OptimisticLockException 발생
     *      -> Spring Data JPA의 경우  JPA를 래핑하고, 예외를 스프링 예외로 변환해서 ObjectOptimisticLockingFailureException 발생
     */
    public void decreaseStock(String productName, int amount) {
        // 1. 조회 시점에는 DB 락을 걸지 않고 단순 조회 (엔티티와 version 정보 가져옴)
        SolutionOptimisticLockRaceConditionJpaEntity entity = optimisticLockRepository.findByProductName(productName)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productName));

        // 2. 재고 감소
        entity.decrease(amount);

        // 3. flush 시점에 UPDATE ... WHERE version=? 실행 → 다른 트랜잭션이 이미 수정했으면 OptimisticLockException 발생
        optimisticLockRepository.save(entity);
    }

    /**
     * 재고 초기화 메서드
     */
    public void initializeStock(String productName, int quantity) {
        SolutionOptimisticLockRaceConditionJpaEntity entity = new SolutionOptimisticLockRaceConditionJpaEntity(productName, quantity);
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
