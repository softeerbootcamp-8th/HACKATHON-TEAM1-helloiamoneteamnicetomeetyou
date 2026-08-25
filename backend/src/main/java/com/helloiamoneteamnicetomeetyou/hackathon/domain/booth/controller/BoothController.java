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
 * <p>같은 {@code /api/booths} 아래에 카드 목록({@code ItemController})과 실시간 구독
 * ({@code global.sse.SseController})이 따로 있다. 스트림 응답은 팀 공통 응답 형식으로 감쌀 수
 * 없고, 카드는 부스가 아니라 카드 도메인의 일이라 컨트롤러를 나눠 뒀다.
 */
@RestController
@RequestMapping("/api/booths")
@RequiredArgsConstructor
public class BoothController {

    private final BoothService boothService;

    /**
     * 부스 목록이다. 프론트가 앱을 열 때 한 번 불러 어느 부스를 볼지 정한다.
     *
     * <p>{@code userId} 를 받지 않는다. 누가 보든 같은 목록이다.
     */
    @GetMapping
    public ResponseEntity<CommonResponse<List<BoothResponseDto>>> findAll() {
        return ResponseEntity.ok(CommonResponse.ok(boothService.findAll(), "조회했습니다."));
    }

    /** 부스 안의 교환 장소다. 약속 화면이 지도에 핀을 찍을 때 쓴다. */
    @GetMapping("/{boothId}/zones")
    public ResponseEntity<CommonResponse<List<ZoneResponseDto>>> findZones(@PathVariable Long boothId) {
        return ResponseEntity.ok(CommonResponse.ok(boothService.findZones(boothId), "조회했습니다."));
    }
}
