package com.helloiamoneteamnicetomeetyou.hackathon.domain.user.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.dto.UserRegisterRequestDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.service.UserService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인이 없다. 클라이언트가 UUID 를 만들어 localStorage 에 두고, 그 값을 모든 요청에 실어 보낸다.
 *
 * <p>이건 인증이 아니다. 남의 UUID 를 실어 보내면 그 사람인 척할 수 있다. 시연 범위에서 감수한
 * 선택이고, 실제 인증이 필요해지면 팀에서 다시 정한다.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 멱등하다. 앱을 열 때마다 불러도 되고, 이미 등록된 값이면 200 으로 답한다. */
    @PostMapping
    public ResponseEntity<CommonResponse<Void>> register(
            @RequestBody UserRegisterRequestDto request) {

        boolean created = userService.register(request.userId());

        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.OK)
                .body(CommonResponse.ok("등록했습니다."));
    }
}
