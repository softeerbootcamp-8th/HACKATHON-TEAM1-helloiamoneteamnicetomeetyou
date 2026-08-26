package com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByBoothIdOrderByIdAsc(Long boothId);

    long countByBoothId(Long boothId);

    /**
     * 카드가 어느 부스 것인지. 3인 교환이 세 다리를 한 부스 안에 묶을 때 쓴다.
     *
     * <p>카드마다 엔티티를 꺼내면 N+1 이 되고, 필요한 것은 부스 id 하나뿐이다.
     */
    @Query("SELECT i.id, i.booth.id FROM Item i WHERE i.id IN :itemIds")
    List<Object[]> findBoothIdsByItemIds(@Param("itemIds") Collection<Long> itemIds);
}
