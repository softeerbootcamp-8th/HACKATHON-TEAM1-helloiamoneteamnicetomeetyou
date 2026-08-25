package com.helloiamoneteamnicetomeetyou.hackathon.admin.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminBoothService;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminUserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 사용자와 그 사람의 보유·희망 카드를 다룬다.
 *
 * <p>부스에서 제일 자주 열게 될 화면이다. 매칭이 안 붙으면 여기서 카드를 붙였다 뗐다 하면서
 * 짝이 나게 만든다.
 */
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AdminBoothService adminBoothService;

    @GetMapping
    public String users(Model model) {
        model.addAttribute("nav", "users");
        model.addAttribute("users", adminUserService.findUsers());
        return "admin/users";
    }

    @GetMapping("/{userId}")
    public String user(@PathVariable UUID userId, Model model) {
        model.addAttribute("nav", "users");
        model.addAttribute("user", adminUserService.findUser(userId));
        model.addAttribute("haveItems", adminUserService.findHaveItems(userId));
        model.addAttribute("wantItems", adminUserService.findWantItems(userId));
        model.addAttribute("allItems", adminBoothService.findAllItems());
        return "admin/user-detail";
    }

    @PostMapping
    public String createDummy(@RequestParam String username, RedirectAttributes redirectAttributes) {
        UUID userId = adminUserService.createDummy(username);
        redirectAttributes.addFlashAttribute("toast", "더미 사용자를 만들었습니다.");

        return "redirect:/admin/users/" + userId;
    }

    @PostMapping("/{userId}/rename")
    public String rename(
            @PathVariable UUID userId,
            @RequestParam String username,
            RedirectAttributes redirectAttributes) {

        adminUserService.rename(userId, username);
        redirectAttributes.addFlashAttribute("toast", "이름을 고쳤습니다.");

        return "redirect:/admin/users/" + userId;
    }

    @PostMapping("/{userId}/have")
    public String addHave(
            @PathVariable UUID userId,
            @RequestParam Long itemId,
            @RequestParam(defaultValue = "1") int quantity,
            RedirectAttributes redirectAttributes) {

        adminUserService.addHaveItem(userId, itemId, quantity);
        redirectAttributes.addFlashAttribute("toast", "보유 카드를 붙였습니다.");

        return "redirect:/admin/users/" + userId;
    }

    @PostMapping("/{userId}/have/{haveId}/quantity")
    public String changeHaveQuantity(
            @PathVariable UUID userId,
            @PathVariable Long haveId,
            @RequestParam int quantity,
            RedirectAttributes redirectAttributes) {

        adminUserService.changeHaveQuantity(haveId, quantity);
        redirectAttributes.addFlashAttribute("toast", "수량을 고쳤습니다.");

        return "redirect:/admin/users/" + userId;
    }

    @PostMapping("/{userId}/have/{haveId}/delete")
    public String removeHave(
            @PathVariable UUID userId,
            @PathVariable Long haveId,
            RedirectAttributes redirectAttributes) {

        adminUserService.removeHaveItem(haveId);
        redirectAttributes.addFlashAttribute("toast", "보유 카드를 뗐습니다.");

        return "redirect:/admin/users/" + userId;
    }

    @PostMapping("/{userId}/want")
    public String addWant(
            @PathVariable UUID userId,
            @RequestParam Long itemId,
            RedirectAttributes redirectAttributes) {

        adminUserService.addWantItem(userId, itemId);
        redirectAttributes.addFlashAttribute("toast", "희망 카드를 붙였습니다.");

        return "redirect:/admin/users/" + userId;
    }

    @PostMapping("/{userId}/want/{wantId}/delete")
    public String removeWant(
            @PathVariable UUID userId,
            @PathVariable Long wantId,
            RedirectAttributes redirectAttributes) {

        adminUserService.removeWantItem(wantId);
        redirectAttributes.addFlashAttribute("toast", "희망 카드를 뗐습니다.");

        return "redirect:/admin/users/" + userId;
    }

    @PostMapping("/{userId}/delete")
    public String deleteDummy(@PathVariable UUID userId, RedirectAttributes redirectAttributes) {
        adminUserService.deleteDummy(userId);
        redirectAttributes.addFlashAttribute("toast", "더미 사용자를 지웠습니다.");

        return "redirect:/admin/users";
    }
}
