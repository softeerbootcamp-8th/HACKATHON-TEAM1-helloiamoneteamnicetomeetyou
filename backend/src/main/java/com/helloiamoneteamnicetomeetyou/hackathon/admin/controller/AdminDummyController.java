package com.helloiamoneteamnicetomeetyou.hackathon.admin.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminResetService;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminSeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 더미를 한 번에 만들고 한 번에 지운다. 화면 맨 위 두 버튼이 여기로 온다.
 *
 * <p>부스에서 같은 시연을 수십 번 반복하는데, 매번 사람을 하나씩 만들고 카드를 하나씩 붙이면
 * 관람객을 세워 둔 채로 몇 분을 쓰게 된다.
 */
@Controller
@RequiredArgsConstructor
public class AdminDummyController {

    private final AdminSeedService adminSeedService;
    private final AdminResetService adminResetService;

    @PostMapping("/admin/seed")
    public String create(
            @RequestParam Long boothId,
            @RequestParam(defaultValue = "6") int dummyCount,
            RedirectAttributes redirectAttributes) {

        int created = adminSeedService.seed(boothId, dummyCount);
        redirectAttributes.addFlashAttribute("toast", "더미 %d명을 만들었습니다.".formatted(created));

        return "redirect:/admin?tab=users";
    }

    @PostMapping("/admin/reset")
    public String deleteAll(@RequestParam Long boothId, RedirectAttributes redirectAttributes) {
        int removed = adminResetService.resetDummies(boothId);
        redirectAttributes.addFlashAttribute("toast", "더미 %d명과 그 약속을 지웠습니다.".formatted(removed));

        return "redirect:/admin?tab=users";
    }
}
