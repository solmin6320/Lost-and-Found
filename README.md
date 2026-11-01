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
- 배포 : Docker · AWS EC2 · CloudFront · GitHub Actions(CI · CodeQL · 배포)

- EC2 보안 그룹은 8080을 CloudFront 접두사 목록에서만 연다(인터넷에서 직접 접근 불가)
- 이벤트 흐름 — 상태가 바뀌는 곳은 이벤트만 던지고, 알림 · 채팅 전달 · 매칭은 리스너가 맡는다

| 이벤트 | 받는 곳 | 시점 |
|---|---|---|
| `NotificationEvent` | 알림 저장 + SSE 전송 | 커밋 뒤(`AFTER_COMMIT`), 새 트랜잭션 |
| `PostCreatedEvent` | 자동 매칭 · 공공 습득물 매칭 | 커밋 뒤, 비동기(`@Async`) |
| `ClaimStatusChangedEvent` | 채팅방 생성 · 읽기 전용 · 닫기 | **같은 트랜잭션**(`@EventListener`) |
| `ChatMessageSentEvent` | STOMP로 방에 전달 | 커밋 뒤 |

## 설계 포인트

- **채팅방은 요청 상태를 같은 트랜잭션에서 따라간다** — 승인은 됐는데 방이 없는 상태가 생기지 않게. 반대로 화면 전달은 커밋 뒤에만 해서 롤백된 메시지가 상대 화면에 먼저 뜨지 않는다
- **개인정보는 저장 전에 가린다** — 전화번호 · 계좌번호 · 이메일을 서버가 가린 결과와 `masked` 여부만 저장한다(원문은 남기지 않음). 가리는 순서는 이메일 → 전화번호 → 계좌번호(전화번호가 계좌번호로 잡히지 않게), 날짜는 가리지 않는다
- **채팅 구독은 숫자 방 id만 허용** — STOMP simple broker는 `*` 패턴 구독을 받아 주므로 `/topic/rooms/*`로 남의 방을 엿듣는 것을 인터셉터에서 막고, 방 당사자만 구독하게 한다
- **채팅 알림은 방마다 한 줄** — 상대가 방을 보고 있으면(`SimpUserRegistry`) 알림을 만들지 않고, 같은 방의 안 읽은 알림이 있으면 새로 만들지 않는다
- **방 · 메시지는 지우지 않는다** — 신고 증거로 남긴다. 글이 지워지면 요청 FK만 `ON DELETE SET NULL`이 되고, 그런 방은 닫힌 방으로 본다
- **N+1 없이 목록 조회** — 대화방 목록은 방 수와 상관없이 쿼리 3번(방 + 마지막 메시지 + 안 읽은 수)
- **공공데이터 XML은 DOCTYPE을 막고 읽는다** — XXE 방어
- **리프레시 토큰은 쿠키(HttpOnly), 액세스 토큰은 Bearer** — 쿠키는 재발급 한 곳에만 쓰여 CSRF 범위를 좁힌다

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

`main`에 push → CI(빌드 · 단위 · 통합 테스트 · Docker 이미지) 통과 → `Deploy` 워크플로가 EC2에 새 컨테이너를 띄운다.

