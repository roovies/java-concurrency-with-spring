package com.roovies.java_concurrency_with_spring.racecondition.inmemory.service;

import com.roovies.java_concurrency_with_spring.racecondition.inmemory.entity.AtomicStock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AtomicStockService {
    /**
     * 인메모리에서 발생될 수 있는 Race Condition 문제를 재현하고
     * Atomic 자료형을 통해 연산을 원자적으로 처리하여 해결하는 로직
     */

    // 인메모리 저장소 (DB 대신 사용)
    private final Map<String, AtomicStock> stockStore = new HashMap<>();

    /*
     * 레이스 컨디션이 발생하는 메서드
     * - 문제점: 여러 쓰레드가 동시에 같은 재고를 조회하고 수정할 때 데이터 꼬임 발생
     */
    public void decreaseStock(String productName, int amount) {
        // 1. 재고 조회 (동시에 여러 쓰레드가 읽을 수 있음)
        AtomicStock stock = stockStore.get(productName);
        if (stock == null)
            throw new IllegalArgumentException("상품을 찾을 수 없습니다: " + productName);

        // 2. 레이스 컨디션 발생을 위한 의도적 지연
        try {
            Thread.sleep((long)(Math.random() * 5));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 3. 재고 감소 (여기서 동시성 문제가 발생할 수 있음)
        stock.decrease(amount);

        // 4. 저장 (실제로는 같은 객체 참조라 덮어쓰기 의미 없음)
        stockStore.put(productName, stock);
    }

    /*
     * 재고 초기화
     */
    public void initializeStock(Long id, String productName, int quantity) {
        stockStore.put(productName, new AtomicStock(id, productName, quantity));
    }

    /*
     * 현재 재고 조회
     */
    public int getCurrentQuantity(String productName) {
        AtomicStock stock = stockStore.get(productName);
        return stock != null ? stock.getQuantity().get() : 0;
    }
}
