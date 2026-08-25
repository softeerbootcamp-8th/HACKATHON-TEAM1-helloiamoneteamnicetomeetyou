package com.helloiamoneteamnicetomeetyou.hackathon.admin.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/exchanges")
@RequiredArgsConstructor
public class AdminExchangeController {

    private final AdminExchangeService adminExchangeService;

    @PostMapping("/participants/{participantId}/accept")
    public String accept(@PathVariable Long participantId, RedirectAttributes redirectAttributes) {
        adminExchangeService.acceptAsDummy(participantId);
        redirectAttributes.addFlashAttribute("toast", "더미 대신 수락했습니다.");

        return "redirect:/admin?tab=exchanges";
    }

    @PostMapping("/participants/{participantId}/reject")
    public String reject(@PathVariable Long participantId, RedirectAttributes redirectAttributes) {
        adminExchangeService.rejectAsDummy(participantId);
        redirectAttributes.addFlashAttribute("toast", "더미 대신 거절했습니다.");

        return "redirect:/admin?tab=exchanges";
    }

    @PostMapping("/{exchangeId}/cancel")
    public String cancel(@PathVariable Long exchangeId, RedirectAttributes redirectAttributes) {
        adminExchangeService.cancel(exchangeId);
        redirectAttributes.addFlashAttribute("toast", "교환을 취소했습니다.");

        return "redirect:/admin?tab=exchanges";
    }

    @PostMapping("/{exchangeId}/complete")
    public String complete(@PathVariable Long exchangeId, RedirectAttributes redirectAttributes) {
        adminExchangeService.complete(exchangeId);
        redirectAttributes.addFlashAttribute("toast", "교환을 완료 처리했습니다.");

        return "redirect:/admin?tab=exchanges";
    }
}
