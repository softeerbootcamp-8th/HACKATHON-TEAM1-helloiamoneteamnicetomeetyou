package com.helloiamoneteamnicetomeetyou.hackathon.domain.user.service;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.repository.UserRepository;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ApplicationException;
import com.helloiamoneteamnicetomeetyou.hackathon.global.exception.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * 클라이언트가 만든 UUID 를 등록한다. 이미 있으면 이름만 갱신한다.
     *
     * <p>멱등이라 프론트가 앱을 열 때마다 불러도 안전하다. 덕분에 "이 UUID 가 서버에 있는가" 를
     * 따로 묻는 조회 API 가 필요 없다.
     *
     * @return 이번 호출로 새로 만들었으면 true
     */
    @Transactional
    public boolean register(UUID userId, String username) {
        // Bean Validation 이 아직 없어서 형식 검증을 여기서 한다.
        if (userId == null) {
            throw new ApplicationException(ErrorCode.INVALID_INPUT);
        }

        // PK 를 직접 넣으면 Hibernate 가 새 엔티티인지 몰라 save() 가 merge() 로 돌면서
        // SELECT 를 한 번 더 쏜다. 여기서 미리 갈라내므로 그 경로를 타지 않는다.
        return userRepository.findById(userId)
                .map(user -> {
                    user.changeUsername(username);
                    return false;
                })
                .orElseGet(() -> {
                    userRepository.save(User.of(userId, username));
                    return true;
                });
    }
}
