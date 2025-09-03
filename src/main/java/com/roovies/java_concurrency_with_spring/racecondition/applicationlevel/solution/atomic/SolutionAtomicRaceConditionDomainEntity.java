package com.roovies.java_concurrency_with_spring.racecondition.applicationlevel.solution.atomic;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicInteger;

@NoArgsConstructor
@Getter
public class SolutionAtomicRaceConditionDomainEntity {
    private Long id;
    private String productName;
    // AtomicInteger: 멀티스레드 환경에서도 안전하게 값을 증가/감소할 수 있는 원자적 변수
    private AtomicInteger quantity = new AtomicInteger();

    public SolutionAtomicRaceConditionDomainEntity(Long id, String productName, int quantity) {
        this.id = id;
        this.productName = productName;
        this.quantity = new AtomicInteger(quantity);
    }

    public void decrease(int amount) {
        int oldValue, newValue;

        // CAS 연산이 성공할 때까지 반복
        // - 다른 스레드가 동시에 값을 변경하면 compareAndSet은 false를 반환
        // - 이 경우 원자적 연산에 실패했기 때문에 루프를 다시 수행하여 최신 값으로 재시도함
        // - 재시도하지 않을 경우, 다른 스레드가 값을 변경한 순간에 데이터 불일치가 발생하여
        //   잘못된 재고 감소가 이루어질 수 있음 (Lost Update 문제 발생)
        do {
            // 1. 현재 재고 값 읽기
            oldValue = quantity.get();

            // 2. 재고 부족 체크
            if (oldValue < amount)
                throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + this.quantity);

            // 3. 감소 후 값 계산
            newValue = oldValue - amount;

        // 4. CAS 시도
        // - oldValue와 현재 quantity가 같으면 newValue로 변경 후 true 반환
        // - 다르면 false 반환 → 다른 스레드가 먼저 변경한 것임 → 루프 반복
        } while (!quantity.compareAndSet(oldValue, newValue));
    }
}
