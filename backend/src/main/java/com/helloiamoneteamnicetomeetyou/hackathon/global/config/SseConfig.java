package com.helloiamoneteamnicetomeetyou.hackathon.global.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SSE 전송에만 쓰는 실행기와 스케줄링을 켠다.
 *
 * <p>{@code @EnableScheduling} 은 죽은 연결을 찾아내는 heartbeat 를 위해 필요하다.
 */
@Configuration
@EnableScheduling
public class SseConfig {

    /**
     * SSE 전송 전용 실행기다. 가상 스레드를 쓴다.
     *
     * <p><b>이유는 속도가 아니라 블로킹이다.</b> 전파가 약한 곳에 있는 사람 하나가 소켓 write 에서
     * 멈추면 그 작업은 응답이 있을 때까지 스레드를 붙잡는다. 크기가 정해진 스레드 풀이면 그런
     * 연결 몇 개로 풀이 고갈되고, 그때부터 멀쩡한 사람들의 알림까지 전부 밀린다. 가상 스레드는
     * 블로킹하는 동안 캐리어 스레드를 반납해서 이 문제가 없다.
     *
     * <p><b>{@code spring.threads.virtual.enabled} 로 전역을 켜지 않는다.</b> 그러면 톰캣 워커까지
     * 가상 스레드가 되는데, 매칭 계산처럼 CPU 를 오래 쓰는 작업은 캐리어 스레드를 점유한 채
     * 놓지 않아서 같은 캐리어를 쓰는 SSE 전송까지 같이 굶는다. 성격이 정반대인 두 작업을 한
     * 실행기에 섞지 않으려고 SSE 쪽만 따로 뒀다. <b>여기에 CPU 작업을 올리면 안 된다.</b>
     *
     * <p>{@code close()} 는 남은 전송이 끝날 때까지 기다렸다 닫는다. SSE 전송 한 건은 짧아서
     * 종료가 오래 걸리지 않는다.
     */
    @Bean(destroyMethod = "close")
    public ExecutorService sseExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
