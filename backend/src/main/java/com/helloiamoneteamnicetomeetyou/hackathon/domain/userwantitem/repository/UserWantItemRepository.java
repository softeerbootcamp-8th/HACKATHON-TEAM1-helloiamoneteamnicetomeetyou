package com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserWantItemRepository extends JpaRepository<UserWantItem, Long> {

    // 내 희망 아이템 전체 (have-item 등록 후 매칭 트리거용)
    @Query("SELECT uwi FROM UserWantItem uwi JOIN FETCH uwi.item WHERE uwi.user.id = :userId")
    List<UserWantItem> findByUserId(@Param("userId") Long userId);

    // 쿼리 A: 내 보유 아이템을 원하는 후보와 교환 가능 수량 (LEAST로 cap)
    @Query(value = """
        SELECT uwi.user_id, uwi.item_id,
               LEAST(my_uhi.quantity_left, uwi.quantity) AS qty,
               uwi.id AS want_id
        FROM user_want_items uwi
        JOIN user_have_items my_uhi
            ON my_uhi.item_id = uwi.item_id
           AND my_uhi.user_id = :myUserId
           AND my_uhi.status = 'LEFT'
           AND my_uhi.quantity_left > 0
        WHERE uwi.user_id != :myUserId
          AND uwi.user_id NOT IN (
              SELECT ep.user_id FROM exchange_participants ep
              JOIN exchanges e ON e.id = ep.exchange_id
              WHERE e.status = 'PENDING'
          )
        ORDER BY my_uhi.created_at ASC
        """, nativeQuery = true)
    List<Object[]> findToThemData(@Param("myUserId") Long myUserId);
}
