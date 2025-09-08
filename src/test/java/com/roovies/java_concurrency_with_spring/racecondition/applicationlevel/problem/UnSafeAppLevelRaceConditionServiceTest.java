package com.roovies.java_concurrency_with_spring.racecondition.applicationlevel.problem;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class UnSafeAppLevelRaceConditionServiceTest {

    private final UnSafeAppLevelRaceConditionService unSafeAppLevelRaceConditionService = new UnSafeAppLevelRaceConditionService();

    @Test
    void 레이스_컨디션_문제_재현() throws InterruptedException {
        /* given: 초기 재고 100개 */
        String productName = "갤럭시 폴드7";
        int initialQuantity = 1000;
        unSafeAppLevelRaceConditionService.initializeStock(1L, productName, initialQuantity);

        /* when: 1000개의 쓰레드가 동시에 1개씩 재고 감소를 요청함 */
        int threadCount = 1000;

        // 1. 고정된 크기의 쓰레드 풀 생성 (최대 threadCount개의 쓰레드가 동시에 동작 가능)
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 2. CountDownLatch 생성 (threadCount만큼 작업이 끝날 때까지 대기할 수 있는 도구)
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // 3. 재고 1개씩 감소
                    unSafeAppLevelRaceConditionService.decreaseStock(productName, 1);
                } catch (Exception e) {
                    System.err.println("예외 발생: " + e.getMessage());
                } finally {
                    // 4. 쓰레드 작업이 끝나면 latch 1 감소시킴
                    // -> 100개 쓰레드 모두 종료되면, latch 값이 0이 되어 다음 코드 진행 가능
                    latch.countDown();
                }
            });
        }
        latch.await(); // 모든 쓰레드 작업이 끝날 때까지 블로킹
        executor.shutdown(); // 쓰레드 풀 종료

        /* then: 예상 결과는 0이어야 하지만 Race Condition 때문에 0이 아닐 가능성이 높음 */
        int finalQuantity = unSafeAppLevelRaceConditionService.getCurrentQuantity(productName);
        // 최종 재고 확인
        System.out.println("최종 재고: " + finalQuantity);
        System.out.println("예상 재고: 0");
        System.out.println("손실된 재고: " + finalQuantity);

        // 레이스 컨디션이 발생했다면 재고가 0이 아닐 것임
        if (finalQuantity > 0) {
            System.out.println("⚠️ 레이스 컨디션 발생! 데이터 일관성이 깨졌습니다.");
        } else {
            System.out.println("✅ 이번에는 운 좋게 레이스 컨디션이 발생하지 않았습니다.");
        }
        assertThat(finalQuantity).isNotEqualTo(0);
    }
}
