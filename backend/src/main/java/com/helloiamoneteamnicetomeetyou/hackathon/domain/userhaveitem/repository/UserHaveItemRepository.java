package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserHaveItemRepository extends JpaRepository<UserHaveItem, Long> {

    // 내 LEFT 아이템 전체 (want-item 등록 후 매칭 트리거용)
    @Query("SELECT uhi FROM UserHaveItem uhi JOIN FETCH uhi.item WHERE uhi.user.id = :userId AND uhi.status = 'LEFT' AND uhi.quantityLeft > 0")
    List<UserHaveItem> findMyLeftItems(@Param("userId") Long userId);

    // 쿼리 B: 내가 원하는 아이템을 가진 후보와 교환 가능 수량 (LEAST로 cap)
    @Query(value = """
        SELECT uhi.user_id, uhi.item_id,
               LEAST(uhi.quantity_left, my_uwi.quantity) AS qty
        FROM user_have_items uhi
        JOIN user_want_items my_uwi
            ON my_uwi.item_id = uhi.item_id
           AND my_uwi.user_id = :myUserId
        WHERE uhi.user_id != :myUserId
          AND uhi.status = 'LEFT'
          AND uhi.quantity_left > 0
        """, nativeQuery = true)
    List<Object[]> findToMeData(@Param("myUserId") Long myUserId);

    // createExchange에서 B의 UserHaveItem 엔티티 로드 (quantityLeft 감소용)
    @Query("SELECT uhi FROM UserHaveItem uhi JOIN FETCH uhi.item WHERE uhi.user.id = :userId AND uhi.item.id IN :itemIds AND uhi.status = 'LEFT' AND uhi.quantityLeft > 0")
    List<UserHaveItem> findByUserIdAndItemIds(@Param("userId") Long userId, @Param("itemIds") Set<Long> itemIds);
}
