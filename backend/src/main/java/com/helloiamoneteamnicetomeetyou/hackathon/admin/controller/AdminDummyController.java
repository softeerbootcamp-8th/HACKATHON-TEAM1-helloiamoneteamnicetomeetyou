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
}
