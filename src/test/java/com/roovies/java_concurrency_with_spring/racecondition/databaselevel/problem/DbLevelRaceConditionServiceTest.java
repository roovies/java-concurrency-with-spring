package com.roovies.java_concurrency_with_spring.racecondition.databaselevel.problem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest // Spring Boot 통합 테스트를 실행하기 위한 어노테이션 => 스프링 컨테이너를 띄워서 테스트 수행
@ActiveProfiles("test") // 실행할 때 사용할 Spring 프로파일을 지정 => "test" 프로파일에 해당하는 환경설정을 적용해 테스트가 실행됨
public class DbLevelRaceConditionServiceTest {

    @Autowired
    private DbLevelRaceConditionService raceConditionService;

    @Autowired
    private DbLevelRaceConditionRepository dbLevelRaceConditionRepository;

    @BeforeEach
    void setUp() {
        dbLevelRaceConditionRepository.deleteAll();
    }

    @Test
    void 레이스_컨디션_문제_재현() throws InterruptedException {
        /* given: 초기 재고 100개 */
        String productName = "갤럭시 폴드7";
        int initialQuantity = 100;
        raceConditionService.initializeStock(productName, initialQuantity);

        /* when: 100개의 쓰레드가 동시에 1개씩 재고 감소를 요청함 */
        int threadCount = 100;

        // 1. 고정된 크기의 스레드 풀 생성. 최대 threadCount개의 스레드가 동시에 동작 가능
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        // 2. CountDownLatch 생성: threadCount 만큼 스레드가 완료될 때까지 기다릴 수 있도록 하는 도구
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // 3. 재고 1개씩 감소
                    raceConditionService.decreaseStock(productName, 1);
                } catch (Exception e) {
                    System.err.println("예외 발생: " + e.getMessage());
                } finally {
                    // 4. 쓰레드 작업이 끝나면 latch 1 감소시킴
                    // -> 100개 쓰레드 모두 종료되면, latch 값이 0이 되어 다음 코드 진행 가능
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 쓰레드가 종료될 때까지 블로킹
        executor.shutdown(); // 쓰레드 풀 종료

        /* then: 예상 결과는 0이어야 하지만, 레이스 컨디션으로 인해 0보다 큰 값이 나올 가능성이 높음 */
        int finalQuantity = raceConditionService.getCurrentQuantity(productName);
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
