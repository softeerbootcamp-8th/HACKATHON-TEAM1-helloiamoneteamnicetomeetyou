package com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.dto.WantItemRegisterRequestDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.service.UserWantItemService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/want-items")
@RequiredArgsConstructor
public class UserWantItemController {

    private final UserWantItemService userWantItemService;

    /** 이미 등록한 카드면 원하는 개수를 덮어쓰고 200 으로 답한다. */
    @PostMapping
    public ResponseEntity<CommonResponse<Void>> register(
            @RequestBody WantItemRegisterRequestDto request) {

        boolean created = userWantItemService.register(
                request.userId(), request.itemId(), request.quantity());

        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.OK)
                .body(CommonResponse.ok("등록했습니다."));
    }
}
