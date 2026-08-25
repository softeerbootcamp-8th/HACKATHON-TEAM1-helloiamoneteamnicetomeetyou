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

    /** 등록할 때마다 새 행을 만들기 때문에 항상 201 을 반환한다. 멱등하지 않다. */
    @PostMapping
    public ResponseEntity<CommonResponse<Void>> register(
            @RequestBody HaveItemRegisterRequestDto request) {

        userHaveItemService.register(request.userId(), request.itemId(), request.quantity());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.ok("등록했습니다."));
    }
}
