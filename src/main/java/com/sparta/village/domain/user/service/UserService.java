package com.sparta.village.domain.user.service;


import com.sparta.village.domain.user.dto.UserResponseDto;
import com.sparta.village.domain.user.entity.User;
import com.sparta.village.domain.user.repository.UserRepository;
import com.sparta.village.global.exception.CustomException;
import com.sparta.village.global.exception.ErrorCode;
import com.sparta.village.global.exception.ResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service // spring service 등록
@RequiredArgsConstructor // final @NonNull 필드를 사용하여 생성자를 자동으로 생성
// User Service 클래스 선언
public class UserService {
    private  final UserRepository userRepository;

    @Transactional
    // 사용자의 닉네임을 업데이트하고 업데이트된 사용자를 저장하는 메소드
    public ResponseEntity<ResponseMessage> updateNickname(String newNickname, User user) {

        Optional<User> existingUser = userRepository.findByNickname(newNickname);
        if (existingUser.isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        // 사용자 객체의 닉네임을 새 닉네임으로 설정
        user.setNickname(newNickname);
        // userRepository에 변경된 사용자 정보를 저장
        userRepository.save(user);
        // 변경이 완료되었음을 알리는 응답 메시지 생성
        return ResponseMessage.SuccessResponse("변경 완료되었습니다.",new UserResponseDto(user.getProfile(), user.getNickname()));
    }
}










