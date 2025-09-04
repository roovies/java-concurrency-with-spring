package com.roovies.java_concurrency_with_spring.racecondition.databaselevel.solution.pessimisticlock;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SafeDBLevelRaceConditionUsingPessimisticLockRepository extends JpaRepository<SafeDBLevelRaceConditionUsingPessimisticLockJpaEntity, Long> {

//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    Optional<SolutionPessimisticLockRaceConditionJpaEntity> findByProductName(String productName);

    // 메서드명에 명시적으로 FOR UPDATE 구문을 넣고 싶을 경우 JPQL을 사용해야 함
    // - findBy 뒤에는 Entity의 필드명이 와야 하는데, findByProductNameForUpdate를 하게 되면 "ProductNameForUpdate"라는 필드명을 찾으려고 시도함
    //   (없으면 오류까지 발생)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM SafeDBLevelRaceConditionUsingPessimisticLockJpaEntity p WHERE p.productName = :productName")
    Optional<SafeDBLevelRaceConditionUsingPessimisticLockJpaEntity> findByProductNameForUpdate(String productName);

}
