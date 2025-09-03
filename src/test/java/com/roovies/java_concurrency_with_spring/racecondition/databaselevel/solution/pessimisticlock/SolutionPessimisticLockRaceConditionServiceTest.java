package com.roovies.java_concurrency_with_spring.racecondition.databaselevel.solution.pessimisticlock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class SolutionPessimisticLockRaceConditionServiceTest {

    @Autowired
    private SolutionPessimisticLockRaceConditionService pessimisticLockService;

    @Autowired
    private SolutionPessimisticLockRaceConditionRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void 비관적_락을_적용하면_동시성_문제를_방지한다() {
        // given: 초기 재고 100개
        String productName = "아이폰15";
        int initialQuantity = 100;
        pessimisticLockService.initializeStock(productName, initialQuantity);

        // when: 100개의 비동기 작업 동시 실행
        int threadCount = 100;
        List<CompletableFuture<Void>> futures =
                IntStream.range(0, threadCount)
                        .mapToObj(n -> CompletableFuture.runAsync(() -> {
                            pessimisticLockService.decreaseStock(productName, 1);
                        }))
                        .toList();

        // 모든 작업이 수행될 때까지 대기
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
        all.join();

        // then: 최종 재고는 0이어야 함
        int finalQuantity = pessimisticLockService.getCurrentQuantity(productName);
        System.out.println("최종 재고: " + finalQuantity);
        System.out.println("예상 재고: 0");
        assertThat(finalQuantity).isEqualTo(0);
    }
}
