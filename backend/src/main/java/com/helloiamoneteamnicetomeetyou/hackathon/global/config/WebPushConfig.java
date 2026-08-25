package com.helloiamoneteamnicetomeetyou.hackathon.global.config;

import com.interaso.webpush.VapidKeys;
import com.interaso.webpush.WebPushService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 웹푸시 발송에 쓰는 VAPID 키를 읽어 서비스 빈을 만든다.
 *
 * <p>키는 {@code npx web-push generate-vapid-keys --json} 이 뱉는 값을 그대로 쓴다. 그 출력이
 * base64url(패딩 없음) uncompressed bytes 라 {@code fromUncompressedBytes} 가 받는 형식과 같고,
 * publicKey 문자열이 브라우저의 {@code applicationServerKey} 값과도 그대로 일치한다.
 * {@code getX509PublicKey()} 는 DER 이라 브라우저가 못 받으니 섞지 않는다.
 *
 * <p>기본값을 주지 않는 것은 datasource 와 같은 이유다. 값이 빠지면 부팅 단계에서 바로 죽는
 * 편이, 아무한테도 안 가는 알림을 조용히 성공으로 처리하는 것보다 낫다.
 */
@Configuration
public class WebPushConfig {

    /**
     * 공개키는 비밀이 아니고 프론트가 그대로 쓴다.
     *
     * <p>{@code VITE_} 환경변수로 넣지 않는 이유는, 값을 바꾸면 Vercel 재배포를 강제하기
     * 때문이다. 백엔드가 이미 갖고 있으니 API 로 내려주면 출처가 한 곳으로 모인다.
     */
    public record VapidPublicKey(String value) {}

    @Bean
    public VapidPublicKey vapidPublicKey(@Value("${push.vapid.public-key}") String publicKey) {
        return new VapidPublicKey(publicKey);
    }

    @Bean
    public WebPushService webPushService(
            @Value("${push.vapid.public-key}") String publicKey,
            @Value("${push.vapid.private-key}") String privateKey,
            @Value("${push.vapid.subject}") String subject) {

        return new WebPushService(subject, VapidKeys.fromUncompressedBytes(publicKey, privateKey));
    }
}
