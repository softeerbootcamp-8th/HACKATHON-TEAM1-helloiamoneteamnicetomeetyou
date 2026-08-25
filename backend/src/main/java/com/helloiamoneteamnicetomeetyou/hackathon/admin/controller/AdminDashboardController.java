package com.helloiamoneteamnicetomeetyou.hackathon.admin.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.BoothView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ItemDemandView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminBoothService;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminDashboardService;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminResetService;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminSeedService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 부스 운영 중에 제일 먼저 보는 화면이다.
 *
 * <p>매칭이 안 붙을 때 무엇을 해야 하는지가 여기서 보여야 한다. 그래서 카드별 수요와 공급을
 * 맨 위에 두고, 그 아래에 시연 준비와 정리를 붙였다.
 */
@Controller
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final AdminBoothService adminBoothService;
    private final AdminSeedService adminSeedService;
    private final AdminResetService adminResetService;

    @GetMapping({"/admin", "/admin/"})
    public String dashboard(Model model) {
        List<ItemDemandView> demand = adminDashboardService.findDemand();
        List<BoothView> booths = adminBoothService.findBooths();

        model.addAttribute("nav", "dashboard");
        model.addAttribute("demand", demand);
        model.addAttribute("deadEndCount", adminDashboardService.countDeadEnds(demand));
        model.addAttribute("booths", booths);
        model.addAttribute("connectedTotal", booths.stream().mapToInt(BoothView::connectedCount).sum());

        return "admin/dashboard";
    }

    @PostMapping("/admin/seed")
    public String seed(
            @RequestParam Long boothId,
            @RequestParam(defaultValue = "6") int dummyCount,
            RedirectAttributes redirectAttributes) {

        int created = adminSeedService.seed(boothId, dummyCount);
        redirectAttributes.addFlashAttribute("toast", "더미 %d명을 세웠습니다.".formatted(created));

        return "redirect:/admin";
    }

    @PostMapping("/admin/reset")
    public String reset(
            @RequestParam Long boothId,
            @RequestParam String confirmName,
            RedirectAttributes redirectAttributes) {

        int removed = adminResetService.resetDummies(boothId, confirmName);
        redirectAttributes.addFlashAttribute("toast", "더미 %d명과 그 교환 기록을 지웠습니다.".formatted(removed));

        return "redirect:/admin";
    }
}
