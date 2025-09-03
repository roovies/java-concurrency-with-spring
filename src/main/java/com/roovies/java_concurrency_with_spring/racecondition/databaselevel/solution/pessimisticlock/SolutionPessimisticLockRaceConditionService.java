package com.roovies.java_concurrency_with_spring.racecondition.databaselevel.solution.pessimisticlock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class SolutionPessimisticLockRaceConditionService {

    private final SolutionPessimisticLockRaceConditionRepository raceConditionRepository;

    /**
     * 비관적 락을 적용하여 동시성 문제를 해결함 => @Lock(LockModeType.PESSIMISTIC_WRITE) 추가
     */
    public void decreaseStock(String productName, int amount) {
        // 1. 조회 시점에 DB row-level exclusive lock 획득
        SolutionPessimisticLockRaceConditionJpaEntity entity = raceConditionRepository.findByProductNameForUpdate(productName)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productName));

        // 2. 안전하게 재고 감소
        entity.decrease(amount);

        // 3. JPA 특성상 Dirty-checking을 수행하지만, 코드 명시적 흐름을 위해 작성
        raceConditionRepository.save(entity);
    }

    /**
     * 재고 초기화 메서드
     */
    public void initializeStock(String productName, int quantity) {
        SolutionPessimisticLockRaceConditionJpaEntity entity = new SolutionPessimisticLockRaceConditionJpaEntity(productName, quantity);
        raceConditionRepository.save(entity);
    }

    /**
     * 현재 재고 조회 메서드
     */
    @Transactional(readOnly = true)
    public int getCurrentQuantity(String productName) {
        return raceConditionRepository.findByProductNameForUpdate(productName)
                .map(stock -> stock.getQuantity()) // Optional의 map: Optional 안의 값을 변환할 때 사용
                .orElse(0); // Empty일 경우 기본값을 반환 => 0
    }
}
