package com.roovies.java_concurrency_with_spring.racecondition.applicationlevel.solution.atomic;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

public class AtomicRaceConditionTest {

    private final AtomicStockService atomicStockService = new AtomicStockService();

    @Test
    void Atomic_자료형을_사용하면_연산을_원자적으로_처리하기_때문에_동시성_문제없이_재고가_정상_감소한다() throws InterruptedException {
        /* given: 초기 재고 1000개 */
        String productName = "갤럭시 폴드7";
        int initialQuantity = 1000;
        atomicStockService.initializeStock(1L, productName, initialQuantity);

        /* when: 1000개의 비동기 태스크가 동시에 실행됨 */
        int threadCount = 1000;
        List<CompletableFuture<Void>> futures =
                IntStream.range(0, threadCount)
                        .mapToObj(num -> CompletableFuture.runAsync(() -> {
                            try {
                                atomicStockService.decreaseStock(productName, 1);
                            } catch (Exception e) {
                                System.err.println("예외 발생: " + e.getMessage());
                            }
                        }))
                        .toList();

        // 모든 비동기 작업 완료 대기
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
        all.join();

        /* then: 최종 재고는 0이어야 함 */
        int finalQuantity = atomicStockService.getCurrentQuantity(productName);
        System.out.println("최종 재고: " + finalQuantity);
        System.out.println("예상 재고: 0");
        assertThat(finalQuantity).isEqualTo(0);
    }
}
