package com.helloiamoneteamnicetomeetyou.hackathon.admin.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminUserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
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

    /** 무엇을 고치든 방금 보던 사람으로 돌아온다. 그래야 바뀐 것이 눈에 바로 보인다. */
    private static final String CONSOLE = "redirect:/admin?tab=users&userId=";

    private final AdminUserService adminUserService;

    /**
     * 사용자를 만든다. 내놓는 카드와 찾는 카드를 함께 고를 수 있다.
     *
     * <p>카드를 안 고르면 그냥 빈 사용자가 만들어진다. 이름만 먼저 만들어 두고 카드를 뒤에
     * 붙이는 것도 부스에서 실제로 하는 순서다.
     */
    @PostMapping
    public String createDummy(
            @RequestParam String username,
            @RequestParam(required = false) List<Long> haveItemIds,
            @RequestParam(required = false) List<Long> wantItemIds,
            RedirectAttributes redirectAttributes) {

        UUID userId = adminUserService.createDummy(username, haveItemIds, wantItemIds);
        redirectAttributes.addFlashAttribute("toast", "%s 을(를) 만들었습니다.".formatted(username));

        return CONSOLE + userId;
    }

    @PostMapping("/{userId}/rename")
    public String rename(
            @PathVariable UUID userId,
            @RequestParam String username,
            RedirectAttributes redirectAttributes) {

        adminUserService.rename(userId, username);
        redirectAttributes.addFlashAttribute("toast", "이름을 고쳤습니다.");

        return CONSOLE + userId;
    }

    @PostMapping("/{userId}/have")
    public String addHave(
            @PathVariable UUID userId,
            @RequestParam Long itemId,
            @RequestParam(defaultValue = "1") int quantity,
            RedirectAttributes redirectAttributes) {

        adminUserService.addHaveItem(userId, itemId, quantity);
        redirectAttributes.addFlashAttribute("toast", "내놓는 카드에 추가했습니다.");

        return CONSOLE + userId;
    }

    @PostMapping("/{userId}/have/{haveId}/quantity")
    public String changeHaveQuantity(
            @PathVariable UUID userId,
            @PathVariable Long haveId,
            @RequestParam int quantity,
            RedirectAttributes redirectAttributes) {

        adminUserService.changeHaveQuantity(haveId, quantity);
        redirectAttributes.addFlashAttribute("toast", "수량을 고쳤습니다.");

        return CONSOLE + userId;
    }

    @PostMapping("/{userId}/have/{haveId}/delete")
    public String removeHave(
            @PathVariable UUID userId,
            @PathVariable Long haveId,
            RedirectAttributes redirectAttributes) {

        adminUserService.removeHaveItem(haveId);
        redirectAttributes.addFlashAttribute("toast", "내놓는 카드에서 뺐습니다.");

        return CONSOLE + userId;
    }

    @PostMapping("/{userId}/want")
    public String addWant(
            @PathVariable UUID userId,
            @RequestParam Long itemId,
            RedirectAttributes redirectAttributes) {

        adminUserService.addWantItem(userId, itemId);
        redirectAttributes.addFlashAttribute("toast", "찾는 카드에 추가했습니다.");

        return CONSOLE + userId;
    }

    @PostMapping("/{userId}/want/{wantId}/delete")
    public String removeWant(
            @PathVariable UUID userId,
            @PathVariable Long wantId,
            RedirectAttributes redirectAttributes) {

        adminUserService.removeWantItem(wantId);
        redirectAttributes.addFlashAttribute("toast", "찾는 카드에서 뺐습니다.");

        return CONSOLE + userId;
    }

    @PostMapping("/{userId}/delete")
    public String deleteDummy(@PathVariable UUID userId, RedirectAttributes redirectAttributes) {
        int removedExchanges = adminUserService.deleteDummy(userId);
        redirectAttributes.addFlashAttribute("toast", removedExchanges == 0
                ? "더미를 지웠습니다."
                : "더미를 지웠습니다. 이 사람이 낀 교환 " + removedExchanges + "건도 같이 지웠습니다.");

        return "redirect:/admin?tab=users";
    }
}
