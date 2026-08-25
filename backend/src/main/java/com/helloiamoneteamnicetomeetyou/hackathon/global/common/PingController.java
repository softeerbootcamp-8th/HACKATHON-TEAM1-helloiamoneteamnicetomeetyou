package com.helloiamoneteamnicetomeetyou.hackathon.global.common;

import com.helloiamoneteamnicetomeetyou.hackathon.global.common.dto.PingResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.CommonResponse;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프론트와 백엔드가 붙었는지 확인하는 엔드포인트다.
 *
 * <p>{@code /health} 는 컨테이너 헬스체크가 쓰는 자리라 응답이 평문 "OK" 고 형식이 팀 규칙과 다르다.
 * 프론트가 확인할 때는 실제 API 와 같은 {@code /api} prefix 와 {@code CommonResponse} 형식을 거쳐야
 * 경로와 응답 파싱까지 한꺼번에 검증되기 때문에 따로 뒀다.
 */
@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public CommonResponse<PingResponseDto> ping() {
        return CommonResponse.ok(PingResponseDto.of(LocalDateTime.now()), "연결되었습니다.");
    }
}
