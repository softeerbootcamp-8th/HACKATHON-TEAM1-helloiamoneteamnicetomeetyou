package com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import java.util.List;
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

    /** 카드별 희망 등록 수. 수요와 공급 표의 수요 쪽이다. */
    @Query("select w.item.id, count(w) from UserWantItem w group by w.item.id")
    List<Object[]> countSeekersByItem();

    /** 사용자별 희망 카드 줄 수. */
    @Query("select w.user.id, count(w) from UserWantItem w group by w.user.id")
    List<Object[]> countByUser();

    void deleteByUserId(UUID userId);
}
