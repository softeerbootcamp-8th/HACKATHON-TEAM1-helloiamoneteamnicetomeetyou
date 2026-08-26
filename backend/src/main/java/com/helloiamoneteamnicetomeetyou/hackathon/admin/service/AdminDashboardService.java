package com.helloiamoneteamnicetomeetyou.hackathon.admin.service;

import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ItemDemandView;
import com.helloiamoneteamnicetomeetyou.hackathon.admin.dto.ItemView;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository.UserHaveItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository.UserWantItemRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카드별 수요와 공급을 센다.
 *
 * <p>매칭이 안 붙을 때 부스에서 할 수 있는 판단은 둘 중 하나다. 어떤 카드를 가진 사람을 넣을
 * 것인가, 아니면 어떤 카드를 찾는 사람을 넣을 것인가. 그 판단에 필요한 것이 이 표다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final ItemRepository itemRepository;
    private final UserHaveItemRepository userHaveItemRepository;
    private final UserWantItemRepository userWantItemRepository;

    /**
     * 카드별 보유자 수와 희망자 수.
     *
     * <p>짝이 날 수 없는 카드를 위로 올린다. 부스에서는 목록을 끝까지 훑을 시간이 없어서, 손을
     * 대야 하는 것이 위에 있어야 한다.
     */
    public List<ItemDemandView> findDemand() {
        Map<Long, Long> holders = toCountMap(userHaveItemRepository.countHoldersByItem());
        Map<Long, Long> seekers = toCountMap(userWantItemRepository.countSeekersByItem());

        return itemRepository.findAllWithBooth().stream()
                .map(item -> new ItemDemandView(
                        ItemView.of(item),
                        holders.getOrDefault(item.getId(), 0L),
                        seekers.getOrDefault(item.getId(), 0L)))
                .sorted(Comparator
                        .comparing(ItemDemandView::isDeadEnd).reversed()
                        .thenComparing(view -> view.item().name()))
                .toList();
    }

    /** 짝이 날 수 없는 카드 수. 대시보드 맨 위 요약에 쓴다. */
    public long countDeadEnds(List<ItemDemandView> demand) {
        return demand.stream().filter(ItemDemandView::isDeadEnd).count();
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }
}
