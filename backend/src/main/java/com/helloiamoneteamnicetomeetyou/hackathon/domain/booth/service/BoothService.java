package com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.dto.BoothResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.repository.BoothRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoothService {

    private final BoothRepository boothRepository;

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
}
