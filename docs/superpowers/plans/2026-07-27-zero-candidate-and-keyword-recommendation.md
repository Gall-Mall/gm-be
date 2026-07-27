# Zero Candidate And Keyword Recommendation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 후보 0개 세션의 재추천을 복구하고 음식권·맛·온도·재료·식감·형태·상황 핵심 키워드에 맞는 메뉴를 최대 10개 추천한다.

**Architecture:** 메뉴 마감은 후보 수와 무관하게 최종 선택 화면 상태로 전환하고, 기존 재추천 API가 추천 상태 전환과 이벤트 발행을 담당한다. 추천은 안전 필터를 통과한 전체 메뉴를 AI에 제공하며 모든 종류의 당일 핵심 선호를 최우선으로 적용한다.

**Tech Stack:** Java, Spring, JUnit 5, Mockito, MySQL 8 SQL, OpenAI JSON API

## Global Constraints

- 기존 API 경로와 프론트 버튼 계약을 변경하지 않는다.
- 알레르기, 회원 제외 메뉴, 이전 라운드 메뉴 필터를 유지한다.
- 후보 2개 이상 재추천은 계속 거부한다.
- 현재 메뉴 마스터와 SQL은 MySQL 8.0 문법을 사용한다.
- 커밋과 푸시는 수행하지 않는다.

---

### Task 1: 후보 0개 마감 상태와 기존 세션 복구

**Files:**
- Modify: `core/src/main/java/com/gm/core/domain/vote/candidate/service/menuvote/MenuVoteFinalizationService.java`
- Modify: `core/src/main/java/com/gm/core/domain/vote/candidate/service/finalmenu/FinalMenuSelectionService.java`
- Test: `core/src/test/java/com/gm/core/domain/vote/candidate/service/menuvote/MenuVoteFinalizationServiceTest.java`
- Test: `core/src/test/java/com/gm/core/domain/vote/candidate/service/finalmenu/FinalMenuSelectionServiceTest.java`

**Interfaces:**
- Consumes: `finalizeVote(UUID)`, `reRecommendSingleCandidate(UUID, UUID, UUID)`
- Produces: 후보 0개 `MENU_SELECTION` 전환과 기존 `MENU_RECOMMENDING` 세션 이벤트 복구

- [ ] **Step 1: 후보 0개 마감이 `MENU_SELECTION`으로 전환되는 실패 테스트를 작성한다**

```java
given(voteCandidateRepository.saveMenuVoteResults(voteSessionId, rejectedResults))
        .willReturn(rejectedResults);

service().finalizeVote(voteSessionId);

verify(voteSessionRepository).updateStatus(
        voteSessionId, VoteSessionStatus.MENU_SELECTION);
```

- [ ] **Step 2: 기존 추천 상태의 후보 0개 세션 복구 실패 테스트를 작성한다**

```java
given(voteSessionRepository.findByIdForUpdate(sessionId))
        .willReturn(Optional.of(session(
                sessionId, groupId, VoteSessionStatus.MENU_RECOMMENDING)));
given(voteCandidateRepository.findRemainingCandidateIdsForUpdate(sessionId))
        .willReturn(List.of());

service().reRecommendSingleCandidate(groupId, ownerId, sessionId);

verify(eventPublisher).publish(new SurveyRequested(groupId, sessionId));
```

- [ ] **Step 3: 집중 테스트를 실행해 상태 조건 때문에 실패하는지 확인한다**

Run: `.\gradlew.bat :core:test --tests "*MenuVoteFinalizationServiceTest*" --tests "*FinalMenuSelectionServiceTest*"`

Expected: 후보 0개 마감은 `MENU_RECOMMENDING` 호출 불일치, 기존 세션 복구는 `VOTE-004`로 실패

- [ ] **Step 4: 마감 상태와 복구 조건을 최소 수정한다**

```java
VoteSession finalized = session.changeStatus(VoteSessionStatus.MENU_SELECTION);
```

```java
boolean newRequest = session.voteSessionStatus() == VoteSessionStatus.MENU_SELECTION
        && candidates.size() <= 1;
boolean recoverStuckRequest =
        session.voteSessionStatus() == VoteSessionStatus.MENU_RECOMMENDING
                && candidates.isEmpty();
```

- [ ] **Step 5: 집중 테스트를 통과시킨다**

Run: `.\gradlew.bat :core:test --tests "*MenuVoteFinalizationServiceTest*" --tests "*FinalMenuSelectionServiceTest*"`

Expected: PASS

### Task 2: 멕시코 메뉴 데이터 확장

**Files:**
- Create: `db/mexican-menu-expansion.sql`

**Interfaces:**
- Consumes: 기존 `양식` 카테고리 UUID와 알레르기 UUID
- Produces: 기존 2개를 포함해 총 10개가 되는 멕시코 메뉴 마스터

- [ ] **Step 1: 멱등 메뉴·알레르기 SQL을 작성한다**

```sql
INSERT INTO menu (...) VALUES
  (..., '타코', NULL, NOW(6), NOW(6)),
  (..., '엔칠라다', NULL, NOW(6), NOW(6)),
  (..., '나초', NULL, NOW(6), NOW(6)),
  (..., '파히타', NULL, NOW(6), NOW(6)),
  (..., '칠리 콘 카르네', NULL, NOW(6), NOW(6)),
  (..., '케사비리아', NULL, NOW(6), NOW(6)),
  (..., '타말', NULL, NOW(6), NOW(6)),
  (..., '포솔레', NULL, NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE name=VALUES(name), updated_at=NOW(6);
```

- [ ] **Step 2: SQL에 정확히 8개 신규 메뉴와 참조 가능한 UUID만 있는지 정적 검증한다**

Run: `Select-String -Path db/mexican-menu-expansion.sql -Pattern "NOW\\(6\\)" | Measure-Object`

Expected: 메뉴 8개와 알레르기 관계가 출력되고 트랜잭션이 `COMMIT`으로 끝남

### Task 3: 전체 후보 풀과 일반화된 핵심 키워드

**Files:**
- Modify: `core/src/main/java/com/gm/core/domain/recommendation/service/MenuRecommendationService.java`
- Modify: `client/src/main/java/com/gm/client/openai/adapter/OpenAiMenuCurationAdapter.java`
- Test: `core/src/test/java/com/gm/core/domain/recommendation/service/MenuRecommendationServiceTest.java`
- Test: `client/src/test/java/com/gm/client/openai/adapter/OpenAiMenuCurationAdapterTest.java`

**Interfaces:**
- Consumes: `VoteSession.likeKeyword()`, 안전 필터를 거친 `RecommendationService.recommend`
- Produces: 최대 250개 AI 후보 풀과 음식권·맛·온도·재료·식감·형태·상황 선택 프롬프트

- [ ] **Step 1: 전체 메뉴 후보 풀 크기 실패 테스트를 작성한다**

```java
verify(recommendationService).recommend(groupId, Set.of(), 250);
```

- [ ] **Step 2: 모든 핵심 키워드 유형의 무관 메뉴 배제 프롬프트 실패 테스트를 작성한다**

```java
assertThat(systemPrompt.getValue())
        .contains("음식권, 맛, 온도, 재료, 식감, 음식 형태, 식사 상황")
        .contains("무관한 메뉴를 섞지 않는다");
```

- [ ] **Step 3: 코어·클라이언트 집중 테스트를 실행해 실패를 확인한다**

Run: `.\gradlew.bat :core:test --tests "*MenuRecommendationServiceTest*" :client:test --tests "*OpenAiMenuCurationAdapterTest*"`

Expected: 후보 풀 30과 프롬프트 문구 부재로 실패

- [ ] **Step 4: 후보 풀 크기와 프롬프트를 최소 수정한다**

```java
private static final int CANDIDATE_POOL_SIZE = 250;
```

```text
"오늘의 핵심 선호:"의 음식권·맛·온도·재료·식감·음식 형태·식사 상황을 의미상 반영한다.
최대 개수를 채우기 위해 무관한 메뉴를 섞지 않는다.
```

- [ ] **Step 5: 집중 테스트를 통과시킨다**

Run: `.\gradlew.bat :core:test --tests "*MenuRecommendationServiceTest*" :client:test --tests "*OpenAiMenuCurationAdapterTest*"`

Expected: PASS

### Task 4: 회귀 검증

**Files:**
- Verify only: `core`
- Verify only: `client`
- Verify only: `api`

**Interfaces:**
- Consumes: Tasks 1-3
- Produces: 테스트와 빌드 증거

- [ ] **Step 1: 백엔드 전체 관련 테스트를 실행한다**

Run: `.\gradlew.bat :core:test :client:test --no-daemon`

Expected: PASS

- [ ] **Step 2: API 애플리케이션을 조립한다**

Run: `.\gradlew.bat :api:assemble -x test --no-daemon`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 현재 DB 적용 명령과 서버 재시작 절차를 전달한다**

```powershell
Get-Content .\db\mexican-menu-expansion.sql | mysql -u <user> -p <database>
.\gradlew.bat :api:bootRun
```
