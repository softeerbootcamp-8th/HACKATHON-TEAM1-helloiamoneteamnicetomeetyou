package com.helloiamoneteamnicetomeetyou.hackathon.domain.item.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.dto.ItemResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.service.ItemService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.CommonResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/booths")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    /**
     * 부스가 내놓은 카드 목록이다. 등록 화면이 고를 수 있는 것 전부다.
     *
     * <p>{@code userId} 를 받지 않는다. 누가 보든 같은 목록이라 사용자를 알 필요가 없다.
     */
    @GetMapping("/{boothId}/items")
    public ResponseEntity<CommonResponse<List<ItemResponseDto>>> findByBooth(
            @PathVariable Long boothId) {

        return ResponseEntity.ok(
                CommonResponse.ok(itemService.findByBooth(boothId), "조회했습니다."));
    }
}
