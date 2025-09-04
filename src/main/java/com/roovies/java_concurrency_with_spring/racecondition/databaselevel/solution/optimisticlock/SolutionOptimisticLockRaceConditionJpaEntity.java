package com.roovies.java_concurrency_with_spring.racecondition.databaselevel.solution.optimisticlock;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stocks")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SolutionOptimisticLockRaceConditionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 낙관적 락을 적용해서, version을 관리할 필드
     * -> 일반적으로 큰 범위로 두는 것이 좋으므로 Long 사용
     * 
     * @Version 어노테이션 (import jakarta.persistence.Version)
     * - 엔티티 객체가 데이터베이스에 저장될 때마다, 이 필드의 값이 자동으로 증가됨
     * - 이 값을 기준으로 동시에 여러 트랜잭션이 같은 데이터를 수정할 때 충돌 여부를 감지해줌
     * - 충돌 발생 시
     *      -> JPA의 경우 OptimisticLockException 발생
     *      -> Spring Data JPA의 경우  JPA를 래핑하고, 예외를 스프링 예외로 변환해서 ObjectOptimisticLockingFailureException 발생
     * - 해당 예외를 기준으로 재시도 or 사용자 알림 or 롤백(기본 제공) 등 부가 로직 구현 필요
     */
    @Version
    private Long version;

    public SolutionOptimisticLockRaceConditionJpaEntity(String productName, Integer quantity) {
        this.productName = productName;
        this.quantity = quantity;
    }

    public void decrease(int amount) {
        if (this.quantity < amount)
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + this.quantity);

        this.quantity -= amount;
    }
}
