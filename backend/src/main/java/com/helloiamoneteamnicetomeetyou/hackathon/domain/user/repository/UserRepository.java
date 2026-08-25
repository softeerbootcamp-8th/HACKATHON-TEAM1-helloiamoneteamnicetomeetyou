package com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
}
