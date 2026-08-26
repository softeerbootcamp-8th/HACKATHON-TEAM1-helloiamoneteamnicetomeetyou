package com.helloiamoneteamnicetomeetyou.hackathon.admin.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminExchangeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /**
     * 더미가 고른 시간 칸을 넣는다.
     *
     * <p><b>{@code required = false} 다.</b> 체크를 하나도 안 하면 브라우저가 {@code slots} 를
     * 아예 안 보내는데, 그것을 필수로 받으면 "고른 것 전부 지우기" 가 400 이 되어 버린다.
     */
    @PostMapping("/participants/{participantId}/time-slots")
    public String updateTimeSlots(
            @PathVariable Long participantId,
            @RequestParam(required = false) List<Integer> slots,
            RedirectAttributes redirectAttributes) {

        adminExchangeService.updateTimeSlotsAsDummy(participantId, slots);
        redirectAttributes.addFlashAttribute(
                "toast",
                slots == null || slots.isEmpty() ? "더미가 고른 시간을 지웠습니다." : "더미 대신 시간을 넣었습니다.");

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
