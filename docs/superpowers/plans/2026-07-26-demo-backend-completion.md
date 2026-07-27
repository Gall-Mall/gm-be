# Demo Backend Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the backend compile, pass its tests, and provide every authenticated API needed for the real multi-user demo through completed restaurant history.

**Architecture:** Preserve the existing controller/core/repository boundaries and vote state machine. Add the smallest restaurant query/selection slice, close the session transactionally, and configure browser CORS without changing unrelated MQ or notification behavior.

**Tech Stack:** Java 21, Spring Boot, Spring Security, Spring Data JPA, Gradle, JUnit 5, MockMvc, H2/MySQL, Redis

## Global Constraints

- Existing backend DTOs and state names are the source of truth.
- Demo-reachable actions validate authentication, group membership, ownership, group/session association, and state.
- New production behavior is written only after a focused test fails for the expected reason.
- No notification, deployment, routing, or unrelated refactoring work.
- Existing untracked `.hermes/` content is preserved.

---

### Task 1: Restore the backend test baseline

**Files:**
- Modify: `api/src/test/java/com/gm/integration/history/PreviousHistoryIntegrationTest.java`

**Interfaces:**
- Consumes: `com.gm.core.domain.vote.candidate.model.menu.VoteCandidateResult`
- Produces: a compilable existing integration test suite

- [ ] **Step 1: Reproduce the compile failure**

Run:

```powershell
.\gradlew.bat :api:compileTestJava --console=plain
```

Expected: failure because `VoteCandidateResult` is imported from the removed parent package.

- [ ] **Step 2: Correct the stale test import**

Replace:

```java
import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;
```

with:

```java
import com.gm.core.domain.vote.candidate.model.menu.VoteCandidateResult;
```

- [ ] **Step 3: Verify the baseline reaches test execution**

Run:

```powershell
.\gradlew.bat :api:compileTestJava --console=plain
```

Expected: `BUILD SUCCESSFUL`.

---

### Task 2: Permit the configured frontend origin

**Files:**
- Create: `api/src/test/java/com/gm/api/config/CorsIntegrationTest.java`
- Modify: `api/src/main/java/com/gm/api/config/SecurityConfig.java`
- Modify: `api/src/main/resources/application-example.yml`

**Interfaces:**
- Consumes: property `gm.web.allowed-origins`
- Produces: `CorsConfigurationSource corsConfigurationSource(...)`

- [ ] **Step 1: Write the failing preflight test**

Add a MockMvc test that sends:

```java
mockMvc.perform(options("/api/groups")
        .header("Origin", "http://localhost:5173")
        .header("Access-Control-Request-Method", "GET")
        .header("Access-Control-Request-Headers", "authorization,content-type"))
    .andExpect(status().isOk())
    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
    .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
```

Configure the test with:

```java
@SpringBootTest(properties = "gm.web.allowed-origins=http://localhost:5173")
```

- [ ] **Step 2: Verify the preflight test fails**

Run:

```powershell
.\gradlew.bat :api:test --tests "com.gm.api.config.CorsIntegrationTest" --console=plain
```

Expected: missing CORS response headers.

- [ ] **Step 3: Add the minimal CORS configuration**

Enable Spring Security CORS and expose a configuration source that:

```java
configuration.setAllowedOrigins(allowedOrigins);
configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
configuration.setAllowCredentials(true);
configuration.setMaxAge(3600L);
source.registerCorsConfiguration("/api/**", configuration);
```

Add this example property:

```yaml
gm:
  web:
    allowed-origins: ${FRONTEND_ALLOWED_ORIGINS:http://localhost:5173}
```

- [ ] **Step 4: Verify the CORS test passes**

Run the focused test again and expect `BUILD SUCCESSFUL`.

---

### Task 3: Query persisted restaurant results

**Files:**
- Modify: `core/src/main/java/com/gm/core/domain/store/repository/StoreRepository.java`
- Modify: `storage/db/src/main/java/com/gm/db/domain/store/repository/StoreJpaRepository.java`
- Modify: `storage/db/src/main/java/com/gm/db/domain/store/repository/StoreRepositoryImpl.java`
- Modify: `storage/db/src/main/java/com/gm/db/domain/store/mapper/StoreMapper.java`
- Modify: `api/src/test/java/com/gm/api/controller/store/StoreControllerIntegrationTest.java`

**Interfaces:**
- Produces: `List<Store> findAllByVoteSessionId(UUID voteSessionId)`
- Produces: distance-ascending stable restaurant results

- [ ] **Step 1: Add a failing repository-backed API fixture assertion**

Extend the store integration fixture with persisted restaurants at distances
`300`, `100`, and `200`, then assert the future listing endpoint returns
external place IDs in `100`, `200`, `300` order.

- [ ] **Step 2: Verify failure because listing is unavailable**

Run:

```powershell
.\gradlew.bat :api:test --tests "com.gm.api.controller.store.StoreControllerIntegrationTest" --console=plain
```

Expected: the requested GET endpoint returns a non-success status.

- [ ] **Step 3: Add the repository query and domain mapping**

Add:

```java
List<StoreEntity> findAllByVoteSessionIdOrderByDistanceAscIdAsc(UUID voteSessionId);
```

and map each entity to:

```java
new Store(
    entity.getExternalPlaceId(),
    entity.getName(),
    entity.getAddress(),
    null,
    entity.getUrl(),
    new Coordinate(entity.getLongitude(), entity.getLatitude()),
    entity.getProvider(),
    String.valueOf(entity.getDistance())
)
```

The API may return `categoryName: null` because the current DB snapshot does not
persist Kakao category text; the UI must not depend on this optional field.

- [ ] **Step 4: Re-run the focused test**

Expected: ordering assertions pass.

---

### Task 4: Add member-visible results and owner-only final selection

**Files:**
- Create: `core/src/main/java/com/gm/core/domain/store/StoreSelectionService.java`
- Modify: `core/src/main/java/com/gm/core/domain/store/repository/StoreRepository.java`
- Modify: `core/src/main/java/com/gm/core/domain/vote/session/repository/VoteSessionRepository.java`
- Modify: `core/src/main/java/com/gm/core/domain/vote/session/service/VoteSessionService.java`
- Modify: `storage/db/src/main/java/com/gm/db/domain/store/repository/StoreRepositoryImpl.java`
- Modify: `storage/db/src/main/java/com/gm/db/domain/vote/session/entity/VoteSessionEntity.java`
- Modify: `storage/db/src/main/java/com/gm/db/domain/vote/session/repository/VoteSessionRepositoryImpl.java`
- Modify: `api/src/main/java/com/gm/api/controller/store/StoreController.java`
- Modify: `api/src/test/java/com/gm/api/controller/store/StoreControllerIntegrationTest.java`

**Interfaces:**
- Produces: `List<Store> findResults(UUID groupId, UUID userId, UUID voteSessionId)`
- Produces: `Store selectFinalRestaurant(UUID groupId, UUID userId, UUID voteSessionId, String externalPlaceId, LocalDateTime completedAt)`
- Produces: `Optional<VoteSession> complete(UUID voteSessionId, LocalDateTime completedAt)`
- Produces: `GET /api/groups/{groupId}/vote-sessions/{voteSessionId}/stores`
- Produces: `PUT /api/groups/{groupId}/vote-sessions/{voteSessionId}/stores/{externalPlaceId}/selection`

- [ ] **Step 1: Add failing behavior tests**

Cover these exact cases in `StoreControllerIntegrationTest`:

```java
// active member can list RESTAURANT_SELECTION results
// non-member receives the existing group access error
// mismatched groupId and voteSessionId is rejected
// owner selects a persisted restaurant and receives StoreResponse
// ordinary member cannot select
// missing externalPlaceId returns STORE-001
// invalid session state returns SESSION-003
// successful selection sets selected=true, status=COMPLETED, and closedAt
```

- [ ] **Step 2: Run and verify the expected failures**

Run the focused store integration test. Expected: missing endpoints and
completion behavior fail; existing search tests remain green.

- [ ] **Step 3: Implement core authorization and state rules**

`findResults` must:

```java
VoteSession session = voteSessionService.findVoteSession(voteSessionId);
if (!session.diningGroupId().equals(groupId)) throw SESSION_NOT_FOUND;
groupService.findGroupDetail(groupId, userId);
if (session.voteSessionStatus() != RESTAURANT_SELECTION
        && session.voteSessionStatus() != COMPLETED) throw SESSION-003;
return storeRepository.findAllByVoteSessionId(voteSessionId);
```

`selectFinalRestaurant` must lock the session, verify the same group, require
`GroupMemberRole.OWNER`, require `RESTAURANT_SELECTION`, select the persisted
store, and complete the session in one transaction.

- [ ] **Step 4: Persist completion time atomically**

Add entity behavior:

```java
public void complete(LocalDateTime completedAt) {
    updateStatus(VoteSessionStatus.COMPLETED);
    this.closedAt = completedAt;
}
```

Expose it through the repository and service so restaurant selection and
completion share the caller transaction.

- [ ] **Step 5: Add controller mappings and response conversion**

Return standard response envelopes and reuse `StoreResponse.from(Store)`.

- [ ] **Step 6: Verify focused and history tests**

Run:

```powershell
.\gradlew.bat :api:test --tests "com.gm.api.controller.store.StoreControllerIntegrationTest" --tests "com.gm.integration.history.PreviousHistoryIntegrationTest" --console=plain
```

Expected: both suites pass.

---

### Task 5: Close demo-reachable authorization gaps

**Files:**
- Modify: `core/src/test/java/com/gm/core/domain/vote/session/service/VoteSessionServiceTest.java`
- Modify: `core/src/main/java/com/gm/core/domain/vote/session/service/VoteSessionService.java`

**Interfaces:**
- Consumes: `GroupService.findGroupDetail(groupId, requestUserId)`
- Produces: owner-only cancel/delete behavior

- [ ] **Step 1: Add failing owner and non-owner tests**

Assert cancel and delete:

```java
// load the session, resolve its diningGroupId, and require OWNER
// reject an active MEMBER before repository mutation
```

- [ ] **Step 2: Verify tests fail because requestId is ignored**

Run the focused `VoteSessionServiceTest`.

- [ ] **Step 3: Add the minimal owner check**

Use `groupService.findGroupDetail(...).currentUserRole()` and the existing
`GroupErrorCode.NOT_GROUP_OWNER`; do not introduce a parallel permission model.

- [ ] **Step 4: Verify the focused tests pass**

Re-run the focused suite.

---

### Task 6: Backend regression and runtime readiness

**Files:**
- No production files unless a failing regression has a reproduced test

**Interfaces:**
- Produces: a passing backend build and a documented local runtime command

- [ ] **Step 1: Run affected module tests**

```powershell
.\gradlew.bat :core:test :storage:db:test :storage:redis:test :api:test --console=plain
```

- [ ] **Step 2: Run the full build**

```powershell
.\gradlew.bat build --console=plain
```

- [ ] **Step 3: Start local dependencies and API**

Use the repository's existing `application-example.yml` contract with MySQL,
Redis, Naver, OpenAI, and Kakao values supplied through environment variables.
Do not commit secrets.

- [ ] **Step 4: Smoke-check contract endpoints**

Verify `/v3/api-docs`, CORS preflight, authentication exchange, vote-state,
restaurant listing/selection, and history response against the running API.

