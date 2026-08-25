package com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.dto.BoothResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.service.BoothService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.CommonResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
