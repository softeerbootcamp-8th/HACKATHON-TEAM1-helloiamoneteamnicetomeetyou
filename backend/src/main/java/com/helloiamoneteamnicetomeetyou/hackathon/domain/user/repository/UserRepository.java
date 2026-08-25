package com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** 어드민 사용자 목록. 방금 들어온 사람이 위에 와야 부스에서 찾기 쉽다. */
    List<User> findAllByOrderByCreatedAtDesc();
}
