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

    /**
     * 열린 부스를 전부 준다. 만든 순서대로다.
     *
     * <p>페이징을 두지 않는다. 행사에서 부스는 한 자리 수라 나눠 받을 이유가 없다.
     */
    public List<BoothResponseDto> findAll() {
        return boothRepository.findAllByOrderByIdAsc().stream()
                .map(BoothResponseDto::from)
                .toList();
    }

    /**
     * 부스 안의 교환 장소를 만든 순서대로 준다.
     *
     * <p>화면이 지도 위 핀 좌표를 이 순서에 얹기 때문에 정렬이 고정돼 있어야 한다.
     */
    public List<ZoneResponseDto> findZones(Long boothId) {
        if (!boothRepository.existsById(boothId)) {
            throw new ApplicationException(ErrorCode.BOOTH_NOT_FOUND);
        }

        return zoneRepository.findByBoothIdOrderByIdAsc(boothId).stream()
                .map(ZoneResponseDto::from)
                .toList();
    }
}
