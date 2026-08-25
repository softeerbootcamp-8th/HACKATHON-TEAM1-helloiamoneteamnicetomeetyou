package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.dto.HaveItemRegisterRequestDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.service.UserHaveItemService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/have-items")
@RequiredArgsConstructor
public class UserHaveItemController {

    private final UserHaveItemService userHaveItemService;

    /** 이미 등록한 카드면 개수를 덮어쓰고 200 으로 답한다. */
    @PostMapping
    public ResponseEntity<CommonResponse<Void>> register(
            @RequestBody HaveItemRegisterRequestDto request) {

        boolean created = userHaveItemService.register(
                request.userId(), request.itemId(), request.quantity());

        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.OK)
                .body(CommonResponse.ok("등록했습니다."));
    }
}
