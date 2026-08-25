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
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final List<String> allowedOriginPatterns;

    public CorsConfig(@Value("${cors.allowed-origin-patterns}") List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOriginPatterns.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
