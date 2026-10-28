# 영수증 OCR (무료 플랜 전용)

1. OCR.space에서 무료 API 키를 발급합니다. 유료 구독이나 결제 정보는 등록하지 않습니다.
2. IntelliJ Run Configuration 또는 실행 환경변수에 `OCR_SPACE_API_KEY`를 설정하고 서버를 재시작합니다. 키는 Git에 커밋하지 않습니다.
3. 원화 모임의 승인된 본인 신청을 선택하고 정산 화면에서 JPEG/PNG(1MB 이하, 1,200만 화소 이하)를 선택합니다.
4. 외부 전송 안내 확인 후 분석하고, 영수증과 금액을 비교하여 적용한 뒤 기존 정산 확인을 진행합니다.

API: POST /rooms/{roomId}/budget/requests/{requestId}/receipt/analyze (multipart file).
일반 회원 인증 + 모임 멤버 + 신청자 본인 + 승인 상태를 검사합니다. OCR 분석만으로는 이미지/인식 원문을 영구 저장하지 않습니다. 정산 시 첨부한 이미지 원본은 별도 ReceiptEvidence 테이블에 저장하며 정산과 함께 커밋/롤백합니다. 외부 OCR.space에는 이미지가 전달됩니다.
공식 무료 endpoint만 사용하며 재시도·유료 fallback은 없습니다. 키 미설정/외부 오류는 503, 사용 제한은 429입니다. 직접 입력은 계속 가능합니다.

서버 인스턴스별 하루 최대 400회, 사용자별 30초 간격으로 제한합니다. 제한 카운터는 메모리 기반이므로 재시작 시 초기화되고 다중 서버 간 공유되지 않습니다. 제공자 IP·계정 제한은 별도이며 무료 키만 사용해야 합니다. 운영 환경에서 공유 제한이 필요하면 DB 등의 공용 카운터로 확장해야 합니다.

금액 추출은 명확한 합계/결제금액 라벨만 사용하며 서로 다른 합계가 나오면 자동 선택하지 않습니다. 날짜·품목 추출 및 이미지 보관은 이번 범위에서 제외합니다. 무료 서비스의 인식 품질과 가용성은 보장하지 않으며 실제 키와 개인정보를 제거한 영수증으로 별도 확인해야 합니다.


증빙 저장: POST /rooms/{roomId}/budget/changes/{requestId}/with-receipt (multipart request JSON + file). 기존 JSON 정산 API는 유지합니다. 조회: GET /rooms/{roomId}/budget/changes/{changeId}/receipt. 모임 멤버 인증 후 원본을 Base64 JSON으로 반환하며 브라우저/공개 정적 폴더에 보관하지 않습니다. 기본 ddl-auto:update로 새 테이블이 생성되므로 운영에서는 스키마 배포 절차가 필요합니다. DB 백업에 증빙도 포함해야 합니다.
