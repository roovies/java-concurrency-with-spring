package com.roovies.java_concurrency_with_spring.racecondition.databaselevel.solution.optimisticlock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class SafeDBLevelRaceConditionUsingOptimisticLockServiceTest {

    @Autowired
    private SafeDBLevelRaceConditionUsingOptimisticLockService optimisticLockSafeService;

    @Autowired
    private SafeDBLevelRaceConditionUsingOptimisticLockRepository optimisticLockSafeRepository;

    @BeforeEach
    public void setUp() {
        optimisticLockSafeRepository.deleteAll();

    }

    @Test
    void 낙관적_락은_트랜잭션_충돌시_ObjectOptimisticLockingFailureException을_발생시킨다() {
        // given:
        // - 초기 재고 100개
        String productName = "아이폰15";
        int initialQuantity = 100;
        optimisticLockSafeService.initializeStock(productName, initialQuantity);

        // - Thread-safe하게 OptimisticLockException를 적재할 리스트 생성
        List<ObjectOptimisticLockingFailureException> exceptions = Collections.synchronizedList(new ArrayList<>());

        // then: 100개의 비동기 작업 동시 실행
        int threadCount = 100;
        List<CompletableFuture<Void>> futures =
                IntStream.range(0, threadCount)
                        .mapToObj(n -> CompletableFuture.runAsync(() -> {
                            try {
                                // 재고 감소 시도
                                optimisticLockSafeService.decreaseStock(productName, 1);
                            } catch (ObjectOptimisticLockingFailureException e) {
                                // 동시성 문제로 인한 트랜잭션 충돌 시 exceptions에 적재
                                exceptions.add(e);
                            } catch (IllegalArgumentException e) {
                                System.out.println(e.getMessage());
                            }
                        }))
                        .toList();

        // 모든 작업이 수행될 때까지 대기
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
        all.join();

        // then: OptimisticLockException이 발생하는지 확인
        System.out.println("예외 발생 개수: " + exceptions.size());
        assertThat(exceptions)
                .allMatch(e -> e instanceof ObjectOptimisticLockingFailureException);
    }
}
