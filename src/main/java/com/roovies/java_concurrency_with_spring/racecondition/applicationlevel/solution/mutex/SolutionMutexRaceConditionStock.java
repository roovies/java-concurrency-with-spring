package com.roovies.java_concurrency_with_spring.racecondition.applicationlevel.solution.mutex;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SolutionMutexRaceConditionStock {
    private Long id;
    private String productName;
    private Integer quantity;

    public void decrease(int amount) {
        if (this.quantity < amount)
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + this.quantity);

        this.quantity -= amount;
    }
}
