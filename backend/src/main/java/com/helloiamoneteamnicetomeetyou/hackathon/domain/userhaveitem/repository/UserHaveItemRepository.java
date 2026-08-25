package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserHaveItemRepository extends JpaRepository<UserHaveItem, Long> {
}
