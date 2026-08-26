package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExchangeRepository extends JpaRepository<Exchange, Long> {

    /**
     * 어드민 교환 목록이다. 구역과 부스를 같이 읽는다.
     *
     * <p>{@code zone} 이 {@code LAZY} 라 fetch join 없이 목록을 그리면 행마다 구역 조회가 따로
     * 나가고, 그 구역에서 부스 이름을 꺼낼 때 한 번 더 나간다. 최신 것을 위에 둔다.
     */
    @Query("""
            select e from Exchange e
            left join fetch e.zone z
            left join fetch z.booth
            order by e.id desc
            """)
    List<Exchange> findAllForAdmin();

    List<Exchange> findByStatusOrderByIdDesc(ExchangeStatus status);

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

    /**
     * 이 사람이 참가 중인 아직 안 끝난 교환.
     *
     * <p><b>앱을 다시 열었을 때 약속을 되찾는 유일한 길이다.</b> 화면 상태는 메모리에만 있어서
     * 새로고침 한 번에 사라지는데, 그때 어느 교환에 속해 있는지 물어볼 곳이 없으면 진행 중인
     * 약속이 통째로 없던 일이 된다.
     *
     * <p>한 사람이 동시에 여러 교환에 들어가지 않는 것이 지금 규칙이지만, 취소가 어긋나 두 개가
     * 남는 경우까지 막지는 않으므로 최신 것을 먼저 준다.
     */
    @Query("""
            select p.exchange from ExchangeParticipant p
            where p.user.id = :userId and p.exchange.status in :statuses
            order by p.exchange.id desc
            """)
    List<Exchange> findActiveByUserId(UUID userId, List<ExchangeStatus> statuses);
}
