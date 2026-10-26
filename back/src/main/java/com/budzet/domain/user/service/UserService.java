package com.budzet.domain.user.service;

import com.budzet.domain.user.dto.TokenRefreshResponse;
import com.budzet.domain.user.dto.UserLoginResponse;
import com.budzet.domain.user.entity.User;
import com.budzet.domain.user.repository.UserRepository;
import com.budzet.global.exception.BusinessException;
import com.budzet.global.exception.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AuthTokenService authTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User join(String email, String password, String name) {
        if(userRepository.existsByEmail(email)){
            throw new BusinessException(ErrorCode.USER_CONFLICT);
        }

        User user = new User(email, passwordEncoder.encode(password), name);
        return userRepository.save(user);
    }

    @Transactional
    public UserLoginResponse login(String email, String password){
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
        );

        checkPassword(password, user.getPassword());

        String accessToken = authTokenService.genAccessToken(user);
        String refreshToken = authTokenService.genRefreshToken(user);

        user.updateRefreshToken(refreshToken);

        return UserLoginResponse.from(
                user,
                accessToken,
                refreshToken
        );
    }

    @Transactional
    public void logout(Long userId){
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
        );

        user.clearRefreshToken();
    }

    @Transactional
    public TokenRefreshResponse refresh(String refreshToken){
        Map<String, Object> payload = authTokenService.refreshPayloadOrNull(refreshToken);

        if(payload == null){
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = ((Number)payload.get("id")).longValue();

        User user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
        );

        if(user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)){
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String newAccessToken = authTokenService.genAccessToken(user);
        String newRefreshToken = authTokenService.genRefreshToken(user);

        user.updateRefreshToken(newRefreshToken);

        return TokenRefreshResponse.from(newAccessToken, newRefreshToken);
    }

    public void checkPassword(String inputPassword, String encodedPassword) {
        if(!passwordEncoder.matches(inputPassword, encodedPassword)){
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
    }

    public Map<String, Object> accessPayloadOrNull(String accessToken) {
        return authTokenService.accessPayloadOrNull(accessToken);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User updateProfile(Long userId, String name) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
        );

        user.updateName(name);
        return user;
    }
}
