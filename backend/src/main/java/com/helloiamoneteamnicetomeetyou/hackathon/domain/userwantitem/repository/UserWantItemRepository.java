package com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWantItemRepository extends JpaRepository<UserWantItem, Long> {

    boolean existsByUserIdAndItemId(UUID userId, Long itemId);
}
