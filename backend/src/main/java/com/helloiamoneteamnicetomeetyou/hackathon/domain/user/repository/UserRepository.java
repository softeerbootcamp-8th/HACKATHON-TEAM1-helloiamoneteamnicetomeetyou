package com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** 어드민 사용자 목록. 방금 들어온 사람이 위에 와야 부스에서 찾기 쉽다. */
    List<User> findAllByOrderByCreatedAtDesc();

    /**
     * 매칭이 교환을 만들기 직전에 참가자 줄을 잠글 때 쓴다.
     *
     * <p>사용자 행 자체를 바꾸려는 것이 아니라, 같은 사람을 두 매칭이 동시에 짝지으려는 것을
     * 막을 자리가 여기밖에 없어서 이 행을 문지기로 쓴다. 교환과 참가자는 매번 새로 만들어지는
     * 행이라 잠글 대상이 없고, {@code @Version} 이 붙은 {@code UserHaveItem} 은 같은 카드를
     * 다툴 때만 막아 준다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") UUID userId);
}
