package com.helloiamoneteamnicetomeetyou.hackathon.global.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 프론트가 백엔드와 다른 오리진에서 뜨기 때문에 CORS 를 열어 준다.
 *
 * <p>Vercel 프리뷰 배포는 커밋마다 도메인이 달라서 정확한 오리진을 미리 적어 둘 수 없다.
 * 그래서 {@code allowedOrigins} 가 아니라 와일드카드를 받는 {@code allowedOriginPatterns} 를 쓴다.
 *
 * <p><b>{@code /api/**} 에만 건다. {@code /**} 로 두면 어드민 화면이 자기 자신에게 보내는 POST 가
 * 403 {@code Invalid CORS request} 로 막힌다.</b> 브라우저는 same-origin 이어도 POST 에 Origin 을
 * 붙이는데, 앞에 Caddy 가 있는 배포 환경에서는 서버가 자기 주소를 {@code http://...:8080} 으로
 * 알고 있어서 {@code https://52-78-131-174.sslip.io} 를 남의 오리진으로 본다. 그러면 허용 목록에
 * 없으니 거절한다. 로그인 화면은 GET 이라 열리는데 로그인 버튼만 안 먹는 식으로 보인다.
 *
 * <p>어드민은 서버가 그려서 내려보내는 화면이라 애초에 CORS 가 필요 없다. 다른 오리진에서
 * 불리는 것은 {@code /api/**} 뿐이라 거기에만 연다.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final List<String> allowedOriginPatterns;

    public CorsConfig(@Value("${cors.allowed-origin-patterns}") List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
