package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * 부스 안에서 <b>나를 뺀</b> 다른 사용자들의 보유 카드다.
     *
     * <p>{@code User} 와 {@code Booth} 를 잇는 관계가 없어서 "같은 부스의 사용자" 를 그 부스
     * 카드를 하나라도 보유 등록한 사람으로 유도한다. 부스에 왔지만 아직 등록하지 않은 사람은
     * 목록에 뜨지 않는다.
     *
     * <p>수량이 0 인 줄은 뺀다. 교환으로 다 나간 카드를 찔러봐도 성사될 수 없다.
     *
     * <p>정렬 기준이 행마다 달라져서(내 희망 카드인지, 줄 수 있는 카드가 있는지) SQL 로 옮길 수
     * 없다. 여기서는 {@code id} 오름차순으로만 고정해 두고 나머지는 서비스가 메모리에서 정렬한다.
     * 부스 규모에서는 전부 읽어 오는 편이 싸다.
     */
    @Query("""
            select h from UserHaveItem h
            join fetch h.item i
            join fetch h.user
            where i.booth.id = :boothId
              and h.user.id <> :userId
              and h.quantity > 0
            order by h.id asc
            """)
    List<UserHaveItem> findAllByBoothIdExcludingUser(Long boothId, UUID userId);

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

    /**
     * 쿼리 B: 내가 원하는 아이템을 가진 후보와 교환 가능 수량 (LEAST로 cap)
     *
     * <p>userId 를 UUID 가 아니라 String 으로 받는 이유는 {@code findToThemData} 와 같다.
     * user_id 는 varchar(36) 인데 네이티브 쿼리에 UUID 를 넘기면 binary(16) 으로 바인딩된다.
     */
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
              WHERE e.status IN ('PENDING', 'IN_PROGRESS')
          )
          AND NOT EXISTS (
              SELECT 1 FROM exchange_items ei
              JOIN exchanges e2 ON e2.id = ei.exchange_id
              WHERE e2.status = 'CANCELLED'
                AND ei.from_user_id = uhi.user_id
                AND ei.to_user_id = :myUserId
                AND ei.item_id = uhi.item_id
          )
        ORDER BY uhi.created_at ASC
        """, nativeQuery = true)
    List<Object[]> findToMeData(@Param("myUserId") String myUserId);

    /**
     * 3인 교환 쿼리: B가 C에게 줄 수 있는 아이템과 수량 (B ∈ bIds, C ∈ cIds)
     *
     * <p>같은 (B, 카드, C) 조합이 예전에 거절돼 취소된 적 있으면 뺀다. {@code findToThemData},
     * {@code findToMeData} 의 거절 이력 필터와 같은 규칙이다 — 사람이 아니라 그 카드 조합만
     * 막아서, B 나 C 가 다른 카드로는 계속 매칭될 수 있게 한다.
     */
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
          AND NOT EXISTS (
              SELECT 1 FROM exchange_items ei
              JOIN exchanges e2 ON e2.id = ei.exchange_id
              WHERE e2.status = 'CANCELLED'
                AND ei.from_user_id = uhi.user_id
                AND ei.to_user_id = uwi.user_id
                AND ei.item_id = uhi.item_id
          )
        ORDER BY uhi.created_at ASC
        """, nativeQuery = true)
    List<Object[]> findBToCData(@Param("bIds") Set<String> bIds, @Param("cIds") Set<String> cIds);

    // createExchange에서 UserHaveItem 엔티티 로드 (quantityLeft 감소용)
    @Query("SELECT uhi FROM UserHaveItem uhi JOIN FETCH uhi.item WHERE uhi.user.id = :userId AND uhi.item.id IN :itemIds AND uhi.status = 'LEFT' AND uhi.quantityLeft > 0")
    List<UserHaveItem> findByUserIdAndItemIds(@Param("userId") UUID userId, @Param("itemIds") Set<Long> itemIds);
}
