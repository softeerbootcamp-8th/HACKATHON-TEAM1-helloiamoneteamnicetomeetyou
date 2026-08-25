package com.helloiamoneteamnicetomeetyou.hackathon.admin.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 어드민 경로에만 인증 인터셉터를 건다.
 *
 * <p>기존 {@code CorsConfig} 와 같은 {@code WebMvcConfigurer} 를 또 만드는 것인데, 스프링은
 * 여러 개를 모두 적용하므로 CORS 설정을 건드리지 않고 인터셉터만 얹을 수 있다. 한 클래스에
 * 몰아 두면 CORS 를 고칠 때 어드민 인증까지 같이 읽어야 한다.
 */
@Configuration
@EnableConfigurationProperties(AdminProperties.class)
@RequiredArgsConstructor
public class AdminWebConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/**")
                // 로그인 화면까지 막으면 들어갈 방법이 없어진다.
                // CSS 와 JS 는 /admin-assets/ 에 두었다. /admin/ 아래에 두면 로그인 화면이
                // 자기 스타일시트를 못 받아서 글자만 나온다.
                .excludePathPatterns("/admin/login", "/admin/logout");
    }
}
