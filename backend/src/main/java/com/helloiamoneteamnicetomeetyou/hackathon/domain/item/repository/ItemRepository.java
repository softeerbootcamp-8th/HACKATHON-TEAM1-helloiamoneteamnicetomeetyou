package com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByBoothIdOrderByIdAsc(Long boothId);

    /**
     * 어드민 화면이 쓰는 목록. <b>부스를 같이 읽는다.</b>
     *
     * <p>카드가 어느 부스 것인지 화면에 붙이는데, {@code booth} 가 LAZY 라 그냥 전체 조회를
     * 쓰면 카드 수만큼 부스 쿼리가 더 나간다.
     */
    @Query("SELECT i FROM Item i JOIN FETCH i.booth ORDER BY i.booth.id ASC, i.id ASC")
    List<Item> findAllWithBooth();

    @Query("SELECT i FROM Item i JOIN FETCH i.booth WHERE i.booth.id = :boothId ORDER BY i.id ASC")
    List<Item> findAllWithBoothByBoothId(@Param("boothId") Long boothId);

    long countByBoothId(Long boothId);

    /**
     * 카드가 어느 부스 것인지. 3인 교환이 세 다리를 한 부스 안에 묶을 때 쓴다.
     *
     * <p>카드마다 엔티티를 꺼내면 N+1 이 되고, 필요한 것은 부스 id 하나뿐이다.
     */
    @Query("SELECT i.id, i.booth.id FROM Item i WHERE i.id IN :itemIds")
    List<Object[]> findBoothIdsByItemIds(@Param("itemIds") Collection<Long> itemIds);
}
