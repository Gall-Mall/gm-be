# Backend Javadoc Style Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 현재 백엔드 브랜치에서 추가한 운영 코드의 모든 클래스와 메서드에 기존 프로젝트 스타일의 Javadoc을 보강한다.

**Architecture:** 기능 코드는 변경하지 않고 API, 코어 도메인, 저장소 계층의 신규 타입과 메서드 바로 위에 책임 중심 Javadoc을 추가한다. 인터페이스 구현은 `{@inheritDoc}`를 사용하고 복잡한 상태 처리에만 인라인 설명을 둔다.

**Tech Stack:** Java 21, Spring Boot, Javadoc, Gradle

## Global Constraints

- 백엔드 운영 Java 코드만 변경한다.
- 테스트 코드는 변경하지 않는다.
- 현재 브랜치에서 추가한 클래스와 메서드는 모두 Javadoc을 가진다.
- 메서드 시그니처, 로직, 예외, API 경로를 변경하지 않는다.
- 기존 한국어 Javadoc 문체와 `{@inheritDoc}` 사용 방식을 유지한다.
- 커밋과 푸시는 수행하지 않는다.

---

### Task 1: API 계층 Javadoc

**Files:**
- Modify: `api/src/main/java/com/gm/api/config/SecurityConfig.java`
- Modify: `api/src/main/java/com/gm/api/controller/user/dto/response/PreviousHistoryDetailResponse.java`
- Modify: `api/src/main/java/com/gm/api/controller/store/StoreSelectionController.java`

**Interfaces:**
- Consumes: 현재 CORS, 지난 기록 응답, 식당 조회·선택 API 시그니처
- Produces: 신규 클래스·레코드·메서드 Javadoc

- [x] **Step 1: 신규 CORS Bean 메서드의 입력과 반환 책임을 문서화한다**

```java
/**
 * 허용된 프론트엔드 출처에 대해 API CORS 정책을 구성한다.
 *
 * @param allowedOrigins 자격 증명과 함께 API를 호출할 수 있는 출처 목록
 * @return {@code /api/**} 경로에 적용할 CORS 설정 소스
 */
```

- [x] **Step 2: 신규 메뉴 후보 응답 레코드와 변환 메서드를 문서화한다**

```java
/** 지난 기록에 포함할 메뉴 후보별 최종 투표 결과 응답이다. */
```

- [x] **Step 3: 신규 식당 선택 컨트롤러와 두 API 메서드를 문서화한다**

```java
/** 투표 세션의 식당 후보 조회와 최종 식당 선택 API를 제공한다. */
```

### Task 2: 코어 도메인 Javadoc

**Files:**
- Modify: `core/src/main/java/com/gm/core/domain/store/StoreSelectionService.java`
- Modify: `core/src/main/java/com/gm/core/domain/store/repository/StoreRepository.java`
- Modify: `core/src/main/java/com/gm/core/domain/vote/session/repository/VoteSessionRepository.java`
- Modify: `core/src/main/java/com/gm/core/domain/vote/session/service/VoteSessionService.java`

**Interfaces:**
- Consumes: 식당 결과 조회·선택 및 세션 완료 시그니처
- Produces: 서비스·저장소 신규 메서드 Javadoc

- [x] **Step 1: 신규 식당 선택 서비스와 모든 메서드를 문서화한다**

```java
/** 식당 후보 조회와 방장의 최종 식당 선택 및 세션 완료를 처리한다. */
```

- [x] **Step 2: 신규 저장소 조회·완료 계약에 파라미터와 반환 설명을 추가한다**

```java
/**
 * 투표 세션에 저장된 식당 후보를 조회한다.
 *
 * @param voteSessionId 투표 세션 식별자
 * @return 거리와 식별자 순으로 정렬된 식당 후보
 */
```

- [x] **Step 3: 세션 완료 서비스 메서드를 문서화한다**

```java
/**
 * 식당 선택이 끝난 투표 세션을 완료하고 완료 시각을 기록한다.
 *
 * @param voteSessionId 완료할 투표 세션 식별자
 * @param completedAt 완료 시각
 * @return 완료 상태로 변경된 투표 세션
 */
```

### Task 3: 저장소 구현 Javadoc

**Files:**
- Modify: `storage/db/src/main/java/com/gm/db/domain/history/PreviousHistoryRepositoryImpl.java`
- Modify: `storage/db/src/main/java/com/gm/db/domain/store/repository/StoreJpaRepository.java`
- Modify: `storage/db/src/main/java/com/gm/db/domain/store/repository/StoreRepositoryImpl.java`
- Modify: `storage/db/src/main/java/com/gm/db/domain/vote/session/entity/VoteSessionEntity.java`
- Modify: `storage/db/src/main/java/com/gm/db/domain/vote/session/repository/VoteSessionRepositoryImpl.java`

**Interfaces:**
- Consumes: 코어 저장소 계약
- Produces: 구현체 `{@inheritDoc}`, 파생 쿼리 설명, 엔티티·매핑 헬퍼 Javadoc

- [x] **Step 1: 코어 계약 구현 메서드에 `{@inheritDoc}`를 추가한다**

```java
/** {@inheritDoc} */
```

- [x] **Step 2: Spring Data 파생 조회 메서드의 정렬 계약을 문서화한다**

```java
/** 투표 세션의 식당 후보를 거리와 식별자 오름차순으로 조회한다. */
```

- [x] **Step 3: 엔티티 완료 메서드와 저장 모델 변환 헬퍼를 문서화한다**

```java
/**
 * 현재 세션을 완료 상태로 전환하고 완료 시각을 기록한다.
 *
 * @param completedAt 완료 시각
 */
```

### Task 4: 누락·스타일·동작 검증

**Files:**
- Verify: 모든 변경된 `src/main/**/*.java`

**Interfaces:**
- Consumes: Tasks 1-3
- Produces: Javadoc 누락 없음과 빌드·테스트 증거

- [x] **Step 1: production diff의 추가 메서드와 주석을 수동 대조한다**

Run: `git diff --unified=3 -- '*src/main/**/*.java'`

Expected: 추가 클래스·메서드 바로 위에 Javadoc 존재, 로직 변경 없음

- [x] **Step 2: 공백 오류를 확인한다**

Run: `git diff --check`

Expected: 출력 없음

- [x] **Step 3: 코어·클라이언트 테스트와 API 조립을 실행한다**

Run: `.\gradlew.bat :core:test :client:test --no-daemon`

Expected: BUILD SUCCESSFUL

Run: `.\gradlew.bat :api:assemble -x test --no-daemon`

Expected: BUILD SUCCESSFUL
