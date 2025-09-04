package com.roovies.java_concurrency_with_spring.racecondition.applicationlevel.solution.mutex;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class SafeAppLevelRaceConditionUsingMutexServiceTest {

    private final SafeAppLevelRaceConditionUsingMutexService mutexSafeService = new SafeAppLevelRaceConditionUsingMutexService();

    @Test
    void 메서드에_synchronized를_적용하면_성능은_느리지만_메서드_호출시에_락이_걸려_레이스_컨디션을_방지한다() throws InterruptedException {
        /* given: 초기 재고 100개 */
        String productName = "갤럭시 폴드7";
        int initialQuantity = 1000;
        mutexSafeService.initializeStock(1L, productName, initialQuantity);

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
                    mutexSafeService.decreaseStockWithMethodSynchronized(productName, 1);
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
        int finalQuantity = mutexSafeService.getCurrentQuantity(productName);
        // 최종 재고 확인
        System.out.println("최종 재고: " + finalQuantity);
        System.out.println("예상 재고: 0");
        System.out.println("손실된 재고: " + finalQuantity);

        assertThat(finalQuantity).isEqualTo(0);
    }

    @Test
    void 객체_단위_synchronized를_적용하면_객체에만_락이_걸려_성능을_개선하고_레이스_컨디션을_방지한다() throws InterruptedException {
        /* given: 초기 재고 100개 */
        String productName = "갤럭시 폴드7";
        int initialQuantity = 1000;
        mutexSafeService.initializeStock(1L, productName, initialQuantity);

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
                    mutexSafeService.decreaseStockWithObjectSynchronized(productName, 1);
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
        int finalQuantity = mutexSafeService.getCurrentQuantity(productName);
        // 최종 재고 확인
        System.out.println("최종 재고: " + finalQuantity);
        System.out.println("예상 재고: 0");
        System.out.println("손실된 재고: " + finalQuantity);

        assertThat(finalQuantity).isEqualTo(0);
    }

    @Test
    void ReentrantLock을_전역으로_적용하면_메서드락과_유사하게_동작한다() throws InterruptedException {
        /* given: 초기 재고 1000개 */
        String productName = "갤럭시 폴드7";
        int initialQuantity = 1000;
        mutexSafeService.initializeStock(1L, productName, initialQuantity);

        /* when: 1000개의 비동기 작업이 동시에 실행됨 */
        int threadCount = 1000;
        List<CompletableFuture<Void>> futures =
                IntStream.range(0, threadCount)
                        .mapToObj(num -> CompletableFuture.runAsync(() -> {
                            try {
                                mutexSafeService.decreaseStockWithMethodReentrantLock(productName, 1);
                            } catch (Exception e) {
                                System.err.println("예외 발생: " + e.getMessage());
                            }
                        }))
                        .toList();

        // 모든 작업이 완료될 때까지 대기
        // List에 담긴 작업들을 allOf()에 넘기려면 배열 형태로 변환해야 함 => CompletableFuture<?>... cfs
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
        all.join();

        /* then */
        int finalQuantity = mutexSafeService.getCurrentQuantity(productName);
        System.out.println("최종 재고: " + finalQuantity);
        System.out.println("예상 재고: 0");
        assertThat(finalQuantity).isEqualTo(0);
    }

    @Test
    void ReentrantLock을_객체별로_적용하면_동일_객체에만_락이_걸리고_병렬성이_개선된다() throws InterruptedException {
        /* given: 초기 재고 1000개 */
        String productName = "갤럭시 폴드7";
        int initialQuantity = 1000;
        mutexSafeService.initializeStock(1L, productName, initialQuantity);

        /* when: 1000개의 비동기 태스크가 동시에 실행됨 */
        int threadCount = 1000;
        List<CompletableFuture<Void>> futures =
                IntStream.range(0, threadCount)
                        .mapToObj(num -> CompletableFuture.runAsync(() -> {
                            try {
                                mutexSafeService.decreaseStockWithObjectReentrantLock(productName, 1);
                            } catch (Exception e) {
                                System.err.println("예외 발생: " + e.getMessage());
                            }
                        }))
                        .toList();

        // 모든 비동기 작업이 완료될 때까지 대기
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
        all.join();

        /* then */
        int finalQuantity = mutexSafeService.getCurrentQuantity(productName);
        System.out.println("최종 재고: " + finalQuantity);
        System.out.println("예상 재고: 0");
        assertThat(finalQuantity).isEqualTo(0);
    }
}
