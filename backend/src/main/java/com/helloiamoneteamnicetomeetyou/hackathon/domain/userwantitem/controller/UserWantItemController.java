package com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.dto.WantItemRegisterRequestDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.service.UserWantItemService;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.dto.WantItemRegisteredResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.CommonResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/want-items")
@RequiredArgsConstructor
public class UserWantItemController {

    private final UserWantItemService userWantItemService;

    /** 이미 등록한 카드면 개수를 덮어쓰고 200 으로 답한다. */
    @PostMapping
    public ResponseEntity<CommonResponse<Void>> register(
            @RequestBody WantItemRegisterRequestDto request) {

        boolean created = userWantItemService.register(
                request.userId(), request.itemId(), request.quantity());

        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.OK)
                .body(CommonResponse.ok("등록했습니다."));
    }

    /**
     * 내가 지금 등록해 둔 찾는 카드 전부.
     *
     * <p>등록 화면이 제출 직전에 읽어 "서버에는 있는데 이번 선택에는 없는 카드" 를 가려낸다.
     * 로그인이 없어서 {@code userId} 가 신원이고, 조회는 저장소 규칙대로 쿼리 파라미터로 받는다.
     */
    @GetMapping
    public ResponseEntity<CommonResponse<List<WantItemRegisteredResponseDto>>> findMine(
            @RequestParam UUID userId) {

        return ResponseEntity.ok(
                CommonResponse.ok(userWantItemService.findMine(userId), "조회했습니다."));
    }

    /**
     * 찾는 카드 등록을 해제한다. 없는 카드를 지워도 200 이다.
     *
     * <p>본문 대신 쿼리 파라미터로 {@code userId} 를 받는다. DELETE 에 본문을 실으면 중간의
     * 프록시가 떨구는 경우가 있다.
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<CommonResponse<Void>> remove(
            @PathVariable Long itemId, @RequestParam UUID userId) {

        userWantItemService.remove(userId, itemId);

        return ResponseEntity.ok(CommonResponse.ok("해제했습니다."));
    }
}
