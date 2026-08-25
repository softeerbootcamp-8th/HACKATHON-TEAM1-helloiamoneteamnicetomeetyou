package com.helloiamoneteamnicetomeetyou.hackathon.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 어드민 로그인 계정이다. {@code application.yml} 의 {@code admin.*} 을 받는다.
 *
 * <p>prod 프로파일에는 기본값이 없어서 환경변수가 빠지면 부팅이 실패한다. 배포된 서버가
 * 로컬용 기본 비밀번호로 열려 있는 것보다 안 뜨는 편이 낫다고 봤다.
 */
@ConfigurationProperties(prefix = "admin")
public record AdminProperties(String username, String password) {

    public boolean matches(String inputUsername, String inputPassword) {
        return username.equals(inputUsername) && password.equals(inputPassword);
    }
}
