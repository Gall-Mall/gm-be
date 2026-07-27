# Session Keyword Recommendation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make vote-session preference keywords affect OpenAI menu curation.

**Architecture:** Prefix the stored session keywords as high-priority signals and prepend them to the existing curation signal lists. Teach the system prompt to prioritize those marked signals while preserving deterministic filtering and scoring.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, Mockito

## Global Constraints

- Standard allergen filtering remains the highest-priority hard exclusion.
- AI may select only from the deterministic candidate pool.
- Existing onboarding preference signals remain available as secondary context.

---

### Task 1: Session keyword propagation

**Files:**
- Modify: `core/src/main/java/com/gm/core/domain/recommendation/service/MenuRecommendationService.java`
- Test: `core/src/test/java/com/gm/core/domain/recommendation/service/MenuRecommendationServiceTest.java`

- [ ] Add a failing test that captures `매콤한, 국물` and `면 요리` from the vote session.
- [ ] Prepend marked session keywords to the existing preference and exclusion signal lists.
- [ ] Run the recommendation service tests.

### Task 2: High-priority OpenAI prompt

**Files:**
- Modify: `client/src/main/java/com/gm/client/openai/adapter/OpenAiMenuCurationAdapter.java`
- Test: `client/src/test/java/com/gm/client/openai/adapter/OpenAiMenuCurationAdapterTest.java`

- [ ] Add a failing test asserting both session keyword sections appear in the user prompt.
- [ ] Add the prompt sections and explicit priority rule.
- [ ] Run client and core tests.

### Task 3: Verification

- [ ] Run complete core and client tests.
- [ ] Assemble the API without tests.
