# NBE12-14-2-JAVACHIP
# 💰 Budzet

> **엑셀 없이 한눈에, 지출 승인부터 정산까지 쉬운 팀 예산 관리**

Budzet은 동아리, 학생회, 소규모 모임 등에서 발생하는 예산을  
**지출 신청 → 승인 → 예산 예약 → 실제 지출 → 정산**의 흐름으로 관리할 수 있는 팀 예산 관리 서비스입니다.

---

## 📌 프로젝트 소개

### Background

팀 단위의 예산을 관리할 때 엑셀, 메신저 등을 이용하면서 다음과 같은 문제가 발생합니다.

- 지출 신청 및 승인 여부를 한눈에 확인하기 어렵습니다.
- 승인된 금액과 실제 사용 금액을 구분하기 어렵습니다.
- 승인된 금액을 고려하지 않고 다른 지출을 진행할 수 있습니다.
- 실제 지출 후 남은 금액을 다시 예산에 반영하기 번거롭습니다.

### Solution

Budzet은 **지출 승인 시 승인 금액을 예산에서 먼저 예약**하고,  
실제 지출이 발생하면 정산을 통해 차액을 다시 예산으로 반환합니다.

```text
지출 신청
   ↓
관리자 승인
   ↓
승인 금액 예산 예약
   ↓
실제 지출
   ↓
정산
   ↓
차액 반환
```

---

## 🎯 핵심 기능

| 기능 | 설명 |
|:---:|---|
| 🔐 회원가입 / 로그인 | JWT 기반 사용자 인증 |
| 🏠 Room 관리 | 팀별 예산 관리 공간 생성 및 관리 |
| 👥 회원 관리 | OWNER / OPERATOR / MEMBER 권한 관리 |
| 📩 초대 | 초대 코드를 통한 Room 참여 |
| 💰 예산 관리 | 총 예산 및 사용 가능 예산 관리 |
| 📝 지출 신청 | 사용 목적과 신청 금액 등록 |
| ✅ 지출 승인 | OWNER / OPERATOR의 지출 승인 및 반려 |
| 🔒 예산 예약 | 승인된 금액을 사용 가능 예산에서 선반영 |
| 🧾 정산 | 실제 지출 금액 반영 및 차액 반환 |
| 📄 정산 조회 | 정산 내역 및 상세 정보 조회 |

---

## 💡 핵심 비즈니스 로직

### 예산 예약 및 정산

예를 들어 Room의 총 예산이 **1,000,000원**이고  
100,000원의 지출 신청이 승인된 경우:

```text
총 예산
1,000,000원
     │
     │ 지출 신청 100,000원
     ▼
승인
     │
     ▼
사용 가능 예산
900,000원
     │
     │ 실제 지출 85,000원
     ▼
정산
     │
     ├── 실제 지출 : 85,000원
     └── 차액 반환 : 15,000원
     │
     ▼
사용 가능 예산
915,000원
```

이를 통해 **승인된 지출 금액까지 고려한 예산 상태**를 유지합니다.

---

## 🔄 예산 상태 흐름

```text
┌──────────────┐
│ 지출 신청     │
│   REQUEST    │
└──────┬───────┘
       │ 승인
       ▼
┌──────────────┐
│ 지출 승인     │
│   APPROVE    │
└──────┬───────┘
       │ 실제 지출
       ▼
┌──────────────┐
│    정산       │
│  SETTLEMENT  │
└──────────────┘
```

반려되는 경우:

```text
REQUEST
   │
   │ 반려
   ▼
REJECT
```

---

## 👥 Room 권한

Budzet은 **Room별 회원 관계를 기준으로 권한을 관리**합니다.

| 권한 | 주요 역할 |
|:---:|---|
| 👑 OWNER | Room 및 회원에 대한 전체 관리 |
| 🛠️ OPERATOR | 지출 신청 승인/반려 및 예산 관련 업무 |
| 👤 MEMBER | 지출 신청 및 일반적인 Room 이용 |

```text
User
 │
 ▼
UserRoomConnection
 │
 ├── Room
 └── Authority
      ├── OWNER
      ├── OPERATOR
      └── MEMBER
```

---

# 🛠️ 기술 스택

### Backend

![Java](https://img.shields.io/badge/Java%2025-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)

### Frontend

![Next.js](https://img.shields.io/badge/Next.js%2016-000000?style=flat-square&logo=next.js&logoColor=white)
![React](https://img.shields.io/badge/React-61DAFB?style=flat-square&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=flat-square&logo=typescript&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-06B6D4?style=flat-square&logo=tailwindcss&logoColor=white)

### Infrastructure

![GitHub](https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white)

---

# 🏗️ 프로젝트 구조

```text
NBE12-14-2-JAVACHIP/
│
├── back/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/budzet/
│   │   │   │       ├── domain/
│   │   │   │       └── global/
│   │   │   └── resources/
│   │   │
│   │   └── test/
│   │
│   └── build.gradle
│
└── app/
    ├── components/
    ├── lib/
    │   └── api/
    ├── rooms/
    └── ...
```

---

# 🔐 인증 및 권한 처리

Budzet은 **JWT 기반 인증**을 사용합니다.

```text
Client
  │
  │ Login
  ▼
POST /users/login
  │
  ▼
JWT 발급
  │
  ▼
인증 정보 저장
  │
  ▼
API 요청
  │
  │ Authorization / Cookie
  ▼
CustomAuthenticationFilter
  │
  ▼
JWT 검증
  │
  ▼
SecurityContext
  │
  ▼
Controller
```

Room 내부의 OWNER / OPERATOR / MEMBER 권한은  
`UserRoomConnection`을 기준으로 서비스 계층에서 검증합니다.

---

# 🔒 데이터 정합성

예산 정산 과정에서는 여러 DB 작업이 하나의 작업 단위로 처리되어야 합니다.

이를 위해 `@Transactional`을 사용합니다.

```java
@Transactional
public BudgetChangeResponse createBudgetChange(...) {
    ...
}
```

정산 과정에서 문제가 발생하면 전체 작업을 롤백하여  
일부 데이터만 변경되는 상황을 방지합니다.

### 동시성 제어

동일한 Room의 예산을 동시에 변경하는 상황을 고려하여  
정산 과정에서 `PESSIMISTIC_WRITE` 기반 비관적 락을 사용합니다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
    SELECT room
    FROM Room room
    WHERE room.id = :roomId
    """)
Optional<Room> findByWithLock(@Param("roomId") Long roomId);
```

```text
동시 요청
   │
   ├──────────────┐
   │              │
   ▼              ▼
Request A      Request B
   │              │
   ▼              │
Room Lock 🔒      │
   │              │
예산 변경         │
   │              │
COMMIT            │
   │              ▼
Unlock 🔓      Room 접근
                  │
                  ▼
               최신 데이터
                  │
                  ▼
                 처리
```

> `PESSIMISTIC_WRITE`는 실제로 적용된 정산 처리 영역을 기준으로 사용합니다.

---

# 🧩 주요 기술적 해결

| 문제 | 적용 기술 | 해결 내용 |
|:---|:---|:---|
| 예산 변경 중 데이터 불일치 | `@Transactional` | 예산 변경 작업을 하나의 트랜잭션으로 처리 |
| 동시 예산 변경 | `PESSIMISTIC_WRITE` | 동일 Room의 예산 변경 요청을 순차적으로 처리 |
| 인증 | JWT | Stateless 방식의 사용자 인증 |
| Room별 권한 | UserRoomConnection | Room 단위의 OWNER / OPERATOR / MEMBER 관리 |
| API 예외 처리 | GlobalExceptionHandler | 공통 예외 응답 처리 |
| Entity / API 응답 분리 | DTO | 데이터 전달 구조와 Entity 분리 |
| 다중 API 요청 | `Promise.all` | 독립적인 API 요청을 병렬 처리 |
| 토큰 갱신 중복 | `refreshPromise` / Browser Lock | 동시에 발생하는 토큰 갱신 요청 제어 |

---

# 🌐 주요 API

### User

```http
POST   /users/join
POST   /users/login
POST   /users/refresh
DELETE /users/logout
GET    /users/me
```

### Room

```http
GET    /rooms
GET    /rooms/{roomId}
POST   /rooms
PATCH  /rooms/{roomId}
DELETE /rooms/{roomId}
```

### Member

```http
GET    /rooms/{roomId}/members
PATCH  /rooms/{roomId}/members/{userId}/authority
PATCH  /rooms/{roomId}/members/{userId}/owner
DELETE /rooms/{roomId}/members/{userId}
DELETE /rooms/{roomId}/members/me
```

### Budget Request

```http
POST   /rooms/{roomId}/budget/request
GET    /rooms/{roomId}/budget/request/list
PATCH  /rooms/{roomId}/budget/request/{requestId}/approve
PATCH  /rooms/{roomId}/budget/request/{requestId}/reject
DELETE /rooms/{roomId}/budget/request/{requestId}
```

### Settlement

```http
POST /rooms/{roomId}/budget/changes/{requestId}
GET  /rooms/{roomId}/budget/changes/settlement
GET  /rooms/{roomId}/budget/changes/{changeId}
PUT  /rooms/{roomId}/budget/changes/{changeId}
```

---

# 🚀 실행 방법

## 1. Backend

```bash
cd back

./gradlew build
./gradlew bootRun
```

Backend:

```text
http://localhost:8080
```

## 2. Frontend

```bash
npm install
npm run dev
```

Frontend:

```text
http://localhost:3000
```

### Environment

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

---

# 🤝 협업 프로세스

Budzet은 **Issue → Branch → Pull Request → Code Review → Approval → Merge**의 흐름으로 개발했습니다.

```text
┌─────────────────┐
│  GitHub Kanban  │
│      Board      │
└────────┬────────┘
         │
         ▼
     Issue 생성
         │
         ▼
    Branch 개발
         │
         ▼
   Pull Request
         │
         ▼
    Code Review
         │
    ┌────┴────┐
    │         │
 수정 필요   문제 없음
    │         │
    ▼         ▼
  수정      Approval
    │         │
    └────┬────┘
         ▼
       Merge
```

### Code Review

Code Review에서는 단순한 오류 확인뿐만 아니라 다음 사항을 함께 검토했습니다.

- 구현 방식 및 의도
- 코드 구조
- 예외 상황
- API 및 Entity 설계
- 다른 구현 방법
- 이해하기 어려운 코드에 대한 질문

코드만으로 이해하기 어려운 부분은 **예시를 공유하거나 화면을 공유하여 함께 확인**했습니다.

### 의사결정

담당자가 맡은 기능이라도 구현 방향을 개인적으로 확정하기보다  
팀원들에게 의견을 확인하고 논의한 뒤 결정했습니다.

---

# 🌿 Git Branch Strategy

```text
main
 │
 ├── feature/#139-budget-settlement
 ├── feature/#124-budzetrequest-cancellation
 ├── feature/#99-approval-member-pages
 └── ...
```

### Branch Naming

```text
feature/#<issue-number>-<feature-name>
```

예시:

```text
feature/#139-budget-settlement
```

### Commit / PR

```text
Issue 생성
    ↓
Feature Branch
    ↓
Commit
    ↓
Pull Request
    ↓
Code Review
    ↓
Approval
    ↓
Merge
```

---

# 📊 프로젝트 개발 목표

### 핵심 목표

- 팀 단위 예산 관리의 복잡성 감소
- 지출 승인과 실제 지출의 분리
- 승인 금액을 고려한 예산 관리
- 정산 후 차액의 자동 예산 반영
- Room 단위 권한 관리
- 안정적인 예산 데이터 정합성 유지

---

# 🚀 배포

## 배포 환경

| 구분 | 환경 |
| --- | --- |
| Frontend | Vercel |
| Backend | AWS Lightsail |
| Database | Aiven MySQL |
| Frontend URL | `https://budzet.chocobe.cloud` |
| Backend URL | `https://chocobe.cloud` |

Frontend는 Vercel과 GitHub Repository를 연동하여 배포하며, Backend는 Spring Boot JAR 파일을 AWS Lightsail에 직접 업로드하여 배포합니다.

---

## Frontend 배포

Frontend는 Vercel과 GitHub Repository가 연결되어 있습니다.

`main` 브랜치에 변경사항을 Push하면 Vercel에서 자동으로 배포됩니다.

```bash
git push origin main
```

Vercel 프로젝트는 다음과 같이 설정되어 있습니다.

- Root Directory: `front`
- Custom Domain: `budzet.chocobe.cloud`
- `NEXT_PUBLIC_API_BASE_URL=https://chocobe.cloud`

배포 후 다음 주소에서 Frontend의 정상 동작 여부를 확인합니다.

```text
https://budzet.chocobe.cloud
```

---

## Backend 배포

Backend는 Spring Boot JAR 파일을 빌드한 후 AWS Lightsail에 직접 업로드하여 배포합니다.

### 1. JAR 빌드

Repository의 `back` 디렉터리에서 다음 명령어를 실행합니다.

```bash
./gradlew clean bootJar
```

빌드가 완료되면 다음 경로에 JAR 파일이 생성됩니다.

```text
back/build/libs/budzet-0.0.1-SNAPSHOT.jar
```

### 2. Lightsail에 JAR 업로드

생성된 JAR 파일을 `scp` 명령어를 사용하여 Lightsail 서버에 업로드합니다.

```bash
scp -i <LIGHTSAIL_PEM_PATH> \
  build/libs/budzet-0.0.1-SNAPSHOT.jar \
  ubuntu@<LIGHTSAIL_IP>:~/budzet/
```

### 3. 기존 Spring Boot 프로세스 종료

Lightsail 서버에 SSH로 접속한 후 실행 중인 Spring Boot 프로세스를 확인합니다.

```bash
ps -ef | grep java | grep budzet
```

확인한 PID를 사용하여 기존 프로세스를 종료합니다.

```bash
kill <PID>
```

### 4. Spring Boot 실행

`budzet` 디렉터리로 이동한 후 배포용 실행 스크립트를 실행합니다.

```bash
cd ~/budzet
./start-prod.sh
```

Spring Boot가 정상적으로 실행되었는지 확인합니다.

```bash
ps -ef | grep java | grep budzet
```

필요한 경우 다음 명령어로 애플리케이션 로그를 확인합니다.

```bash
tail -f ~/budzet/budzet.log
```

### 5. 배포 확인

Frontend는 다음 주소에서 확인합니다.

```text
https://budzet.chocobe.cloud
```

Backend는 다음 주소를 사용합니다.

```text
https://chocobe.cloud
```

배포 완료 후 로그인 및 인증이 필요한 API가 정상적으로 동작하는지 확인합니다.

---

<p align="center">
  <b>Budzet</b><br>
  Team Budget Management Service
</p>
## Docker Compose 실행

프로젝트 루트에서 Makefile 명령으로 로컬 개발 환경을 관리합니다.

### 기본 개발 서비스 실행 (MySQL)

```bash
make dev-up
```

### 컨테이너 종료 및 삭제

named volume의 데이터는 유지합니다.

```bash
make dev-down
```

### 컨테이너와 DB 데이터 전체 초기화

named volume을 포함한 로컬 DB 데이터가 모두 삭제됩니다. 모니터링 컨테이너를 삭제한 후 사용하면 Prometheus·Grafana 데이터 볼륨도 삭제될 수 있습니다.

```bash
make dev-down-reset
```

### MySQL 로그 확인

실시간 로그 확인을 종료하려면 `Ctrl+C`를 누릅니다.

```bash
make db-logs
```

## 개발 환경 모니터링 (선택)

백엔드는 기존처럼 IntelliJ에서 `8080`으로 실행합니다.

Docker Desktop의 Prometheus가 `host.docker.internal:8080/actuator/prometheus`에서 15초마다 지표를 수집하고, Grafana는 Docker 내부 주소 `http://prometheus:9090`으로 저장된 지표를 조회합니다.

별도의 Docker 네트워크 생성은 필요하지 않습니다. 

Compose 기본 네트워크를 사용합니다.

이 구성은 macOS·Windows Docker Desktop의 로컬 개발용입니다.

### 최초 준비와 실행

프로젝트 루트에서 Python 3를 사용해 비밀번호 파일을 준비합니다. 

기존 파일은 덮어쓰지 않습니다.

```bash
make monitoring-init
```

| 파일 | 사용 위치 |
| --- | --- |
| `monitoring/secrets/prometheus-password` | IntelliJ의 `PROMETHEUS_PASSWORD` 환경 변수와 Prometheus 수집 인증 |
| `monitoring/secrets/grafana-admin-password` | Grafana의 `admin` 계정 초기 비밀번호 |

IntelliJ의 백엔드 Run Configuration에 `PROMETHEUS_PASSWORD`를 추가하고, 첫 번째 파일의 **내용**을 마지막 줄바꿈 없이 입력합니다.

기존 JWT 환경 변수는 유지합니다.

설정 후 백엔드를 완전히 중지하고 다시 실행합니다.

새 Spring 프로필을 추가할 필요는 없습니다.

개발용 엔드포인트는 `prod`와 `test` 프로필에서 비활성화됩니다.

```bash
make monitoring-up
```

| 서비스 | 브라우저 주소 | 인증 |
| --- | --- | --- |
| Prometheus | http://localhost:9090 | 로컬 조회 화면은 별도 로그인 없음 |
| Grafana | http://localhost:3001 | 사용자 `admin`, `grafana-admin-password` 파일 내용 |

두 화면은 호스트의 `127.0.0.1`에만 공개됩니다.

Grafana 3001번 포트는 프론트엔드 3000번과 구분합니다.

Grafana에서는 **Budzet → Budzet Overview** 대시보드가 자동 등록되고 기본 홈 화면으로 표시됩니다.

로그인 상태에서는 http://localhost:3001/d/budzet-overview 로도 접근할 수 있습니다.

Grafana 로그인은 백엔드 사용자 JWT 로그인과 별개입니다.

`/actuator/prometheus`에는 수집 전용 Basic 인증을 사용하고, 기존 API와 `/actuator/health`, `/actuator/metrics`는 JWT 인증을 유지합니다.

Grafana에는 백엔드 수집 비밀번호를 전달하지 않습니다.

`monitoring/secrets/`는 Git에서 제외되므로 각 개발자가 로컬에서 생성합니다.

### 기본 대시보드와 확인 방법

| 패널 | 의미 |
| --- | --- |
| 수집 상태 | `UP`: 최근 수집 성공, `DOWN`: 최근 수집 실패. `No data`: 조회할 수집 기록 없음 |
| JVM 힙 메모리 | 사용 중인 힙 메모리 합계. 프로세스 전체 메모리 사용량과는 다름 |
| JVM 스레드 | JVM 내부 작업을 포함한 현재 스레드 수 |
| HTTP 초당 요청 수 | 경로·메서드별 요청 발생 속도. Actuator 호출 제외 |
| HTTP 평균 응답 시간 | 최근 구간의 경로·메서드별 평균 처리 시간(ms). Actuator 호출 제외 |

1. Prometheus의 **Status → Target health**에서 `budzet`이 `UP`인지 확인합니다.
2. Grafana에 로그인하여 수집 상태와 JVM 그래프를 확인합니다.
3. Swagger에서 백엔드 사용자로 로그인한 후 `GET /rooms`를 여러 번 호출합니다.
4. 수집 주기(15초)를 기다린 후 Grafana의 **Refresh**를 눌러 HTTP 그래프의 해당 경로를 확인합니다.

HTTP 지표는 최초 요청 이후 수집되며, 초당 요청 수와 평균 시간은 여러 수집 샘플이 필요합니다.

요청이 없는 구간의 평균 응답 시간은 비어 있을 수 있습니다.

`UP`은 지표 수집 성공을 의미하며, 모든 업무 API가 정상임을 보장하지는 않습니다.

`401`로 수집이 실패하면 IntelliJ 환경 변수와 `prometheus-password` 파일 내용이 같은지 확인하고 백엔드를 재시작합니다.

### 설정 관리와 종료

연결 설정은 `monitoring/grafana/provisioning/`, 기본 대시보드는 `monitoring/grafana/dashboards/budzet-overview.json`으로 관리합니다.

기본 대시보드는 화면에서 수정·저장하지 않도록 구성했습니다.

개인 실험은 복사본으로 진행합니다.

대시보드 JSON 변경은 약 30초 내 반영되며, 연결 설정 변경은 Grafana 재시작이 필요합니다.

```bash
make monitoring-logs
make monitoring-down
```

`monitoring-down`은 두 모니터링 서비스를 중지하며 MySQL은 계속 실행합니다.

`make monitoring-up`으로 다시 시작할 수 있습니다.

Prometheus는 지표를 3일간 보관하고, Grafana의 계정·개인 설정은 `grafana-data` 볼륨에 보관합니다.

Grafana 초기 비밀번호 파일은 데이터 볼륨을 처음 생성할 때 적용되므로, 이미 사용 중인 계정의 비밀번호는 파일 수정만으로 변경되지 않습니다.

이번 구성은 로컬 개발 확인용입니다. 
