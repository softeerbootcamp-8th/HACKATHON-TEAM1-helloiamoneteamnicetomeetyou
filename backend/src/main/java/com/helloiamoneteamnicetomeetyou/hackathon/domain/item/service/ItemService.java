package com.helloiamoneteamnicetomeetyou.hackathon.domain.item.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.repository.BoothRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.dto.ItemResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.repository.ItemRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;
    private final BoothRepository boothRepository;

    /**
     * 부스가 내놓은 카드를 전부 준다.
     *
     * <p>페이징을 두지 않는다. 한 부스의 카드는 굿즈 종류만큼이라 수십 장을 넘지 않고, 등록
     * 화면이 한 번에 다 보여 주기 때문에 나눠 받을 이유가 없다.
     *
     * <p>부스가 없는 것과 부스는 있는데 카드가 아직 없는 것을 구분한다. 구분하지 않으면 프론트가
     * 빈 화면의 원인을 찾을 수 없다.
     */
    public List<ItemResponseDto> findByBooth(Long boothId) {
        if (boothId == null) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }
        if (!boothRepository.existsById(boothId)) {
            throw new ApplicationException(ErrorCode.BOOTH_NOT_FOUND);
        }

        return itemRepository.findByBoothIdOrderByIdAsc(boothId).stream()
                .map(ItemResponseDto::from)
                .toList();
    }
}
