package com.budzet.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),
    PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    INVALID_AUTHORIZATION_HEADER(HttpStatus.UNAUTHORIZED, "Authorization 헤더 형식이 올바르지 않습니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효한 Access Token이 아닙니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효한 Refresh Token이 아닙니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "멤버를 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    CONFLICT(HttpStatus.CONFLICT, "요청이 현재 상태와 충돌합니다."),
    USER_NOT_JOINED_ROOM(HttpStatus.FORBIDDEN, "사용자가 방에 속해있지 않습니다."),
    OWNER_REQUIRED(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    REQUEST_AMOUNT_OVER_BUDGET(HttpStatus.BAD_REQUEST, "신청예산이 가용예산을 초과했습니다."),
    USER_CONFLICT(HttpStatus.CONFLICT, "이미 존재하는 회원입니다."),
    BUDGET_NOT_FOUND(HttpStatus.NOT_FOUND,"해당 모임에 등록된 예산 정보가 없습니다."),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND,"해당 모임이 존재하지 않습니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN,"해당 요청에 대한 권한이 없습니다."),
    FORBIDDEN_REQUEST_WRITER(HttpStatus.FORBIDDEN,"해당 신청의 신청자만 정산할 수 있습니다."),
    BUDGET_EXCEEDED(HttpStatus.BAD_REQUEST,"가용예산을 초과하여 차감할 수 없습니다."),
    INVITE_EXPIRED(HttpStatus.FORBIDDEN, "초대가 만료되어 모임에 참여할 수 없습니다."),
    INVITE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 초대장입니다."),
    USER_ALREADY_JOINED_ROOM(HttpStatus.CONFLICT, "이미 참여한 모임입니다."),
    BUDGET_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND,"해당 예산신청이 존재하지 않습니다."),
    BUDGET_REQUEST_NOT_APPROVED(HttpStatus.BAD_REQUEST, "승인된 예산 신청 건이 아닙니다."),
    SETTLEMENT_AMOUNT_EXCEEDS_APPROVED(HttpStatus.BAD_REQUEST,"승인된 금액을 초과하여 정산할 수 없습니다."),
    BUDGET_CHANGE_NOT_FOUND(HttpStatus.NOT_FOUND,"해당 정산내역이 존재하지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    NOT_BUDGET_REQUESTER(HttpStatus.FORBIDDEN, "예산 신청자가 아닙니다."),
    ROOM_MANAGER_REQUIRED(HttpStatus.FORBIDDEN, "모임을 관리할 권한이 없습니다."),
    INVALID_AUTHORITY(HttpStatus.BAD_REQUEST,"변경할 수 없는 권한입니다."),
    NOT_APPROVABLE(HttpStatus.CONFLICT, "정산, 반려, 승인 상태의 신청은 승인할 수 없습니다."),
    NOT_REJECTABLE(HttpStatus.CONFLICT, "정산, 반려, 승인 상태의 신청은 반려할 수 없습니다."),
    NOT_MODIFYABLE(HttpStatus.CONFLICT, "정산 상태의 신청은 수정할 수 없습니다."),
    LOCKED_RESOURCE(HttpStatus.TOO_MANY_REQUESTS,"현재 다른 요청이 처리 중입니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
