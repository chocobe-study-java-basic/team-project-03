package com.budzet.domain.user.controller;

import com.budzet.domain.user.dto.*;
import com.budzet.domain.user.entity.User;
import com.budzet.domain.user.service.UserService;
import com.budzet.global.api.ApiResponse;
import com.budzet.global.exception.BusinessException;
import com.budzet.global.exception.ErrorCode;
import com.budzet.global.rq.Rq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Tag(name = "01. 회원 & 인증", description = "회원가입, 로그인/로그아웃, 내 정보 조회 및 토큰 재발급 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final Rq rq;

    @Value("${custom.jwt.access-expire-millis}")
    private long accessExpireMillis;

    @Value("${custom.jwt.refresh-expire-millis}")
    private long refreshExpireMillis;


    @Operation(summary = "회원가입")
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<UserDto>> join(
            @RequestBody @Valid UserJoinRequest reqBody
    ){
        User user = userService.join(reqBody.email(), reqBody.password(), reqBody.name());

        return ApiResponse.response(
                HttpStatus.CREATED,
                "회원가입이 완료되었습니다.",
                new UserDto(user)
        );
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호를 확인하여 로그인")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserLoginResponse>> login(
            @RequestBody @Valid UserLoginRequest reqBody
    ){
        UserLoginResponse response = userService.login(reqBody.email(), reqBody.password());

        rq.addCookie("accessToken", response.accessToken(), Duration.ofMillis(accessExpireMillis));
        rq.addCookie("refreshToken", response.refreshToken(), Duration.ofMillis(refreshExpireMillis));

        return ApiResponse.response(
                HttpStatus.OK,
                "로그인에 성공했습니다.",
                response
        );
    }

    @Operation(
            summary = "로그아웃",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @DeleteMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(){
        User actor = rq.getActor();

        userService.logout(actor.getId());

        rq.deleteCookie("accessToken");
        rq.deleteCookie("refreshToken");

        return ApiResponse.response(
                HttpStatus.OK,
                "로그아웃되었습니다.",
                null
        );
    }

    @Operation(summary = "인증 토큰 재발급", description = "access token과 refresh token을 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh( ){
        String headerAuthorization = rq.getHeader("Authorization", "");

        String refreshToken = null;

        if(!headerAuthorization.isBlank()){
            if(!headerAuthorization.startsWith("Bearer ")){
                throw new BusinessException(ErrorCode.INVALID_AUTHORIZATION_HEADER);
            }

            refreshToken = headerAuthorization.substring(7);
        }else {
            refreshToken = rq.getCookieValue("refreshToken", "");
        }

        if(refreshToken.isBlank()){
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        TokenRefreshResponse tokenResponse = userService.refresh(refreshToken);

        rq.addCookie("accessToken", tokenResponse.accessToken(), Duration.ofMillis(accessExpireMillis));
        rq.addCookie("refreshToken", tokenResponse.refreshToken(), Duration.ofMillis(refreshExpireMillis));

        return ApiResponse.response(
                HttpStatus.OK,
                "토큰 재발급 성공",
                tokenResponse
        );
    }

    @Operation(
            summary = "내 정보 조회",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> me(){
        User actor = rq.getActor();

        return ApiResponse.response(
                HttpStatus.OK,
                "내 정보 조회에 성공했습니다.",
                new UserDto(actor)
        );
    }

    @Operation(
            summary = "내 정보 수정",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            @RequestBody @Valid UserProfileUpdateRequest reqBody
    ){
        User actor = rq.getActor();

        User updatedUser = userService.updateProfile(actor.getId(), reqBody.name());

        return ApiResponse.response(
                HttpStatus.OK,
                "내 정보가 수정되었습니다.",
                new UserDto(updatedUser)
        );
    }

}
