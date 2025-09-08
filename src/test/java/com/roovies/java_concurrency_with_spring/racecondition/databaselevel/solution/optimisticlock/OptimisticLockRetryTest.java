package com.roovies.java_concurrency_with_spring.racecondition.databaselevel.solution.optimisticlock;


import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@SpringBootTest
@EnableRetry   // 반드시 필요! (Spring Retry 활성화)
class OptimisticLockRetryTest {

    @Autowired
    private SafeDBLevelRaceConditionUsingOptimisticLockService service;

    @MockitoBean
    private SafeDBLevelRaceConditionUsingOptimisticLockRepository repository;

    @Test
    void 트랜잭션_충돌_시_Sring_Retry가_정확히_3번_시도한다() {
        // given
        SafeDBLevelRaceConditionUsingOptimisticLockJpaEntity entity =
                new SafeDBLevelRaceConditionUsingOptimisticLockJpaEntity("아이폰15", 10);

        given(repository.findByProductName("아이폰15"))
                .willReturn(Optional.of(entity));
        given(repository.save(any()))
                .willThrow(new ObjectOptimisticLockingFailureException(Object.class, 1L));

        // when & then
        // 예외가 발생하지 않고 Recover에서 정상 종료되는지 확인
        assertThatCode(() -> service.decreaseStockWithSpringRetry("아이폰15", 1))
                .doesNotThrowAnyException();

        // then
        // "낙관적 락 실패를 복구 처리했습니다. 관리자 확인 필요: 아이폰15" 라는 로그 메시지 확인 가능
        then(repository).should(times(3)).save(any());
    }

    @Test
    void 첫번째_재시도에서_성공하면_Spring_Retry는_수행되지_않는다() {
        // given
        SafeDBLevelRaceConditionUsingOptimisticLockJpaEntity entity =
                new SafeDBLevelRaceConditionUsingOptimisticLockJpaEntity("아이폰15", 10);

        given(repository.findByProductName("아이폰15"))
                .willReturn(Optional.of(entity));

        // 첫 번째 시도에서 바로 성공하도록 설정
        given(repository.save(any()))
                .willReturn(entity);

        // when
        assertThatCode(() -> service.decreaseStockWithSpringRetry("아이폰15", 1))
                .doesNotThrowAnyException();

        // 첫 번째 시도에서 성공했으므로 save는 단 1번만 호출되어야 함
        then(repository).should(times(1)).save(any());
        // 로그에서 "낙관적 락 실패를 복구 처리했습니다" 메시지가 없어야 함
    }

    @Test
    void 두번째_재시도에서_성공해도_Spring_Retry는_수행되지_않는다() {
        // given
        SafeDBLevelRaceConditionUsingOptimisticLockJpaEntity entity =
                new SafeDBLevelRaceConditionUsingOptimisticLockJpaEntity("아이폰15", 10);

        given(repository.findByProductName("아이폰15"))
                .willReturn(Optional.of(entity));

        // 첫 번째는 실패, 두 번째는 성공하도록 설정
        given(repository.save(any()))
                .willThrow(new ObjectOptimisticLockingFailureException(Object.class, 1L))
                .willReturn(entity);

        // when
        assertThatCode(() -> service.decreaseStockWithSpringRetry("아이폰15", 1))
                .doesNotThrowAnyException();

        // 총 2번의 save 시도가 있어야 함 (첫 번째 실패, 두 번째 성공)
        then(repository).should(times(2)).save(any());
        // 로그에서 "낙관적 락 실패를 복구 처리했습니다" 메시지가 없어야 함
    }

    @Test
    void 마지막_재시도에서_성공해도_Spring_Retry는_수행되지_않는다() {
        // given
        SafeDBLevelRaceConditionUsingOptimisticLockJpaEntity entity =
                new SafeDBLevelRaceConditionUsingOptimisticLockJpaEntity("아이폰15", 10);

        given(repository.findByProductName("아이폰15"))
                .willReturn(Optional.of(entity));

        // 첫 번째는 실패, 두 번째는 성공하도록 설정
        given(repository.save(any()))
                .willThrow(new ObjectOptimisticLockingFailureException(Object.class, 1L))
                .willThrow(new ObjectOptimisticLockingFailureException(Object.class, 2L))
                .willReturn(entity);

        // when
        assertThatCode(() -> service.decreaseStockWithSpringRetry("아이폰15", 1))
                .doesNotThrowAnyException();

        // 총 3번의 save 시도가 있어야 함 (첫 번째 실패, 두 번째 실패, 세 번째 성공)
        then(repository).should(times(3)).save(any());
        // 로그에서 "낙관적 락 실패를 복구 처리했습니다" 메시지가 없어야 함
    }
}