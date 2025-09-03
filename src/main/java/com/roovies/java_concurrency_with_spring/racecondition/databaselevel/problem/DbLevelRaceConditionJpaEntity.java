package com.roovies.java_concurrency_with_spring.racecondition.databaselevel.problem;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stocks")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DbLevelRaceConditionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    public DbLevelRaceConditionJpaEntity(String productName, Integer quantity) {
        this.productName = productName;
        this.quantity = quantity;
    }

    public void decrease(int amount) {
        if (this.quantity < amount)
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + this.quantity);

        this.quantity -= amount;
    }
}
