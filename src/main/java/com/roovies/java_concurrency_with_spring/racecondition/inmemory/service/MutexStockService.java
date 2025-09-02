package com.roovies.java_concurrency_with_spring.racecondition.inmemory.service;

import com.roovies.java_concurrency_with_spring.racecondition.db.entity.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class MutexStockService {
    /**
     * 인메모리에서 발생될 수 있는 Race Condition 문제를 재현하고
     * Mutex 기법(synchronized, ReentrantLock)을 통해 해결하는 로직을 기술함
     */


    // 인메모리 저장소 (DB 대신 사용)
    private final Map<String, Stock> stockStore = new HashMap<>();

    /*
     * 레이스 컨디션이 발생하는 메서드
     * - 문제점: 여러 쓰레드가 동시에 같은 재고를 조회하고 수정할 때 데이터 꼬임 발생
     */
    public void decreaseStock(String productName, int amount) {
        // 1. 재고 조회 (동시에 여러 쓰레드가 읽을 수 있음)
        Stock stock = stockStore.get(productName);
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
     * 해결 방법1) 메서드에 synchronized 적용
     */
    public synchronized void decreaseStockWithMethodSynchronized(String productName, int amount) {
        /**
         * 해당 메서드를 호출하는 시점에 락이 걸리기 때문에, 처리 속도가 느려질 수밖에 없다.
         */
        Stock stock = stockStore.get(productName);
        if (stock == null)
            throw new IllegalArgumentException("상품을 찾을 수 없습니다: " + productName);

        try {
            Thread.sleep((long)(Math.random() * 5));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        stock.decrease(amount);
        stockStore.put(productName, stock);
    }

    /*
     * 해결 방법2) 특정 객체에 synchronized 적용
     */
    public void decreaseStockWithObjectSynchronized(String productName, int amount) {
        Stock stock = stockStore.get(productName);
        if (stock == null)
            throw new IllegalArgumentException("상품을 찾을 수 없습니다: " + productName);

        try {
            Thread.sleep((long)(Math.random() * 5));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        /*
         * 메서드 전체에 락을 거는 대신,
         * 개별 상품 객체(Stock)에 대해서만 synchronized 블록을 적용한다.
         * → 동일한 productName을 가진 요청은 순차적으로 처리되지만,
         *   서로 다른 productName(즉, 다른 Stock 객체)에 대해서는 병렬 처리가 가능하다.
         * → 따라서 메서드 단위 synchronized보다 성능 손실이 적다.
         */
        synchronized (stock) {
            stock.decrease(amount);
        }
        stockStore.put(productName, stock);
    }

    /*
     * 해결 방법3) 메서드 전체에 ReentrantLock 적용
     * - 메서드에 synchronized 붙이는 거와 동일한 성능
     */
    // 전체 상품에 공통으로 적용할 락
    private final ReentrantLock globalLock = new ReentrantLock();
    public void decreaseStockWithMethodReentrantLock(String productName, int amount) {
        globalLock.lock(); // 락 획득
        try {
            Stock stock = stockStore.get(productName);
            if (stock == null)
                throw new IllegalArgumentException("상품을 찾을 수 없습니다: " + productName);

            try {
                Thread.sleep((long)(Math.random() * 5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            stock.decrease(amount);
            stockStore.put(productName, stock);

        } finally {
            globalLock.unlock(); // 락 해제 (finally로 보장)
        }
    }

    /*
     * 해결 방법4) 상품별로 ReentrantLock을 관리
     * → productName 단위로 병렬 처리 가능
     */
    private final Map<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();
    public void decreaseStockWithObjectReentrantLock(String productName, int amount) {
        // 상품별 락 가져오기 (없으면 생성)
        ReentrantLock productLock = lockMap.computeIfAbsent(productName, key -> new ReentrantLock());

        // 특정 상품에 대해서만 락 수행
        productLock.lock();
        try {
            Stock stock = stockStore.get(productName);
            if (stock == null)
                throw new IllegalArgumentException("상품을 찾을 수 없습니다: " + productName);

            try {
                Thread.sleep((long)(Math.random() * 5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            stock.decrease(amount);
            stockStore.put(productName, stock);

        } finally {
            productLock.unlock(); // 🔓 락 해제
        }
    }

    /*
     * 재고 초기화
     */
    public void initializeStock(Long id, String productName, int quantity) {
        stockStore.put(productName, new Stock(id, productName, quantity));
    }

    /*
     * 현재 재고 조회
     */
    public int getCurrentQuantity(String productName) {
        Stock stock = stockStore.get(productName);
        return stock != null ? stock.getQuantity() : 0;
    }
}
