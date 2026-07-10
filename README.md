# 갈래말래 백엔드

## 개발 환경

| 구분 | 구성 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Build | Gradle 9.5.1 Wrapper, Groovy DSL |
| Web | Spring MVC, Validation, Security, OAuth2 Client |
| Database | Spring Data JPA, MySQL |
| Cache | Spring Data Redis |
| External API | Spring `RestClient` |
| API 문서 | springdoc OpenAPI |

로컬에 별도 Gradle을 설치하지 않고 저장소에 포함된 Gradle Wrapper를 사용한다.

## 멀티모듈 구조

```text
gm-be/
├── api/                 # 실행 가능한 Spring Boot 애플리케이션
├── core/                # 도메인 모델, 비즈니스 규칙, 포트 인터페이스
├── client/              # 외부 API 호출 및 외부 응답 변환
└── storage/
    ├── db/              # JPA/MySQL 영속성 구현
    └── redis/           # Redis 영속성 및 캐시 구현
```

| 모듈 | 책임 | 의존 가능한 프로젝트 모듈 |
| --- | --- | --- |
| `api` | 웹 진입점, 요청·응답 처리, 보안 및 애플리케이션 조립 | `core`, `client`, `storage:db`, `storage:redis` |
| `core` | 도메인 모델, 비즈니스 규칙, 저장소·외부 연동 포트 | 없음 |
| `client` | 외부 API 호출과 외부 모델의 도메인 변환 | `core` |
| `storage:db` | 도메인 저장 포트의 JPA/MySQL 구현 | `core` |
| `storage:redis` | 도메인 저장 포트의 Redis 구현 | `core` |

의존 방향은 다음과 같이 유지한다.

```text
api ───────────────→ core
 ├──→ client ──────→ core
 ├──→ storage:db ──→ core
 └──→ storage:redis → core
```

- `core`는 다른 프로젝트 모듈에 의존하지 않는다.
- `client`와 `storage` 모듈은 서로 직접 의존하지 않는다.
- 외부 API 모델, JPA Entity, Redis 전용 모델을 `core`의 도메인 모델로 사용하지 않는다.
- `api`는 각 모듈의 구현을 조립하고 외부 요청·응답을 변환하는 역할에 집중한다.


## GitHub Issue 컨벤션

모든 작업은 GitHub Issue에서 시작한다. 하나의 Issue는 하나의 명확한 목적과 완료 조건을 가진다.

### Issue 제목

```text
[<type>] <작업 요약>
```

예시:

```text
[feat] 그룹 생성 API 추가
[fix] 중복 투표 저장 문제 수정
[refactor] 투표 집계 로직 분리
[docs] API 패키지 규칙 정리
```

### Issue 본문

```markdown
## 목적
- 이 작업이 필요한 이유

## 작업 범위
- 구현하거나 수정할 내용

## 완료 조건
- [ ] 확인 가능한 완료 기준

## 참고
- 관련 문서, API, 논의 내용
```

버그 Issue에는 재현 방법, 기대 동작, 실제 동작을 추가한다.

## 브랜치 컨벤션

브랜치는 Issue 번호를 포함하여 다음 형식으로 작성한다.

```text
<type>/issue-<issue-number>-<short-description>
```

예시:

```text
feature/issue-12-group-create-api
fix/issue-23-duplicate-vote
refactor/issue-31-vote-aggregation
docs/issue-42-package-convention
```

| 타입 | 용도 |
| --- | --- |
| `feature` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 구조 개선 |
| `docs` | 문서 변경 |
| `test` | 테스트 변경 |
| `chore` | 설정 및 기타 작업 |

- 브랜치는 `main`에서 생성한다.
- 이름은 영문 소문자와 숫자, 하이픈만 사용한다.
- `main`에 직접 push하지 않는다.
- 하나의 브랜치는 하나의 Issue만 처리한다.

## 커밋 컨벤션

Conventional Commit 형식을 사용한다.

```text
<type>: <summary>
```

예시:

```text
feat: add group create API
fix: prevent duplicate vote
refactor: extract vote aggregation service
docs: add package convention
```

| 타입 | 용도 |
| --- | --- |
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 구조 개선 |
| `docs` | 문서 변경 |
| `test` | 테스트 추가·수정 |
| `chore` | 설정 및 기타 작업 |
| `build` | 빌드 시스템이나 의존성 변경 |
| `ci` | CI 설정 변경 |
| `perf` | 성능 개선 |
| `style` | 동작 변경 없는 포맷 변경 |


## Pull Request 컨벤션

PR 제목은 커밋과 같은 형식을 사용한다.

```text
<type>: <summary>
```

PR 본문은 다음 형식을 사용한다.

```markdown
## 관련 Issue
- Closes #12

## 작업 내용
- 변경 사항과 변경 이유

## 확인 사항
- 확인한 내용

## 참고
- 리뷰 시 주의할 점이나 후속 작업
```

- Issue를 완료하는 PR은 `Closes #<issue-number>`로 연결한다.
- Issue의 일부만 처리하는 PR은 `Refs #<issue-number>`를 사용한다.
- 하나의 PR은 하나의 Issue와 하나의 목적을 기준으로 작게 유지한다.
- 리뷰가 필요한 상태가 되기 전에는 Draft PR을 사용한다.
- 승인된 PR은 **Squash and merge**로 병합하고 원격 작업 브랜치를 삭제한다.
- Squash commit 제목은 PR 제목의 Conventional Commit 형식을 유지한다.

## 작업 흐름

```text
1. Issue를 생성하고 목적과 완료 조건을 정한다.
2. main에서 Issue 번호가 포함된 브랜치를 만든다.
3. 목적별로 커밋한다.
4. PR을 만들고 Closes 또는 Refs로 Issue를 연결한다.
5. 리뷰를 반영한 뒤 Squash and merge한다.
6. Issue 종료와 원격 브랜치 삭제를 확인한다.
```
