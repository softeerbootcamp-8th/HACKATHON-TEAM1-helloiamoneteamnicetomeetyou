package com.helloiamoneteamnicetomeetyou.hackathon.admin.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminSeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 사용자를 여러 명 한 번에 만든다.
 *
 * <p>부스에서 같은 시연을 수십 번 반복하는데, 매번 사람을 하나씩 만들고 카드를 하나씩 붙이면
 * 관람객을 세워 둔 채로 몇 분을 쓰게 된다.
 *
 * <p><b>두 버튼은 목적이 다르다.</b> {@code /admin/seed} 는 이미 성사된 교환을 만들어 교환
 * 진행 화면을 시연하는 것이고, {@code /admin/seed/waiting} 은 아직 짝이 없는 사람을 부스에
 * 세워 두어 관람객이 들어왔을 때 매칭이 붙게 하는 것이다. 부스를 열 때 눌러야 하는 것은
 * 뒤쪽이다 — 케이스로 만든 더미는 이미 짝이 있어서 관람객의 후보에서 빠진다.
 *
 * <p><b>한 번에 지우는 기능은 두지 않았다.</b> 더미와 실제 사용자가 섞여 있는 목록에서 되돌릴 수
 * 없는 일괄 삭제를 버튼 하나로 만들어 두는 것이 부스에서 너무 위험하다고 봤다. 지우는 것은
 * 사용자 화면에서 한 명씩 한다.
 */
@Controller
@RequiredArgsConstructor
public class AdminDummyController {

    private final AdminSeedService adminSeedService;

    @PostMapping("/admin/seed")
    public String createCases(
            @RequestParam Long boothId,
            @RequestParam(defaultValue = "2") int caseSize,
            @RequestParam(defaultValue = "1") int caseCount,
            RedirectAttributes redirectAttributes) {

        int created = adminSeedService.createCases(boothId, caseSize, caseCount);
        redirectAttributes.addFlashAttribute(
                "toast", "%d인 매칭 %d개를 만들었습니다. (사용자 %d명)".formatted(caseSize, caseCount, created));

        return "redirect:/admin?tab=users";
    }

    /**
     * 부스를 연다. 아직 짝이 없는 대기 관람객을 채운다.
     *
     * <p>인원을 입력받지 않는다. 카드 종류마다 몇 명이 서 있어야 어떤 조합이 들어와도 매칭이
     * 붙는지는 계산으로 정해지는 값이라 {@link AdminSeedService#WAITING_PER_ITEM}), 운영자가
     * 부스에서 고를 일이 아니다. 모자란 만큼만 채우므로 여러 번 눌러도 무방하다.
     */
    @PostMapping("/admin/seed/waiting")
    public String openBooth(@RequestParam Long boothId, RedirectAttributes redirectAttributes) {
        AdminSeedService.OpenBoothResult result = adminSeedService.openBooth(boothId);

        redirectAttributes.addFlashAttribute("toast", toastOf(result));

        return "redirect:/admin?tab=users";
    }

    /** 한 명도 안 늘었을 때 "0명을 만들었습니다" 로 끝나면 눌러도 안 먹은 것처럼 보인다. */
    private String toastOf(AdminSeedService.OpenBoothResult result) {
        if (result.created() == 0) {
            return "이미 다 채워져 있습니다. 새로 세운 사람은 없습니다.";
        }

        return "대기 관람객 %d명을 세웠습니다. (비어 있던 카드 조합 %d개를 메웠습니다)"
                .formatted(result.created(), result.filledCombos());
    }
}
