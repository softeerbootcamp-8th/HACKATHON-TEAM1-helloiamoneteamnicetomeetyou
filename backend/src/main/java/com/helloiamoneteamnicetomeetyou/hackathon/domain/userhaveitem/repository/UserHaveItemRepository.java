package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import java.util.List;
import java.util.Set;
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
     * 이 카드를 가진 사람들. 카드 화면 오른쪽에 명단 그대로 띄운다.
     *
     * <p>숫자만 보여 주면 "3명이 가지고 있다" 까지는 알아도 누구인지 알 수 없어서, 카드를
     * 옮기려면 사람 목록을 따로 뒤져야 한다.
     */
    @Query("""
            select h from UserHaveItem h
            join fetch h.user
            where h.item.id = :itemId
            order by h.id asc
            """)
    List<UserHaveItem> findAllByItemId(Long itemId);

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

    /**
     * 전부 카드까지 붙여서 읽는다. 사용자 목록이 누가 무엇을 들고 있는지 그림으로 보여 준다.
     *
     * <p>사람마다 따로 읽으면 목록 한 번 그리는 데 사람 수만큼 쿼리가 나간다. 부스 규모에서는
     * 전부 읽어 와서 메모리에서 묶는 편이 훨씬 싸다.
     */
    @Query("select h from UserHaveItem h join fetch h.item join fetch h.user order by h.id asc")
    List<UserHaveItem> findAllWithItem();

    void deleteByUserId(UUID userId);
    // 내 LEFT 아이템 전체 (want-item 등록 후 매칭 트리거용)
    @Query("SELECT uhi FROM UserHaveItem uhi JOIN FETCH uhi.item WHERE uhi.user.id = :userId AND uhi.status = 'LEFT' AND uhi.quantityLeft > 0")
    List<UserHaveItem> findMyLeftItems(@Param("userId") UUID userId);

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
          AND uhi.user_id NOT IN (
              SELECT ep.user_id FROM exchange_participants ep
              JOIN exchanges e ON e.id = ep.exchange_id
              WHERE e.status = 'PENDING'
          )
        ORDER BY uhi.created_at ASC
        """, nativeQuery = true)
    List<Object[]> findToMeData(@Param("myUserId") UUID myUserId);

    // 3인 교환 쿼리: B가 C에게 줄 수 있는 아이템과 수량 (B ∈ bIds, C ∈ cIds)
    @Query(value = """
        SELECT uhi.user_id, uwi.user_id,
               uhi.item_id,
               LEAST(uhi.quantity_left, uwi.quantity) AS qty
        FROM user_have_items uhi
        JOIN user_want_items uwi ON uwi.item_id = uhi.item_id
        WHERE uhi.user_id IN :bIds
          AND uwi.user_id IN :cIds
          AND uhi.user_id != uwi.user_id
          AND uhi.status = 'LEFT'
          AND uhi.quantity_left > 0
        ORDER BY uhi.created_at ASC
        """, nativeQuery = true)
    List<Object[]> findBToCData(@Param("bIds") Set<UUID> bIds, @Param("cIds") Set<UUID> cIds);

    // createExchange에서 UserHaveItem 엔티티 로드 (quantityLeft 감소용)
    @Query("SELECT uhi FROM UserHaveItem uhi JOIN FETCH uhi.item WHERE uhi.user.id = :userId AND uhi.item.id IN :itemIds AND uhi.status = 'LEFT' AND uhi.quantityLeft > 0")
    List<UserHaveItem> findByUserIdAndItemIds(@Param("userId") UUID userId, @Param("itemIds") Set<Long> itemIds);
}
