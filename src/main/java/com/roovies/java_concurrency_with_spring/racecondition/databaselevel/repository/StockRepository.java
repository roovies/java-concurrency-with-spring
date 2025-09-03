package com.roovies.java_concurrency_with_spring.racecondition.databaselevel.repository;

import com.roovies.java_concurrency_with_spring.racecondition.databaselevel.problem.DbLevelRaceConditionStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<DbLevelRaceConditionStock, Long> {

    Optional<DbLevelRaceConditionStock> findByProductName(String productName);
}
