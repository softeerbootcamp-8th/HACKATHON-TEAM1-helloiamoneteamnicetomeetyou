package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserHaveItemRepository extends JpaRepository<UserHaveItem, Long> {

    @Query("""
            select h from UserHaveItem h
            join fetch h.item
            where h.user.id = :userId
            order by h.id asc
            """)
    List<UserHaveItem> findAllByUserId(UUID userId);

    Optional<UserHaveItem> findByUserIdAndItemId(UUID userId, Long itemId);

    /**
     * 카드별로 보유 등록이 몇 건인지 센다. 수요와 공급 표의 공급 쪽이다.
     *
     * <p>수량이 아니라 사람 수를 센다. 매칭에서 중요한 것은 그 카드를 내놓을 수 있는 사람이 몇
     * 명인가이지, 한 사람이 몇 장을 들고 있는가가 아니기 때문이다.
     */
    @Query("select h.item.id, count(h) from UserHaveItem h group by h.item.id")
    List<Object[]> countHoldersByItem();

    /** 사용자별 보유 카드 줄 수. 목록에서 사람마다 따로 세면 사람 수만큼 쿼리가 나간다. */
    @Query("select h.user.id, count(h) from UserHaveItem h group by h.user.id")
    List<Object[]> countByUser();

    void deleteByUserId(UUID userId);
}
