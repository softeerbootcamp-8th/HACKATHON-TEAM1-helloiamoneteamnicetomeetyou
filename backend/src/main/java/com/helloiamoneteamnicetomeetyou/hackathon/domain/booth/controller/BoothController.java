package com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.dto.BoothResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.dto.ZoneResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.service.BoothService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.CommonResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부스와 그 안의 교환 장소를 읽는다.
 *
 * <p>같은 {@code /api/booths} 아래에 실시간 구독 엔드포인트가 따로 있다
 * ({@code global.sse.SseController}). 스트림 응답이라 팀 공통 응답 형식으로 감쌀 수 없어서
 * 컨트롤러를 나눠 뒀다.
 */
@RestController
@RequestMapping("/api/booths")
@RequiredArgsConstructor
public class BoothController {

    private final BoothService boothService;

    /** 화면이 실시간 알림을 구독하려면 부스 id 를 알아야 해서, 앱을 열 때 이걸 먼저 부른다. */
    @GetMapping
    public ResponseEntity<CommonResponse<List<BoothResponseDto>>> findAll() {
        return ResponseEntity.ok(CommonResponse.ok(boothService.findAll(), "부스 목록입니다."));
    }

    @GetMapping("/{boothId}/zones")
    public ResponseEntity<CommonResponse<List<ZoneResponseDto>>> findZones(@PathVariable Long boothId) {
        return ResponseEntity.ok(CommonResponse.ok(boothService.findZones(boothId), "교환 장소 목록입니다."));
    }
}
