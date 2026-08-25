package com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserWantItemRepository extends JpaRepository<UserWantItem, Long> {

    @Query("""
            select w from UserWantItem w
            join fetch w.item
            where w.user.id = :userId
            order by w.id asc
            """)
    List<UserWantItem> findAllByUserId(UUID userId);

    boolean existsByUserIdAndItemId(UUID userId, Long itemId);

    Optional<UserWantItem> findByUserIdAndItemId(UUID userId, Long itemId);

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
