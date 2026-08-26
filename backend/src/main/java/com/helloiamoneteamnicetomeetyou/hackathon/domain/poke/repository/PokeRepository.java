package com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.entity.Poke;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.enums.PokeStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PokeRepository extends JpaRepository<Poke, Long> {

    /**
     * 이 상대에게 이미 답을 기다리는 찔러보기가 있는지.
     *
     * <p>시안이 "이미 신청해 대답을 기다리고 있는 사용자에게는 재신청 불가" 라고 못박았다
     * (desc 165:3514). 찔러보기 총 횟수에는 제한이 없고, 막는 것은 같은 상대에 대한 중복뿐이다.
     */
    boolean existsByFromUserIdAndToUserIdAndStatus(
            UUID fromUserId, UUID toUserId, PokeStatus status);

    /**
     * 내가 받은 찔러보기. 최근 것이 위다.
     *
     * <p>보낸 사람과 요청받은 카드를 함께 읽는다. 화면이 "상대가 원하는 카드" 를 바로 그려야
     * 하는데, fetch join 이 없으면 줄마다 조회가 두 번씩 더 나간다.
     */
    @Query("""
            select p from Poke p
            join fetch p.fromUser
            join fetch p.requestedItem
            where p.toUser.id = :userId
              and p.status = :status
            order by p.id desc
            """)
    List<Poke> findAllByToUserIdAndStatus(UUID userId, PokeStatus status);

    /**
     * 내가 보낸 찔러보기. 최근 것이 위다.
     *
     * <p>대기 중인 상대의 카드를 화면에서 비활성화하는 데 쓰고, 알림을 놓친 뒤 다시 붙었을 때
     * 상태를 복원하는 데도 쓴다. 고른 카드까지 읽어야 성사 화면을 그릴 수 있다.
     */
    @Query("""
            select p from Poke p
            join fetch p.toUser
            join fetch p.requestedItem
            left join fetch p.chosenItem
            where p.fromUser.id = :userId
            order by p.id desc
            """)
    List<Poke> findAllByFromUserId(UUID userId);

    /** 응답 처리용. 양쪽 사람과 요청받은 카드가 전부 필요하다. */
    @Query("""
            select p from Poke p
            join fetch p.fromUser
            join fetch p.toUser
            join fetch p.requestedItem
            where p.id = :pokeId
            """)
    Optional<Poke> findByIdWithUsers(Long pokeId);
}
