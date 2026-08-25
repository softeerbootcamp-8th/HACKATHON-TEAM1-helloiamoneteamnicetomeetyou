package com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.repository;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoothRepository extends JpaRepository<Booth, Long> {

    List<Booth> findAllByOrderByIdAsc();
}
