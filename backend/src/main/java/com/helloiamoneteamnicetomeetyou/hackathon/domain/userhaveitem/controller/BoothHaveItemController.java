package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.dto.BoothHaveItemResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.service.BoothHaveItemService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.CommonResponse;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.PageRequestValues;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.PageResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/booths")
@RequiredArgsConstructor
public class BoothHaveItemController {

    private final BoothHaveItemService boothHaveItemService;

    /**
     * 부스 안에서 다른 사용자들이 내놓은 카드 목록이다. 교환 대기장소의 전체 리스트가 이걸 쓴다.
     *
     * <p>{@code userId} 를 쿼리 파라미터로 받는다. GET 은 본문을 실을 수 없어서 정한 팀 규칙이다.
     * 이 값으로 나를 목록에서 빼고, 내 희망·보유 카드와 견줘 각 줄의 상태를 채운다.
     */
    @GetMapping("/{boothId}/have-items")
    public ResponseEntity<CommonResponse<PageResponse<BoothHaveItemResponseDto>>> findByBooth(
            @PathVariable Long boothId,
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "" + PageRequestValues.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + PageRequestValues.DEFAULT_SIZE) int size) {

        return ResponseEntity.ok(CommonResponse.ok(
                boothHaveItemService.findByBooth(boothId, userId, page, size), "조회했습니다."));
    }
}
