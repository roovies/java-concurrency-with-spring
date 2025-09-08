package com.roovies.java_concurrency_with_spring.racecondition.databaselevel.problem;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UnSafeDBLevelRaceConditionService {

    private final UnSafeDBLevelRaceConditionRepository unSafeDBLevelRaceConditionRepository;

    /**
     * 레이스 컨디션이 발생하는 메서드
     * - 문제점: 여러 쓰레드가 동시에 같은 재고를 조회하고 수정할 때, 데이터 일관성이 깨짐
     */
    public void decreaseStock(String productName, int amount) {
        // 1. 재고 조회 (여러 쓰레드가 동시에 같은 값을 읽음)
        UnSafeDBLevelRaceConditionJpaEntity unSafeDBLevelRaceConditionJpaEntity = unSafeDBLevelRaceConditionRepository.findByProductName(productName)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productName));

        // 2. 재고 감소 수행 (레이스 컨디션 발생 구간)
        // 이 시점에서 다른 쓰레드가 이미 재고를 변경했을 수도 있음
        unSafeDBLevelRaceConditionJpaEntity.decrease(amount);

        // 3. 감소된 재고 저장
        // 나중에 실행된 트랜잭션이 먼저 실행된 트랜잭션의 결과를 덮어쓰게 됨
        unSafeDBLevelRaceConditionRepository.save(unSafeDBLevelRaceConditionJpaEntity);
    }

    /**
     * 재고 초기화 메서드
     */
    public void initializeStock(String productName, int quantity) {
        UnSafeDBLevelRaceConditionJpaEntity unSafeDBLevelRaceConditionJpaEntity = new UnSafeDBLevelRaceConditionJpaEntity(productName, quantity);
        unSafeDBLevelRaceConditionRepository.save(unSafeDBLevelRaceConditionJpaEntity);
    }

    /**
     * 현재 재고 조회 메서드
     */
    @Transactional(readOnly = true)
    public int getCurrentQuantity(String productName) {
        return unSafeDBLevelRaceConditionRepository.findByProductName(productName)
                .map(stock -> stock.getQuantity()) // Optional의 map: Optional 안의 값을 변환할 때 사용
                .orElse(0); // Empty일 경우 기본값을 반환 => 0
    }
}
