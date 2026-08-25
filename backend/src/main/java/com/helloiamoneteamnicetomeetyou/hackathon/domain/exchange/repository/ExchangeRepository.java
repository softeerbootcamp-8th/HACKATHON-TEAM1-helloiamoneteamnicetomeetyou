package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExchangeRepository extends JpaRepository<Exchange, Long> {

    /**
     * 아직 진행 중인 교환들이 쓰고 있는 식별자.
     *
     * <p>표시와 번호를 한 정수로 합쳐서 준다. 둘을 따로 받으면 {@code Object[]} 가 되어 부르는
     * 쪽이 캐스팅을 하게 되는데, 어차피 "쓰이고 있는가" 만 보면 되는 값이라 합쳐 두는 편이 낫다.
     *
     * <p>끝나거나 취소된 교환은 빠진다. 그래서 식별자가 저절로 다시 쓸 수 있는 상태가 된다.
     */
    @Query("""
            select e.identityMark * 100 + e.identityNumber
            from Exchange e
            where e.status in :statuses
            """)
    List<Integer> findIdentityCodesByStatuses(List<ExchangeStatus> statuses);
}
