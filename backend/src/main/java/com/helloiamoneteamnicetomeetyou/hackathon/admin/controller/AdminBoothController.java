package com.helloiamoneteamnicetomeetyou.hackathon.admin.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminBoothService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 부스와 그 안의 구역, 카드를 다룬다.
 *
 * <p>구역과 카드를 부스 상세 한 화면에 같이 둔다. 둘 다 "이 부스에 딸린 것" 이라 따로 놓으면
 * 부스를 고르는 일을 두 번 하게 된다.
 */
@Controller
@RequestMapping("/admin/booths")
@RequiredArgsConstructor
public class AdminBoothController {

    private final AdminBoothService adminBoothService;

    @PostMapping
    public String createBooth(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {

        Long boothId = adminBoothService.createBooth(name, description);
        redirectAttributes.addFlashAttribute("toast", "부스를 만들었습니다.");

        return "redirect:/admin?tab=zones&boothId=" + boothId;
    }

    @PostMapping("/{boothId}")
    public String updateBooth(
            @PathVariable Long boothId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {

        adminBoothService.updateBooth(boothId, name, description);
        redirectAttributes.addFlashAttribute("toast", "부스 정보를 고쳤습니다.");

        return "redirect:/admin?tab=zones&boothId=" + boothId;
    }

    @PostMapping("/{boothId}/zones")
    public String createZone(
            @PathVariable Long boothId,
            @RequestParam String name,
            @RequestParam(required = false) String location,
            RedirectAttributes redirectAttributes) {

        adminBoothService.createZone(boothId, name, location);
        redirectAttributes.addFlashAttribute("toast", "구역을 추가했습니다.");

        return "redirect:/admin?tab=zones&boothId=" + boothId;
    }

    @PostMapping("/{boothId}/zones/{zoneId}")
    public String updateZone(
            @PathVariable Long boothId,
            @PathVariable Long zoneId,
            @RequestParam String name,
            @RequestParam(required = false) String location,
            RedirectAttributes redirectAttributes) {

        adminBoothService.updateZone(zoneId, name, location);
        redirectAttributes.addFlashAttribute("toast", "구역을 고쳤습니다.");

        return "redirect:/admin?tab=zones&boothId=" + boothId;
    }

    @PostMapping("/{boothId}/zones/{zoneId}/delete")
    public String deleteZone(
            @PathVariable Long boothId,
            @PathVariable Long zoneId,
            RedirectAttributes redirectAttributes) {

        adminBoothService.deleteZone(zoneId);
        redirectAttributes.addFlashAttribute("toast", "구역을 지웠습니다.");

        return "redirect:/admin?tab=zones&boothId=" + boothId;
    }

    @PostMapping("/{boothId}/items")
    public String createItem(
            @PathVariable Long boothId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String imageUrl,
            RedirectAttributes redirectAttributes) {

        adminBoothService.createItem(boothId, name, description, imageUrl);
        redirectAttributes.addFlashAttribute("toast", "카드를 추가했습니다.");

        return "redirect:/admin?tab=zones&boothId=" + boothId;
    }

    @PostMapping("/{boothId}/items/{itemId}")
    public String updateItem(
            @PathVariable Long boothId,
            @PathVariable Long itemId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String imageUrl,
            RedirectAttributes redirectAttributes) {

        adminBoothService.updateItem(itemId, name, description, imageUrl);
        redirectAttributes.addFlashAttribute("toast", "카드를 고쳤습니다.");

        return "redirect:/admin?tab=zones&boothId=" + boothId;
    }

    @PostMapping("/{boothId}/items/{itemId}/delete")
    public String deleteItem(
            @PathVariable Long boothId,
            @PathVariable Long itemId,
            RedirectAttributes redirectAttributes) {

        adminBoothService.deleteItem(itemId);
        redirectAttributes.addFlashAttribute("toast", "카드를 지웠습니다.");

        return "redirect:/admin?tab=zones&boothId=" + boothId;
    }
}
