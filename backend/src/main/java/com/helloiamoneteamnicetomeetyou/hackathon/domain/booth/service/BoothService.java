package com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.dto.BoothResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.dto.ZoneResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.repository.BoothRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.zone.repository.ZoneRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoothService {

    private final BoothRepository boothRepository;
    private final ZoneRepository zoneRepository;

    public List<BoothResponseDto> findAll() {
        return boothRepository.findAll().stream().map(BoothResponseDto::from).toList();
    }

    public List<ZoneResponseDto> findZones(Long boothId) {
        if (!boothRepository.existsById(boothId)) {
            throw new ApplicationException(ErrorCode.BOOTH_NOT_FOUND);
        }

        return zoneRepository.findByBoothIdOrderByIdAsc(boothId).stream()
                .map(ZoneResponseDto::from)
                .toList();
    }
}
