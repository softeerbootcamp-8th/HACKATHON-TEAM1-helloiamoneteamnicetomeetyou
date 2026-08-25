package com.helloiamoneteamnicetomeetyou.hackathon.domain.push.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.push.dto.PushSubscribeRequestDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.push.dto.PushTestRequestDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.push.dto.VapidPublicKeyResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.push.service.PushSendService;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.push.service.PushSubscriptionService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.config.WebPushConfig.VapidPublicKey;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushSubscriptionService pushSubscriptionService;
    private final PushSendService pushSendService;
    private final VapidPublicKey vapidPublicKey;

    /**
     * 브라우저가 구독을 만들 때 넘겨야 하는 공개키다.
     *
     * <p>프론트 환경변수로 넣지 않는 이유는, 값을 바꾸면 Vercel 재배포를 해야 하는데 백엔드가
     * 이미 같은 값을 갖고 있어서다. 비밀이 아니라 그대로 내려줘도 된다.
     */
    @GetMapping("/vapid-public-key")
    public CommonResponse<VapidPublicKeyResponseDto> getVapidPublicKey() {
        return CommonResponse.ok(new VapidPublicKeyResponseDto(vapidPublicKey.value()), "조회했습니다.");
    }

    /** 멱등하다. 앱을 열 때마다 불러도 되고, 같은 브라우저면 행이 늘지 않는다. */
    @PostMapping("/subscriptions")
    public CommonResponse<Void> subscribe(@RequestBody PushSubscribeRequestDto request) {
        pushSubscriptionService.subscribe(
                request.userId(), request.endpoint(), request.p256dh(), request.auth());

        return CommonResponse.ok("알림을 켰습니다.");
    }

    /**
     * 본인 기기로 시험 삼아 한 번 보낸다.
     *
     * <p>교환과 매칭 도메인이 아직 없어서, 이게 없으면 알림이 실제로 도착하는지 확인할 방법이
     * 없다. 알림을 켠 직후 "정말 오는지" 보여 주는 자리에도 그대로 쓴다. 문구를 서버가 고정해
     * 두는 것은 아무 말이나 남에게 보내는 통로가 되지 않게 하기 위해서다.
     */
    @PostMapping("/test")
    public CommonResponse<Void> sendTest(@RequestBody PushTestRequestDto request) {
        pushSendService.send(request.userId(), "NearLy", "알림이 정상적으로 켜졌어요.", "/home");

        return CommonResponse.ok("테스트 알림을 보냈습니다.");
    }
}
