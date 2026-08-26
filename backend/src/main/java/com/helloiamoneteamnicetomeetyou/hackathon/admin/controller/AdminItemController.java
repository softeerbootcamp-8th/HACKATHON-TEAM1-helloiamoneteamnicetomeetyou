package com.helloiamoneteamnicetomeetyou.hackathon.admin.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminBoothService;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminUserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 카드 탭에서 하는 일들.
 *
 * <p>사람 쪽에서 카드를 고르는 것과 카드 쪽에서 사람을 고르는 것은 결과가 같지만, 부스에서
 * 생각하는 방향은 그때그때 다르다. "이 사람에게 뭘 줄까" 일 때도 있고 "이 카드는 누가 가지고
 * 있지" 일 때도 있어서, 어느 쪽에서 시작하든 되게 열어 둔다.
 */
@Controller
@RequestMapping("/admin/items")
@RequiredArgsConstructor
public class AdminItemController {

    private static final String CONSOLE = "redirect:/admin?tab=items&itemId=";

    private final AdminBoothService adminBoothService;
    private final AdminUserService adminUserService;

    @PostMapping("/{itemId}")
    public String update(
            @PathVariable Long itemId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String imageUrl,
            RedirectAttributes redirectAttributes) {

        adminBoothService.updateItem(itemId, name, description, imageUrl);
        redirectAttributes.addFlashAttribute("toast", "카드를 고쳤습니다.");

        return CONSOLE + itemId;
    }

    @PostMapping("/{itemId}/delete")
    public String delete(@PathVariable Long itemId, RedirectAttributes redirectAttributes) {
        adminBoothService.deleteItem(itemId);
        redirectAttributes.addFlashAttribute("toast", "카드를 지웠습니다.");

        return "redirect:/admin?tab=items";
    }

    /** 이 카드를 내놓는 사람으로 넣는다. */
    @PostMapping("/{itemId}/holders")
    public String addHolder(
            @PathVariable Long itemId,
            @RequestParam UUID userId,
            RedirectAttributes redirectAttributes) {

        adminUserService.addHaveItem(userId, itemId, 1);
        redirectAttributes.addFlashAttribute("toast", "내놓는 사람으로 추가했습니다.");

        return CONSOLE + itemId;
    }

    /** 이 카드를 찾는 사람으로 넣는다. */
    @PostMapping("/{itemId}/seekers")
    public String addSeeker(
            @PathVariable Long itemId,
            @RequestParam UUID userId,
            RedirectAttributes redirectAttributes) {

        adminUserService.addWantItem(userId, itemId);
        redirectAttributes.addFlashAttribute("toast", "찾는 사람으로 추가했습니다.");

        return CONSOLE + itemId;
    }
}
