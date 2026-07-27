# 현재 백엔드 브랜치 작업 정리

- 작성일: 2026-07-27
- 대상 저장소: `gm-be`
- 현재 브랜치: `demo/backend-completion-20260726`
- 비교 기준: `origin/main` (`0655e43`)
- 현재 HEAD: `ec12c3a`
- 문서 목적: 현재 브랜치에서 수행한 작업을 커밋된 변경과 미커밋 변경으로 나누고, 파트별 변경 내용·구현 방식·변경 이유·검증 상태를 설명한다.

## 1. 전체 요약

이 브랜치의 핵심 목적은 메뉴 추천 이후의 투표 흐름을 실제 데모에서 끝까지 사용할 수 있도록 완성하는 것이다.

변경의 중심은 다음과 같다.

1. 1차 메뉴 투표를 자동 또는 수동으로 마감하고 최종 후보 수에 따라 다음 단계를 결정한다.
2. 후보가 두 개인 경우 Redis 기반 최종 투표를 수행하고, 후보가 하나이거나 세 개 이상이면 방장이 최종 메뉴를 결정한다.
3. WebSocket/STOMP에 JWT 인증과 세션별 구독 권한 검증을 추가한다.
4. WebSocket 이벤트를 놓친 클라이언트가 REST API로 현재 상태를 다시 동기화할 수 있게 한다.
5. 메뉴 확정 이후 식당 검색 결과를 조회하고 방장이 최종 식당을 선택해 세션을 완료할 수 있게 한다.
6. 완료된 세션의 식당뿐 아니라 메뉴 후보별 최종 투표 집계까지 지난 기록에서 조회할 수 있게 한다.
7. 방장이 입력한 당일 선호·비선호 키워드가 실제 추천 결과에 강하게 반영되도록 추천 파이프라인을 보완한다.
8. 후보가 0개가 된 경우에도 같은 세션에서 재추천할 수 있도록 상태 전이와 후보 교체 방식을 수정한다.
9. 브라우저 프런트엔드가 쿠키와 Authorization 헤더를 사용해 API를 호출할 수 있도록 CORS 설정을 추가한다.

결과적으로 백엔드 흐름은 다음 단계까지 연결된다.

```text
네이버 로그인
→ 온보딩 및 음식 설정
→ 그룹 생성·초대
→ 수동 투표 세션 생성
→ 메뉴 추천
→ 1차 메뉴 투표
→ 최종 메뉴 결정
→ 식당 검색
→ 최종 식당 선택
→ 세션 완료
→ 지난 기록 조회
```

## 2. Git 기준 변경 범위

### 2.1 커밋된 변경

현재 브랜치는 `origin/main`보다 4커밋 앞서 있다.

| 커밋 | 구분 | 내용 |
|---|---|---|
| `53b1207` | 기능 | 메뉴 투표 자동 마감 및 최종 메뉴 선택 구현 |
| `a68aaf6` | 기능 | 투표 실시간 상태 동기화 추가 |
| `432c62d` | 리팩터링 | 투표 후보 패키지를 역할별 하위 패키지로 정리 |
| `ec12c3a` | 병합 | `main` 변경을 기능 브랜치에 병합 |

`origin/main...HEAD` 기준 커밋 변경량은 다음과 같다.

- 변경 파일: 88개
- 추가: 3,074줄
- 삭제: 188줄

### 2.2 아직 커밋되지 않은 변경

이 문서를 만들기 직전 기준으로 작업트리에는 다음 변경이 남아 있었다.

- 수정된 추적 파일: 31개
- 추적 파일 변경량: 731줄 추가, 38줄 삭제
- 새 파일: 15개

따라서 현재 브랜치 작업을 이해할 때는 커밋된 메뉴 투표·WebSocket 작업뿐 아니라, 작업트리에 남아 있는 식당 선택·추천 개선·기록 상세화·CORS 작업도 함께 확인해야 한다.

## 3. 메뉴 투표 자동 마감과 최종 메뉴 결정

### 3.1 변경한 내용

1차 메뉴 투표를 Redis에서 원자적으로 마감하고, 고정된 최종 집계를 DB에 저장하도록 변경했다.

마감 이후에는 남은 후보 수에 따라 다음 흐름으로 분기한다.

| 남은 후보 수 | 현재 처리 |
|---:|---|
| 0개 | `MENU_SELECTION`으로 이동한 뒤 방장이 재추천을 선택 |
| 1개 | 방장이 현재 후보 확정 또는 재추천 선택 |
| 2개 | ACTIVE 그룹원이 Redis 기반 최종 투표 진행 |
| 3개 이상 | 방장이 남은 후보 중 하나를 직접 선택 |

추가된 주요 API는 다음과 같다.

| Method | 경로 | 역할 |
|---|---|---|
| `PUT` | `/api/groups/{groupId}/vote-sessions/{voteSessionId}/menu-candidates/close` | 방장 수동 마감 |
| `PUT` | `/api/groups/{groupId}/vote-sessions/{voteSessionId}/menu-candidates/{candidateId}/final-vote` | 두 후보 최종 투표 |
| `PUT` | `/api/groups/{groupId}/vote-sessions/{voteSessionId}/menu-candidates/{candidateId}/final-selection` | 방장 최종 메뉴 선택 |
| `PUT` | `/api/groups/{groupId}/vote-sessions/{voteSessionId}/menu-candidates/re-recommend` | 후보 0개·1개 상황의 재추천 |

### 3.2 구현 방식

진행 중인 사용자별 선택과 집계는 Redis에 저장하고, 투표가 끝난 뒤의 후보별 최종 집계와 선택 결과는 DB에 저장한다.

두 후보 최종 투표는 세션별 Redis Hash 하나에 다음 정보를 함께 저장한다.

- 투표 상태
- 두 후보 ID
- 투표 가능 인원 수
- 응답 완료 인원 수
- 후보별 득표수
- 사용자별 선택
- 최종 선택 후보 또는 동점 상태

제출·선택 변경·마감은 Lua Script로 처리해 동시 요청에서도 다음 조건이 깨지지 않게 했다.

- 같은 사용자의 재시도는 중복 집계하지 않는다.
- 선택 변경 시 이전 후보 집계를 감소시키고 새 후보 집계를 증가시킨다.
- 최초 응답일 때만 전체 응답자 수를 증가시킨다.
- 마지막 응답 이후 단독 1위와 동점을 원자적으로 판정한다.

DB에서는 세션 행과 후보 행을 잠근 뒤 최종 후보 하나만 `selected=true`로 변경하고, 같은 트랜잭션에서 세션을 `RESTAURANT_SEARCHING`으로 전이한다.

### 3.3 왜 이렇게 변경했는가

투표 도중에는 선택 변경과 동시 요청이 많이 발생하기 때문에 모든 사용자 선택을 DB에 계속 쓰면 락 경합과 쓰기 비용이 커진다. Redis Hash와 Lua Script를 사용하면 한 세션의 상태를 한 위치에서 원자적으로 변경할 수 있다.

반대로 최종 결과는 재시작 이후에도 남아야 하므로 DB를 기준 저장소로 사용했다. Redis는 진행 상태, DB는 최종 상태라는 책임을 분리한 것이다.

또한 최종 후보 선택과 세션 상태 전이를 같은 트랜잭션으로 묶어 다음과 같은 중간 상태를 방지했다.

- 후보는 선택됐지만 세션은 아직 메뉴 선택 단계인 상태
- 세션은 식당 검색 단계인데 선택된 메뉴가 없는 상태

### 3.4 주요 파일

- `core/.../vote/candidate/service/menuvote/MenuVoteFinalizationService.java`
- `core/.../vote/candidate/service/finalmenu/FinalMenuSelectionService.java`
- `core/.../vote/candidate/service/finalmenu/ExpiredFinalMenuVoteService.java`
- `storage/redis/.../RedisFinalMenuVoteRepository.java`
- `storage/redis/.../RedisMenuVoteRepository.java`
- `storage/db/.../VoteCandidateRepositoryImpl.java`
- `api/.../MenuCandidateController.java`
- `api/.../FinalMenuVoteExpirationScheduler.java`

## 4. 후보 0개 복구와 재추천

### 4.1 변경 전 문제

1차 투표 결과 모든 후보가 제외되면 세션이 곧바로 `MENU_RECOMMENDING`으로 이동했다. 그러나 추천 이벤트를 다시 발행하는 명확한 진입점이 없어 세션이 추천 대기 상태에 멈출 수 있었다.

또한 기존 후보가 DB에 그대로 남아 있으면 새 추천 후보 저장 시 동일 세션의 `displayOrder` 유일성 제약과 충돌할 수 있었다.

### 4.2 변경한 방식

후보가 0개여도 우선 `MENU_SELECTION`으로 이동시킨다. 이후 방장이 재추천 API를 호출하면 다음 처리를 한 곳에서 수행한다.

1. 방장 권한 확인
2. 그룹 ID와 세션 소속 일치 확인
3. 남은 후보가 0개 또는 1개인지 확인
4. 세션을 `MENU_RECOMMENDING`으로 전환
5. DB 커밋 이후 `SurveyRequested` 이벤트 발행

이미 `MENU_RECOMMENDING`인데 후보가 없는 중단 상태에서도 같은 API를 다시 호출하면 추천 이벤트를 재발행할 수 있게 했다.

새 추천 후보 저장 시에는 기존 세션 후보를 먼저 일괄 삭제하고 flush한 다음 새 후보를 저장한다.

### 4.3 변경 이유

상태 전이와 이벤트 발행을 재추천 API 한 곳에 모으면 자동 마감 로직이 메시지 발행까지 책임지지 않아도 된다. 방장이 화면에서 재추천을 명시적으로 선택한다는 제품 흐름과도 맞는다.

기존 후보를 새 후보와 교체하는 방식은 다음 문제를 함께 해결한다.

- 이전 라운드 후보가 화면에 다시 노출되는 문제
- 동일 `displayOrder` 충돌
- 재추천 후 지난 후보와 새 후보가 섞이는 문제

대신 이전에 노출된 메뉴 ID는 후보 삭제 전에 조회해 추천 제외 목록으로 전달한다. 즉, DB 행은 교체하지만 메뉴 ID는 추천 필터에 사용한다.

## 5. 추천 품질과 당일 키워드 반영

### 5.1 세션 키워드 우선 반영

투표 세션 생성 시 저장된 `likeKeyword`와 `dislikeKeyword`를 추천 생성 과정에서 읽어 다음 접두어를 붙인다.

- `오늘의 핵심 선호:`
- `오늘의 핵심 비선호:`

이 값을 기존 그룹원 온보딩 선호보다 앞에 배치해 OpenAI 큐레이션으로 전달한다.

OpenAI 시스템 프롬프트에도 다음 우선순위를 명시했다.

1. 알레르기 안전 규칙
2. 오늘의 핵심 선호·비선호
3. 일반 그룹원 선호·비선호
4. 카테고리 다양성

핵심 키워드는 단순 문자열 일치가 아니라 음식권, 맛, 온도, 재료, 식감, 음식 형태, 식사 상황으로 해석하게 했다.

### 5.2 후보 풀 확대

AI에 전달하기 전 결정론 후보 풀을 30개에서 250개로 확대했다.

이유는 `멕시코식`처럼 전체 메뉴 중 상대적으로 수가 적은 음식권이 상위 30개에 포함되지 않으면, AI가 해당 메뉴를 선택하고 싶어도 후보 목록에 없어 추천할 수 없기 때문이다. AI가 임의 메뉴를 생성하지 못하도록 제한한 구조를 유지하면서도 선택 가능성을 높이기 위한 변경이다.

### 5.3 카테고리 다양화

결정론 점수순을 유지하되 1차 순회에서 카테고리별 최대 3개까지만 선택한다. 목표 개수를 채우지 못하면 2차 순회에서 남은 고득점 메뉴로 채운다.

이 방식은 한 카테고리가 후보 풀 전체를 독점하는 것을 막으면서, 카테고리 수가 부족한 경우 추천 수가 불필요하게 줄어드는 것도 방지한다.

### 5.4 이전 노출 메뉴 제외

추천 생성 전에 같은 세션의 기존 후보 메뉴 ID를 조회하고 `shown` 집합으로 전달한다. 재추천 시 이전에 본 메뉴가 그대로 반복되는 문제를 줄이기 위한 변경이다.

### 5.5 멕시코 메뉴 데이터 확장

`db/mexican-menu-expansion.sql`을 추가해 다음 메뉴와 알레르기 매핑을 보강했다.

- 타코
- 엔칠라다
- 나초
- 파히타
- 칠리 콘 카르네
- 케사비리아
- 타말
- 포솔레

SQL은 고정 UUID와 `ON DUPLICATE KEY UPDATE`, `INSERT IGNORE`를 사용해 재실행 가능하게 작성했다.

### 5.6 변경 이유

현재 추천 구조에서는 AI가 결정론 후보 풀 밖의 메뉴를 만들 수 없다. 이 제약은 환각을 막는 데 필요하지만, 후보 풀이 너무 작으면 사용자가 입력한 핵심 키워드를 반영하지 못한다.

따라서 다음 방식으로 균형을 맞췄다.

- 결정론 필터와 알레르기 제외는 유지
- 후보 풀은 충분히 확대
- 카테고리 독점은 완화
- AI는 후보 목록 안에서만 선택
- 당일 키워드는 일반 취향보다 높은 우선순위로 처리

## 6. WebSocket/STOMP 실시간 동기화

### 6.1 변경한 내용

WebSocket 연결과 구독에 다음 보안 경계를 추가했다.

- Handshake endpoint: `/ws`
- STOMP CONNECT의 `Authorization: Bearer {accessToken}` 검증
- Access Token 블랙리스트 확인
- 사용자 존재 여부와 탈퇴 상태 확인
- `/topic/vote-sessions/{voteSessionId}` 형식만 구독 허용
- 해당 세션의 ACTIVE 그룹원인지 확인
- 클라이언트 STOMP SEND 전면 차단

서버가 발행하는 이벤트는 다음과 같다.

| 이벤트 | 발생 시점 |
|---|---|
| `MENU_VOTE_UPDATED` | 1차 메뉴 선택 제출·변경 후 |
| `MENU_VOTE_CLOSED` | 1차 투표 마감 DB 커밋 후 |
| `FINAL_MENU_VOTE_UPDATED` | 두 후보 최종 투표 상태 변경 후 |
| `FINAL_MENU_SELECTED` | 최종 메뉴 DB 확정 후 |

### 6.2 현재 상태 REST API

다음 API를 추가했다.

```http
GET /api/groups/{groupId}/vote-sessions/{voteSessionId}/vote-state
```

응답에는 다음 정보가 포함된다.

- 현재 세션 상태
- 메뉴 후보 목록
- Redis의 진행 중 1차 투표 상태와 집계
- Redis의 최종 후보 투표 상태와 집계
- DB에 확정된 최종 메뉴

### 6.3 변경 이유

WebSocket 이벤트는 일시적인 알림이므로 네트워크 단절이나 화면 이탈 중에 놓칠 수 있다. WebSocket 이벤트 자체를 기준 상태로 사용하면 클라이언트마다 화면 상태가 달라질 수 있다.

따라서 다음 원칙을 적용했다.

- WebSocket: 상태가 바뀌었다는 알림
- REST `vote-state`: 재접속 시 사용하는 기준 상태
- Redis: 진행 중 투표의 기준 상태
- DB: 세션과 최종 결과의 기준 상태

이 구조를 사용하면 클라이언트는 구독 직후와 재연결 직후 REST 상태를 다시 읽어 일관된 화면을 복구할 수 있다.

## 7. 투표 후보 패키지 구조 정리

### 7.1 변경한 내용

기존 `candidate` 하위에 섞여 있던 모델·저장소·서비스를 역할별로 분리했다.

```text
candidate
├─ model
│  ├─ menu
│  ├─ menuvote
│  ├─ finalmenu
│  └─ state
├─ repository
│  ├─ menu
│  ├─ menuvote
│  └─ finalmenu
└─ service
   ├─ menu
   ├─ menuvote
   ├─ finalmenu
   └─ state
```

API, 스케줄러, DB 어댑터, Redis 어댑터, 테스트의 import도 새 패키지 경로로 맞췄다.

### 7.2 변경 이유

메뉴 후보 정보, 1차 투표, 최종 투표, 현재 상태 복구가 한 패키지에 섞이면서 클래스 역할을 찾기 어려워졌다. 기능별 하위 패키지로 나누면 다음 장점이 있다.

- 같은 기능의 모델·저장소·서비스를 빠르게 찾을 수 있다.
- 최종 투표와 1차 투표의 Redis 경계를 구분하기 쉽다.
- 신규 기능이 추가돼도 상위 패키지가 다시 비대해지는 것을 줄인다.

이 변경은 기능 동작을 바꾸기 위한 것이 아니라 구조와 탐색성을 개선하기 위한 리팩터링이다.

## 8. 식당 검색 결과 조회와 최종 식당 선택

### 8.1 추가된 API

| Method | 경로 | 권한 |
|---|---|---|
| `GET` | `/api/groups/{groupId}/vote-sessions/{voteSessionId}/stores` | ACTIVE 그룹원 |
| `PUT` | `/api/groups/{groupId}/vote-sessions/{voteSessionId}/stores/{externalPlaceId}/selection` | 방장 |

### 8.2 조회 처리

식당 결과 조회 시 다음 항목을 검증한다.

1. 세션 존재 여부
2. URL의 `groupId`와 세션의 실제 그룹 ID 일치 여부
3. 요청 사용자의 ACTIVE 그룹원 여부
4. 세션 상태가 `RESTAURANT_SELECTION` 또는 `COMPLETED`인지 여부

결과는 거리 오름차순, 동일 거리에서는 ID 오름차순으로 반환한다.

### 8.3 최종 선택 처리

방장이 식당을 선택할 때는 다음 작업을 하나의 트랜잭션에서 수행한다.

1. 세션 행 잠금 조회
2. 그룹·세션 경로 일치 확인
3. 방장 권한 확인
4. `RESTAURANT_SELECTION` 상태 확인
5. 세션에 저장된 `externalPlaceId`인지 확인
6. 해당 식당을 최종 선택 상태로 변경
7. 세션을 `COMPLETED`로 변경
8. `closedAt` 기록

### 8.4 변경 이유

기존에는 비동기 식당 검색과 저장까지만 있고, 저장된 결과를 다시 읽거나 최종 식당을 선택해 세션을 완료하는 외부 API가 없었다. 이 때문에 메뉴는 확정돼도 실제 사용자 흐름이 완료 기록까지 이어지지 않았다.

최종 식당 선택과 세션 완료를 같은 트랜잭션으로 묶은 이유는 식당 선택만 저장되거나 세션 상태만 완료되는 부분 성공을 막기 위해서다.

## 9. 지난 기록 상세 응답 보강

### 9.1 변경한 내용

기존 완료 세션 상세는 그룹·세션·최종 식당 요약과 일부 집계만 반환했다. 여기에 당시 노출된 모든 메뉴 후보의 최종 결과를 추가했다.

후보별로 다음 값을 반환한다.

- 메뉴 ID
- 메뉴명
- 이미지 URL
- 노출 순서
- 최종 선택 여부
- GO 수
- MAYBE 수
- NO 수
- 응답자 수
- 최종 판정 상태

QueryDSL 조회는 `vote_candidate`와 `menu`를 조인하고 `displayOrder`, 후보 ID 순으로 정렬한다.

### 9.2 변경 이유

최종 식당만 보여주면 사용자가 왜 그 결과가 나왔는지 확인할 수 없다. 후보별 최종 집계를 함께 제공하면 지난 기록 화면에서 투표 과정과 최종 선택 근거를 재구성할 수 있다.

또한 진행 중 사용자별 선택은 Redis에서 삭제되므로, DB에 남아 있는 후보별 최종 집계가 기록 화면에서 사용할 수 있는 안정적인 데이터다.

## 10. 브라우저 CORS 설정

### 10.1 변경한 내용

Spring Security에 CORS 설정을 연결하고 다음 정책을 추가했다.

- 허용 origin: `gm.web.allowed-origins`
- 기본값: `http://localhost:5173`
- 허용 Method: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`
- 허용 Header: `Authorization`, `Content-Type`
- Credentials 허용
- Preflight cache: 3,600초
- 적용 범위: `/api/**`

예제 설정에는 다음 환경변수를 추가했다.

```yaml
gm:
  web:
    allowed-origins: ${FRONTEND_ALLOWED_ORIGINS:http://localhost:5173}
```

### 10.2 변경 이유

현재 인증 방식은 Access Token 요청 헤더와 Refresh Token 쿠키를 함께 사용한다. 프런트엔드와 백엔드 origin이 다르면 브라우저가 CORS 정책에 따라 요청을 차단하므로, 허용 origin과 credentials 정책을 명시해야 한다.

모든 origin을 와일드카드로 허용하지 않고 설정값으로 제한한 이유는 인증 쿠키를 사용하는 API의 보안 경계를 유지하기 위해서다.

## 11. RabbitMQ 메시지 변환 보완

### 11.1 변경한 내용

`JacksonJsonMessageConverter`에 `DefaultJacksonJavaTypeMapper`를 연결하고 신뢰할 내부 패키지를 `com.gm.mq.event`로 제한했다.

관련 테스트에서는 `EventEnvelope`를 메시지로 변환한 뒤 다시 내부 이벤트 봉투로 역직렬화할 수 있는지 확인한다.

### 11.2 변경 이유

Spring AMQP의 타입 헤더를 사용해 내부 이벤트를 복원하려면 역직렬화가 허용된 패키지를 명시해야 한다. 신뢰 패키지를 전체 와일드카드로 열지 않고 내부 이벤트 패키지로 제한해 불필요한 역직렬화 범위를 줄였다.

## 12. 테스트 변경

변경된 기능마다 다음 테스트를 추가하거나 보강했다.

### 12.1 메뉴 투표·최종 선택

- 후보 수별 마감 후 상태
- 수동·자동 마감
- Redis 선택 제출·변경·재시도
- 전원 완료 후 단독 1위와 동점 처리
- 방장 선택 권한
- 다른 그룹 세션 접근 차단
- DB 최종 후보 하나만 선택되는 제약
- Redis 만료 최종 투표 처리
- WebSocket 이벤트 발행 시점

### 12.2 WebSocket

- STOMP CONNECT JWT 검증
- 블랙리스트 Access Token 차단
- 세션 구독 경로 검증
- ACTIVE 그룹원 구독 허용
- 다른 그룹 세션 구독 차단
- 클라이언트 SEND 차단

### 12.3 식당

- 그룹원 결과 조회
- 거리순 정렬
- 다른 그룹 세션 접근 차단
- 방장만 최종 선택 가능
- 존재하지 않는 외부 장소 ID 처리
- 잘못된 세션 상태 처리
- 식당 선택과 세션 완료 처리

### 12.4 추천

- 세션 핵심 키워드가 큐레이션까지 전달되는지 확인
- OpenAI 프롬프트에 우선순위 규칙이 포함되는지 확인
- 이전 노출 메뉴 제외
- 후보 풀 250개 전달
- 카테고리별 우선 분산
- 후보 0개 재추천 흐름

### 12.5 기타

- 설정된 프런트엔드 origin의 CORS preflight
- RabbitMQ 내부 이벤트 봉투 역직렬화
- 지난 기록 후보 집계 포함

## 13. 계층별 변경 파일 정리

| 파트 | 대표 파일 | 변경 역할 |
|---|---|---|
| API 보안 | `SecurityConfig.java` | CORS, WebSocket·API 인증 경계 |
| API 투표 | `MenuCandidateController.java` | 투표 마감, 최종 투표, 직접 선택, 재추천 |
| API 상태 | `VoteCurrentStateController.java` | 재연결용 현재 상태 조회 |
| API 식당 | `StoreSelectionController.java` | 식당 목록과 최종 선택 |
| API 기록 | `PreviousHistoryDetailResponse.java` | 후보별 최종 집계 응답 |
| WebSocket | `WebSocketConfig.java` | STOMP endpoint와 broker 설정 |
| WebSocket 인증 | `VoteStompAuthenticationInterceptor.java` | CONNECT JWT 검증 |
| WebSocket 인가 | `VoteStompSubscriptionInterceptor.java` | 세션별 구독 권한 검증 |
| Core 추천 | `MenuRecommendationService.java` | 세션 키워드, 이전 후보 제외, 후보 풀 확대 |
| Core 점수 | `RecommendationService.java` | 카테고리 다양화 |
| Core 1차 투표 | `MenuVoteFinalizationService.java` | 최종 집계와 후보 수별 상태 |
| Core 최종 투표 | `FinalMenuSelectionService.java` | 1·2·3개 이상 후보 처리와 재추천 |
| Core 식당 | `StoreSelectionService.java` | 조회 권한, 방장 선택, 완료 트랜잭션 |
| Core 기록 | `PreviousHistoryService.java` | 완료 세션 후보 집계 결합 |
| DB 후보 | `VoteCandidateRepositoryImpl.java` | 최종 선택과 재추천 후보 교체 |
| DB 식당 | `StoreRepositoryImpl.java` | 식당 결과 조회와 최종 선택 |
| DB 세션 | `VoteSessionEntity.java` | `COMPLETED` 전이와 `closedAt` |
| DB 기록 | `PreviousHistoryRepositoryImpl.java` | 후보·메뉴 QueryDSL 조회 |
| Redis | `RedisFinalMenuVoteRepository.java` | 두 후보 최종 투표 원자 처리 |
| MQ | `RabbitMQConfig.java` | 내부 이벤트 타입 역직렬화 |
| 데이터 | `mexican-menu-expansion.sql` | 멕시코 메뉴와 알레르기 매핑 |

## 14. 현재 검증 상태

### 14.1 전체 테스트 실행 결과

다음 명령을 실행했다.

```powershell
.\gradlew.bat test
```

결과:

- 실행된 API 테스트: 264개
- 실패: 34개
- 빌드 결과: 실패

실패한 테스트 묶음은 다음과 같다.

| 테스트 묶음 | 테스트 수 | 실패 |
|---|---:|---:|
| `GroupIntegrationTest` | 22 | 11 |
| `InviteIntegrationTest` | 15 | 11 |
| `InviteJoinHourlyRateLimitTest` | 1 | 1 |
| `VoteSessionIntegrationTest` | 8 | 8 |
| `JwtAuthenticationFilterTest` | 8 | 1 |
| `MenuCandidateRepositoryIntegrationTest` | 2 | 2 |

33개 통합 테스트 실패의 주요 공통 원인은 테스트 fixture가 `user` 행보다 `dining_group` 행을 먼저 만들면서 발생한 외래 키 오류다.

```text
FK_dining_group_owner
dining_group.owner_user_id → user.id
```

나머지 1개는 `JwtAuthenticationFilterTest`의 Mockito 검증 불일치다.

따라서 현재 상태를 “전체 테스트 통과”로 볼 수 없다. 기능별 구현과 테스트 추가는 존재하지만, 통합 테스트 fixture 순서와 JWT 필터 테스트 기대값을 정리한 뒤 전체 회귀 테스트를 다시 실행해야 한다.

### 14.2 Git/EOL 주의

여러 작업 파일에서 다음 경고가 발생했다.

```text
LF will be replaced by CRLF the next time Git touches it
```

내용 변경과 줄바꿈 변경이 섞이면 리뷰 diff가 커질 수 있으므로 커밋 전에 EOL을 확인해야 한다.

### 14.3 외부 연동 검증

현재 코드에는 Naver OAuth, OpenAI, Kakao 장소 검색, MySQL, Redis, RabbitMQ 설정이 필요하다. 로컬 단위·통합 테스트와 별개로 실제 자격 증명을 사용한 전체 브라우저 흐름 검증은 추가로 필요하다.

## 15. 남은 위험과 후속 작업

1. 전체 테스트 34개 실패를 해결하고 다시 전체 테스트를 실행해야 한다.
2. 미커밋 변경을 기능 단위로 나눠 리뷰 가능한 커밋으로 정리해야 한다.
3. EOL 변경이 실제 내용 diff에 섞이지 않았는지 확인해야 한다.
4. Redis 최종 투표 TTL 만료 후 DB에 결과가 확정되지 않은 경우의 복구 정책이 필요하다.
5. 후보가 4개 이상 남았을 때 방장 선택을 허용하는 현재 정책을 제품 요구사항으로 확정해야 한다.
6. 추천 완료·식당 검색 완료 RabbitMQ 이벤트를 WebSocket으로 전달하는 listener TODO가 남아 있다.
7. SOLAPI 카카오 알림 발송은 아직 구현되지 않았다.
8. 식당 검색 요청이 현재 요청 본문의 좌표를 사용하므로, 그룹 저장 좌표만 사용하도록 강제할지 제품 정책을 확정해야 한다.
9. 실제 프런트엔드에서 WebSocket 이벤트와 `vote-state` 복구 API를 연결해야 한다.
10. `mexican-menu-expansion.sql`의 운영 DB 적용 방식과 실행 순서를 확정해야 한다.

## 16. 변경 방향의 핵심 판단

이번 변경에서 공통적으로 적용한 판단은 다음과 같다.

### 진행 상태와 최종 상태를 분리

- 진행 중 투표: Redis
- 최종 후보·식당·세션 결과: DB
- 실시간 알림: WebSocket
- 재연결 기준 상태: REST

각 저장소와 통신 방식이 잘하는 역할에 집중하도록 책임을 분리했다.

### 권한 검증을 서비스 계층에서 다시 수행

HTTP 인증만으로는 다른 그룹 ID나 세션 ID를 조합한 접근을 막을 수 없다. Controller에서 받은 `groupId`, `voteSessionId`, 사용자 ID를 Core 서비스에서 다시 검증해 외부 진입 방식과 무관하게 같은 권한 규칙을 적용했다.

### 상태 변경과 최종 선택을 같은 트랜잭션으로 처리

메뉴 선택과 식당 선택은 각각 다음 세션 상태 전이와 분리될 수 없다. 한쪽만 반영되는 부분 성공을 막기 위해 같은 트랜잭션 경계에 넣었다.

### 비동기 이벤트는 DB 커밋 이후 발행

DB 트랜잭션이 롤백됐는데 완료 이벤트만 외부로 나가는 문제를 막기 위해 `AfterCommitExecutor`를 사용한다.

### 추천 AI의 자유도를 제한하면서 입력 반영률을 높임

AI가 임의 메뉴를 생성하지 못하게 결정론 후보 풀 안에서만 선택하게 유지했다. 대신 후보 풀 확대, 카테고리 분산, 핵심 키워드 우선순위로 사용자의 당일 의도를 반영할 공간을 넓혔다.

## 17. 참고

- 이 문서는 Git 브랜치와 현재 작업트리의 실제 변경을 기준으로 작성했다.
- 노션 요구사항과 API 명세도 현재 백엔드 기준으로 별도 갱신했지만, 노션 변경은 Git 브랜치 diff에는 포함되지 않는다.
- 문서 작성 이후 이 파일 자체가 새 작업트리 변경으로 추가된다.
