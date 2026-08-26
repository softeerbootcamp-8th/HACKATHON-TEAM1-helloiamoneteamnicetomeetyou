package com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.dto.MatchSuggestedResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.service.MatchingService;
import com.helloiamoneteamnicetomeetyou.hackathon.global.response.CommonResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 매칭 제안 조회.
 *
 * <p>매칭 자체는 카드를 등록할 때 서버가 알아서 돌리고 결과를 {@code MATCH_SUGGESTED} 로 보낸다.
 * 여기 있는 것은 그 이벤트를 놓친 화면이 현재 상태를 다시 읽는 자리다.
 */
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    /**
     * 내게 온, 아직 수락하지 않은 매칭 제안. 없으면 204 다.
     *
     * <p>실시간 연결이 붙을 때 부른다. 끊겼던 동안 온 {@code MATCH_SUGGESTED} 는 다시 오지 않아서,
     * 이걸 읽지 않으면 재연결한 사람이 자기에게 온 제안을 영영 못 본다.
     *
     * <p>응답 본문은 {@code MATCH_SUGGESTED} 이벤트의 데이터와 같은 모양이다. 화면이 실시간으로
     * 받은 것과 여기서 읽은 것을 같은 코드로 처리하게 하려는 것이라, 한쪽만 고치지 않는다.
     */
    @GetMapping("/pending")
    public ResponseEntity<CommonResponse<MatchSuggestedResponseDto>> findPending(
            @RequestParam UUID userId) {

        MatchSuggestedResponseDto suggestion = matchingService.findPendingSuggestionOf(userId);

        if (suggestion == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(CommonResponse.ok(suggestion, "대기 중인 매칭 제안입니다."));
    }
}
