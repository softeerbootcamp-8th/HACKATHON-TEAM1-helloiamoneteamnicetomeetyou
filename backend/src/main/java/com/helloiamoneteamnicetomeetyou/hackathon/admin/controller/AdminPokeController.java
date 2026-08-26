package com.helloiamoneteamnicetomeetyou.hackathon.admin.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminPokeService;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.enums.PokeStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/pokes")
@RequiredArgsConstructor
public class AdminPokeController {

    private final AdminPokeService adminPokeService;

    /** 더미가 실제 사용자를 찔러본다. requestedItemId 는 상대가 내놓고 있는 카드다. */
    @PostMapping
    public String send(
            @RequestParam UUID fromUserId,
            @RequestParam UUID toUserId,
            @RequestParam Long requestedItemId,
            RedirectAttributes redirectAttributes) {

        adminPokeService.sendAsDummy(fromUserId, toUserId, requestedItemId);
        redirectAttributes.addFlashAttribute("toast", "더미가 찔러보기를 보냈습니다.");

        return "redirect:/admin?tab=pokes";
    }

    @PostMapping("/{pokeId}/accept")
    public String accept(
            @PathVariable Long pokeId,
            @RequestParam Long chosenItemId,
            RedirectAttributes redirectAttributes) {

        adminPokeService.answerAsDummy(pokeId, PokeStatus.ACCEPTED, chosenItemId);
        redirectAttributes.addFlashAttribute("toast", "더미 대신 수락했습니다. 교환이 만들어졌습니다.");

        return "redirect:/admin?tab=pokes";
    }

    @PostMapping("/{pokeId}/reject")
    public String reject(@PathVariable Long pokeId, RedirectAttributes redirectAttributes) {
        adminPokeService.answerAsDummy(pokeId, PokeStatus.REJECTED, null);
        redirectAttributes.addFlashAttribute("toast", "더미 대신 거절했습니다.");

        return "redirect:/admin?tab=pokes";
    }
}
