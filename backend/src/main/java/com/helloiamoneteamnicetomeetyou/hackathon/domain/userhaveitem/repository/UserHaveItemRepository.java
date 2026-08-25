package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserHaveItemRepository extends JpaRepository<UserHaveItem, Long> {

    Optional<UserHaveItem> findByUserIdAndItemId(UUID userId, Long itemId);
}
