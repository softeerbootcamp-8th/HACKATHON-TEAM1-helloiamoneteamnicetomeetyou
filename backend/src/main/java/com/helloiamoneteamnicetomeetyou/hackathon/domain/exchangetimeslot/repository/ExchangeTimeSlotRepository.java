package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangetimeslot.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangetimeslot.entity.ExchangeTimeSlot;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExchangeTimeSlotRepository extends JpaRepository<ExchangeTimeSlot, Long> {

    @Query("select s from ExchangeTimeSlot s where s.exchange.id = :exchangeId")
    List<ExchangeTimeSlot> findAllByExchangeId(@Param("exchangeId") Long exchangeId);

    /**
     * 한 사람의 선택을 통째로 지운다. 다시 저장하기 직전에 부른다.
     *
     * <p>파생 삭제 메서드는 엔티티를 전부 읽어 하나씩 지우기 때문에 벌크로 쓴다.
     * {@code flushAutomatically} 는 지우기 전에 밀린 쓰기를 먼저 내보내서, 같은 칸을 다시 넣을 때
     * 유니크 제약에 걸리지 않게 한다.
     *
     * <p><b>{@code clearAutomatically} 를 쓰지 않는다.</b> 그걸 켜면 영속성 컨텍스트가 통째로
     * 비워져서, 이 호출 앞에서 읽어 둔 엔티티가 전부 detach 된다. 그러면 그 뒤에 lazy 필드를
     * 건드릴 때 {@code LazyInitializationException} 이 나고, 더 나쁜 것은 그 엔티티를 고쳐도
     * 더티 체킹이 안 돌아서 <b>조용히 저장되지 않는다</b>는 점이다. 여기서 지우는 행은 부르는 쪽이
     * 미리 읽지 않기 때문에 비울 이유도 없다.
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from ExchangeTimeSlot s where s.exchange.id = :exchangeId and s.user.id = :userId")
    void deleteAllByExchangeIdAndUserId(@Param("exchangeId") Long exchangeId, @Param("userId") UUID userId);

    @Modifying(flushAutomatically = true)
    @Query("delete from ExchangeTimeSlot s where s.exchange.id = :exchangeId")
    void deleteAllByExchangeId(@Param("exchangeId") Long exchangeId);
}
