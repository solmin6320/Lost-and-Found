# 분실물 찾기 (Lost-and-Found) — 백엔드

잃어버린 물건과 주운 물건을 자동으로 이어 주고, 경찰청 습득물까지 함께 찾아 주는 분실물 찾기 서비스의 백엔드입니다.

- 서비스 : https://d3gltjya6f51ok.cloudfront.net

## 주요 기능

| 기능 | 내용 |
|---|---|
| 회원 · 인증 | 가입 · 로그인 · 로그아웃, JWT 액세스 토큰(Bearer) + 리프레시 토큰(HttpOnly 쿠키, 재발급 시 교체), 로그인 5회 실패 30분 잠금 |
| 게시글 | 분실 · 습득 글 등록 · 수정 · 삭제 · 상태 변경, 사진 업로드(S3), 검색 · 필터(QueryDSL), 조회수 중복 방지(Redis) |
| 댓글 | 등록 · 수정 · 삭제, 페이지 조회 |
| 주인 확인 요청 | 확인 질문에 답해 요청 → 글쓴이 승인 · 거절(승인 취소) → 받았어요(글 완료 · 되찾기 기록) |
| 실시간 알림 | SSE 스트림, 놓친 알림 다시 받기(`Last-Event-ID`), 읽음 · 모두 읽음 |
| 자동 매칭 | 분실 글 ↔ 습득 글을 조건(종류 · 날짜 창)으로 거르고 장소 · 제목 · 본문 유사도로 정렬, 후보 알림 |
| 공공 습득물 매칭 | 경찰청 습득물 공공데이터(data.go.kr)를 매일 수집해 내 분실 글과 매칭 |
| 1:1 채팅 | 요청 승인 시 방 생성, WebSocket(STOMP), 전화번호 · 계좌번호 · 이메일 자동 가림, 방 상태가 요청 상태를 따라감(승인 취소 → 읽기 전용, 받았어요 → 닫힘), 읽음 · 안 읽은 수, 차단 |
| 관리 | 역할 USER < MANAGER < ADMIN, 관리자의 매니저 임명 · 해임 |

## 기술 스택

- Java 21 · Spring Boot 4.1 (Web, Security, Data JPA, Validation, WebSocket, Actuator)
- MariaDB 11.4 · Flyway(V1 ~ V9) · QueryDSL 5.1
- Redis 7 — 리프레시 토큰, 로그인 실패 횟수, 조회수 중복 방지
- JWT(jjwt 0.12) · AWS S3(SDK v2)
- 배포 : Docker · AWS EC2 · CloudFront(`/api/*`, `/ws` → EC2) · GitHub Actions(CI · CodeQL · 배포)

## 실행하기 (로컬)

1. MariaDB · Redis 띄우기

   ```bash
   docker compose up -d
   ```

2. `src/main/resources/application-local.yml` 준비 — 비밀값이 들어가므로 리포에 올리지 않습니다(`.gitignore`). 팀에서 받거나 아래 환경 변수 표를 보고 만듭니다

3. 실행

   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```

## 환경 변수

| 이름 | 용도 |
|---|---|
| `DB_USERNAME` · `DB_PASSWORD` | MariaDB 계정 |
| `JWT_SECRET` | JWT 서명 키 |
| `CORS_ALLOWED_ORIGINS` | 허용 출처(`scheme://host[:port]`, 쉼표로 여러 개) |
| `AWS_ACCESS_KEY` · `AWS_SECRET_KEY` | S3 업로드 자격 |
| `AWS_S3_BUCKET` · `AWS_S3_BASE_URL` | 사진 버킷 · 사진 배포 주소 |
| `ADMIN_EMAILS` | 시작할 때 관리자로 지정할 이메일 |
| `PUBLIC_DATA_ENABLED` · `PUBLIC_DATA_SERVICE_KEY` | 경찰청 습득물 수집 켜기 · data.go.kr 서비스 키(디코딩 키) |

## 테스트

```bash
./gradlew test              # 단위 테스트
./gradlew integrationTest   # 통합 테스트(@Tag("integration")) — MariaDB · Redis 필요
```

통합 테스트는 실수로 다른 DB를 지우지 않도록 `lostfound_test` DB에서만 돈다. CI는 MariaDB · Redis 서비스 컨테이너로 둘 다 실행한다.

## API

| 구분 | 경로 |
|---|---|
| 인증 | `POST /api/auth/signup` · `/login` · `/reissue` · `/logout` |
| 회원 | `GET · PATCH /api/members/me` · `PATCH /api/members/me/password` · `GET /api/members/me/posts` · `GET /api/members/me/claims` |
| 게시글 | `POST · GET /api/posts` · `GET · PUT · DELETE /api/posts/{id}` · `PATCH /api/posts/{id}/status` |
| 댓글 | `POST · GET /api/posts/{postId}/comments` · `PUT · DELETE /api/comments/{commentId}` |
| 주인 확인 요청 | `POST · GET /api/posts/{postId}/claims` · `PATCH /api/claims/{claimId}/approve` · `/reject` · `/receive` |
| 알림 | `GET /api/notifications/stream`(SSE) · `GET /api/notifications` · `/unread-count` · `PATCH /{id}/read` · `/read-all` |
| 매칭 | `GET /api/posts/{postId}/matches` · `PATCH .../matches/{candidatePostId}/dismiss` |
| 공공 습득물 | `GET /api/posts/{postId}/public-matches` · `PATCH .../public-matches/{itemId}/dismiss` |
| 채팅(REST) | `GET /api/chat/rooms` · `GET /api/chat/rooms/{roomId}/messages?before=&size=` · `PATCH .../read` · `POST .../block` |
| 채팅(STOMP) | 연결 `/ws`(CONNECT 헤더 `Authorization: Bearer …`) · 구독 `/topic/rooms/{roomId}` · 보내기 `/app/rooms/{roomId}` · 오류 `/user/queue/errors` |
| 관리자 | `GET /api/admin/members` · `POST · DELETE /api/admin/managers/{memberId}` |

## 폴더 구조

```
src/main/java/com/example/lostandfound
├── config       보안 · CORS · WebSocket · S3 · 스케줄링 설정
├── controller   REST · STOMP 컨트롤러
├── dto          요청 · 응답
├── entity       JPA 엔티티 · enum
├── event        도메인 이벤트와 리스너(알림 · 채팅 전달 · 매칭)
├── exception    ErrorCode · 공통 오류 응답
├── jwt          토큰 발급 · 검증, HTTP 필터 · STOMP 연결 인증
├── repository   JPA · QueryDSL
├── security     UserDetails · 인증 실패 응답 · 채팅 구독 제한
├── client       외부 API(공공데이터)
└── service      비즈니스 로직
src/main/resources/db/migration   Flyway 스키마(V1 ~ V9)
```

## 배포

`main`에 push → CI(빌드 · 단위 · 통합 테스트 · Docker 이미지) 통과 → `Deploy` 워크플로가 EC2에 새 컨테이너를 띄운다. 운영 비밀값은 EC2 환경 변수로만 둔다.
