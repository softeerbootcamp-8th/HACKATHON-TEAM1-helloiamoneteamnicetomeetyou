package com.helloiamoneteamnicetomeetyou.hackathon.admin.controller;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminBoothService;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminExchangeService;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminPokeService;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.service.AdminUserService;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ExchangeView;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 어드민 화면 전체가 이 한 페이지다.
 *
 * <p>탭으로 무엇을 다룰지 고르고, 왼쪽에서 대상을 고르면 오른쪽에서 바로 고친다. 화면을
 * 넘어 다니지 않는 것이 목적인데, 부스에서는 관람객을 앞에 세워 두고 조작하기 때문에 목록과
 * 상세를 오가는 왕복이 그대로 시간이 된다.
 *
 * <p>고른 대상은 주소에 남긴다({@code ?tab=users&userId=...}). 무언가를 고치고 나면 같은
 * 자리로 돌아와야 방금 뭘 했는지 눈으로 확인할 수 있다.
 */
@Controller
@RequiredArgsConstructor
public class AdminConsoleController {

    private final AdminUserService adminUserService;
    private final AdminBoothService adminBoothService;
    private final AdminExchangeService adminExchangeService;
    private final AdminPokeService adminPokeService;

    @GetMapping({"/admin", "/admin/"})
    public String console(
            @RequestParam(defaultValue = "users") String tab,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) Long boothId,
            @RequestParam(required = false) ExchangeStatus status,
            Model model) {

        model.addAttribute("tab", tab);
        model.addAttribute("booths", adminBoothService.findBooths());

        switch (tab) {
            case "items" -> items(itemId, model);
            case "zones" -> zones(boothId, model);
            case "exchanges" -> exchanges(status, model);
            case "pokes" -> model.addAttribute("pokes", adminPokeService.findPokes());
            // 사용자를 만드는 일은 목록 옆에 끼워 넣기에는 고를 것이 많다. 화면을 따로 준다.
            case "new-user" -> model.addAttribute("allItems", adminBoothService.findAllItems());
            default -> users(userId, model);
        }

        return "admin/console";
    }

    /**
     * 왼쪽에서 아무도 고르지 않았으면 맨 위 사람을 편다.
     *
     * <p>빈 오른쪽 화면을 먼저 보여 주고 "골라 주세요" 라고 하면 한 번 더 눌러야 한다. 부스에서
     * 제일 자주 보게 될 화면이라 열자마자 조작할 수 있는 상태로 둔다.
     */
    private void users(UUID userId, Model model) {
        var users = adminUserService.findUsers();
        model.addAttribute("users", users);

        // 사용자가 하나도 없어도 "카드까지 정해서 만들기" 폼은 나와야 한다. 그 폼이 카드 목록을
        // 쓰기 때문에, 고른 사람이 있든 없든 담아 둔다.
        model.addAttribute("allItems", adminBoothService.findAllItems());

        UUID selected = userId != null ? userId : users.stream().findFirst().map(u -> u.id()).orElse(null);
        if (selected == null) {
            return;
        }

        model.addAttribute("user", adminUserService.findUser(selected));
        model.addAttribute("haveItems", adminUserService.findHaveItems(selected));
        model.addAttribute("wantItems", adminUserService.findWantItems(selected));
        model.addAttribute("userExchanges", adminExchangeService.findExchangesOf(selected));

        // 이 사람을 찔러볼 더미를 고르는 자리다. 자기 자신은 뺀다.
        model.addAttribute("dummies", users.stream()
                .filter(u -> u.dummy() && !u.id().equals(selected))
                .toList());
    }

    /**
     * 교환 목록. 상태로 거르되 개수는 항상 전체 기준으로 센다.
     *
     * <p>끝난 것까지 다 보이는 것이 기본이다. 진행 중인 것만 보여 주면 방금 완료 처리한 건이
     * 목록에서 사라져서, 제대로 처리된 것인지 확인할 방법이 없다.
     */
    private void exchanges(ExchangeStatus status, Model model) {
        List<ExchangeView> all = adminExchangeService.findExchanges();

        model.addAttribute("exchanges", all);
        model.addAttribute("status", status);
        model.addAttribute("shown", status == null
                ? all
                : all.stream().filter(view -> view.status() == status).toList());

        // 상태 칸은 건수가 0 이어도 자리를 지킨다. 있다 없다 하면 누를 자리가 매번 움직인다.
        Map<ExchangeStatus, Long> counts = new LinkedHashMap<>();
        for (ExchangeStatus value : ExchangeStatus.values()) {
            counts.put(value, all.stream().filter(view -> view.status() == value).count());
        }
        model.addAttribute("statusCounts", counts);

        // 만날 자리를 옮기는 드롭다운이 쓴다. 부스가 하나라 전부 담아도 몇 개 안 된다.
        model.addAttribute("allZones", adminBoothService.findAllZones());
    }

    private void items(Long itemId, Model model) {
        var items = adminUserService.findItemDetails();
        model.addAttribute("items", items);

        Long selected = itemId != null ? itemId : items.stream().findFirst().map(i -> i.item().id()).orElse(null);
        if (selected == null) {
            return;
        }

        model.addAttribute("item", adminUserService.findItemDetail(selected));
        model.addAttribute("allUsers", adminUserService.findUsers());
    }

    private void zones(Long boothId, Model model) {
        List<?> booths = (List<?>) model.getAttribute("booths");
        if (booths == null || booths.isEmpty()) {
            return;
        }

        Long selected = boothId != null
                ? boothId
                : ((com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.BoothView) booths.get(0)).id();

        model.addAttribute("booth", adminBoothService.findBooth(selected));
        model.addAttribute("zones", adminBoothService.findZones(selected));
    }
}
