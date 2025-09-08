package com.roovies.java_concurrency_with_spring.racecondition.databaselevel.problem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnSafeDBLevelRaceConditionRepository extends JpaRepository<UnSafeDBLevelRaceConditionJpaEntity, Long> {

    Optional<UnSafeDBLevelRaceConditionJpaEntity> findByProductName(String productName);
}
