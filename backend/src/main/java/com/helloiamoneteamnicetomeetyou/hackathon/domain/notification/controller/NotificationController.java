package com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.dto.NotificationReadRequestDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.dto.NotificationResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.notification.service.NotificationService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.CommonResponse;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.PageRequestValues;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.PageResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림함. {@code userId} 는 GET 은 쿼리 파라미터로, PATCH 는 body 첫 필드로 받는다.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** 기기가 꺼져 있던 동안 놓친 것까지 전부 나온다. 최근 것이 먼저다. */
    @GetMapping
    public CommonResponse<PageResponse<NotificationResponseDto>> findAll(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "" + PageRequestValues.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + PageRequestValues.DEFAULT_SIZE) int size) {

        return CommonResponse.ok(notificationService.findAll(userId, page, size), "조회했습니다.");
    }

    @PatchMapping("/{notificationId}")
    public CommonResponse<Void> markAsRead(
            @PathVariable Long notificationId, @RequestBody NotificationReadRequestDto request) {

        notificationService.markAsRead(notificationId, request.userId());
        return CommonResponse.ok("읽음 처리했습니다.");
    }
}
