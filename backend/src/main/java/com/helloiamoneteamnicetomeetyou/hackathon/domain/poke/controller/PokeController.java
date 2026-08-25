package com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto.PokeAnswerRequestDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto.PokeAnswerResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto.PokeSendRequestDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto.PokeSendResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto.ReceivedPokeResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto.SentPokeResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.service.PokeService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.CommonResponse;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.PageRequestValues;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.PageResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 찔러보기. 서로 원하는 것이 맞지 않는 상대에게 보내는 단방향 교환 제안이다.
 *
 * <p>{@code userId} 는 POST·PATCH 는 body 첫 필드로, GET 은 쿼리 파라미터로 받는다. GET 이
 * 본문을 실을 수 없어서 정한 팀 규약이다.
 */
@RestController
@RequestMapping("/api/pokes")
@RequiredArgsConstructor
public class PokeController {

    private final PokeService pokeService;

    /** 찔러보기를 보낸다. 내놓을 카드는 담지 않는다. 내 보유 카드 전부가 묶음으로 간다. */
    @PostMapping
    public ResponseEntity<CommonResponse<PokeSendResponseDto>> send(
            @RequestBody PokeSendRequestDto request) {

        PokeSendResponseDto response = pokeService.send(
                request.userId(), request.targetUserId(), request.requestedItemId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.ok(response, "찔러봤습니다."));
    }

    /**
     * 내가 받은 찔러보기다. 답을 기다리는 것만 나온다.
     *
     * <p>실시간 알림은 끊긴 동안의 것을 다시 보내지 않는다. 화면이 다시 붙었을 때
     * {@code CONNECTED} 를 받고 이걸 불러 상태를 맞춘다.
     */
    @GetMapping("/received")
    public ResponseEntity<CommonResponse<PageResponse<ReceivedPokeResponseDto>>> findReceived(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "" + PageRequestValues.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + PageRequestValues.DEFAULT_SIZE) int size) {

        return ResponseEntity.ok(CommonResponse.ok(
                pokeService.findReceived(userId, page, size), "조회했습니다."));
    }

    /**
     * 내가 보낸 찔러보기다. 거절되고 수락된 것까지 전부 나온다.
     *
     * <p>대기 중인 상대의 카드를 비활성화하는 데 쓴다 (시안 desc 165:3514). 수락된 건의
     * {@code chosenItem} 이 상대가 내 묶음에서 고른 카드다.
     */
    @GetMapping("/sent")
    public ResponseEntity<CommonResponse<PageResponse<SentPokeResponseDto>>> findSent(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "" + PageRequestValues.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + PageRequestValues.DEFAULT_SIZE) int size) {

        return ResponseEntity.ok(
                CommonResponse.ok(pokeService.findSent(userId, page, size), "조회했습니다."));
    }

    /**
     * 받은 찔러보기에 답한다.
     *
     * <p>수락과 거절을 하나로 묶은 것은 팀 URL 규약이 "kebab-case 에 복수 명사" 라서
     * {@code /accept} 같은 동사 경로를 두지 않기 때문이다. 수락이면 {@code chosenItemId} 가
     * 함께 와야 한다.
     */
    @PatchMapping("/{pokeId}")
    public ResponseEntity<CommonResponse<PokeAnswerResponseDto>> answer(
            @PathVariable Long pokeId, @RequestBody PokeAnswerRequestDto request) {

        PokeAnswerResponseDto response = pokeService.answer(
                pokeId, request.userId(), request.status(), request.chosenItemId());

        return ResponseEntity.ok(CommonResponse.ok(response, "응답했습니다."));
    }
}
