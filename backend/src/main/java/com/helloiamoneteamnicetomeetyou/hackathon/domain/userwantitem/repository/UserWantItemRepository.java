package com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserWantItemRepository extends JpaRepository<UserWantItem, Long> {

    // 내 희망 아이템 전체 (have-item 등록 후 매칭 트리거용)
    @Query("SELECT uwi FROM UserWantItem uwi JOIN FETCH uwi.item WHERE uwi.user.id = :userId")
    List<UserWantItem> findByUserId(@Param("userId") UUID userId);

    /**
     * 쿼리 A: 내 보유 아이템을 원하는 후보와 교환 가능 수량 (LEAST로 cap)
     *
     * <p>userId 를 UUID 가 아니라 String 으로 받는다. user_id 컬럼은 varchar(36) 인데,
     * 네이티브 쿼리에는 매핑 정보가 없어서 UUID 를 넘기면 Hibernate 가 binary(16) 으로
     * 바인딩한다. 그러면 비교가 전부 어긋나 결과가 0건이 된다.
     */
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
    List<Object[]> findToThemData(@Param("myUserId") String myUserId);
    @Query("""
            select w from UserWantItem w
            join fetch w.item
            where w.user.id = :userId
            order by w.id asc
            """)
    List<UserWantItem> findAllByUserId(UUID userId);

    boolean existsByUserIdAndItemId(UUID userId, Long itemId);

    /** 이 카드를 찾는 사람들. */
    @Query("""
            select w from UserWantItem w
            join fetch w.user
            where w.item.id = :itemId
            order by w.id asc
            """)
    List<UserWantItem> findAllByItemId(Long itemId);

    /** 카드별 희망 등록 수. 수요와 공급 표의 수요 쪽이다. */
    @Query("select w.item.id, count(w) from UserWantItem w group by w.item.id")
    List<Object[]> countSeekersByItem();

    /** 사용자별 희망 카드 줄 수. */
    @Query("select w.user.id, count(w) from UserWantItem w group by w.user.id")
    List<Object[]> countByUser();

    /**
     * 전부 카드까지 붙여서 읽는다. 사용자 목록이 누가 무엇을 들고 있는지 그림으로 보여 준다.
     *
     * <p>사람마다 따로 읽으면 목록 한 번 그리는 데 사람 수만큼 쿼리가 나간다. 부스 규모에서는
     * 전부 읽어 와서 메모리에서 묶는 편이 훨씬 싸다.
     */
    @Query("select w from UserWantItem w join fetch w.item join fetch w.user order by w.id asc")
    List<UserWantItem> findAllWithItem();

    void deleteByUserId(UUID userId);
}
