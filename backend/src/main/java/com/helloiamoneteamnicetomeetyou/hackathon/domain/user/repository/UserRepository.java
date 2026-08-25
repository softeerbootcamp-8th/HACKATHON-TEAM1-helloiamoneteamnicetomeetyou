package com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
