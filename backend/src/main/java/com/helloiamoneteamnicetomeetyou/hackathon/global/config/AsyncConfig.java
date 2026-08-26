package com.helloiamoneteamnicetomeetyou.hackathon.global.config;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "matchingExecutor")
    public Executor matchingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("matching-");
        executor.initialize();
        return executor;
    }

    /**
     * 비동기 매칭에서 튀어나온 예외를 어떻게 남길지.
     *
     * <p><b>낙관적 락 충돌은 스택 트레이스 없이 한 줄로 남긴다.</b> 한 사람에게 매칭 트리거가
     * 여러 번 겹쳐 들어오면 여러 스레드가 같은 보유 카드를 예약하려 들고, 한 번만 성공하고
     * 나머지는 락에 막힌다. 막는 것이 설계 의도이고, 진 쪽은 아무것도 저장하지 않고 끝난다.
     *
     * <p>그런데 기본 핸들러가 막힌 스레드마다 예순 줄짜리 스택 트레이스를 ERROR 로 뱉어서, 배포
     * 로그에서 진짜 오류가 그 사이에 묻혔다. 실제로 {@code user_have_items.version} 이 NULL 이라
     * 같은 줄이 매번 막히던 문제가 이 소음에 가려 한참 안 보였다
     * ({@code HaveItemVersionBackfill}). 그래서 조용히 삼키지 않고 WARN 한 줄로 남긴다. 같은
     * 카드가 계속 올라오면 로그에서 눈에 띈다.
     *
     * <p>커밋할 때 나는 예외라 {@code runMatching} 안에서는 못 잡는다. 커밋이 메서드 밖의
     * {@code @Transactional} 프록시에서 일어나고, 그것을 다시 감싸는 여기가 잡을 수 있는 자리다.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        AsyncUncaughtExceptionHandler fallback = new SimpleAsyncUncaughtExceptionHandler();

        return (Throwable error, Method method, Object... params) -> {
            if (error instanceof OptimisticLockingFailureException) {
                log.warn("{} 이 낙관적 락에 막혀 지나갔다: {}", method.getName(), error.getMessage());
                return;
            }
            fallback.handleUncaughtException(error, method, params);
        };
    }
}
