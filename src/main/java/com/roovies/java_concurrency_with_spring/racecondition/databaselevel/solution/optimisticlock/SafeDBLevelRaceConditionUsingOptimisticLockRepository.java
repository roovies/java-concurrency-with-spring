package com.roovies.java_concurrency_with_spring.racecondition.databaselevel.solution.optimisticlock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SafeDBLevelRaceConditionUsingOptimisticLockRepository extends JpaRepository<SafeDBLevelRaceConditionUsingOptimisticLockJpaEntity, Long> {

    Optional<SafeDBLevelRaceConditionUsingOptimisticLockJpaEntity> findByProductName(String productName);
}
