-- =====================================================================
-- 맛집 추천 서비스 - DB 스키마 v1
-- 기준: 요구사항 정의서(2026-07-15) + ERDCloud export 검증 반영
--
-- 검증 반영 사항
--   - vote_candidate.result_status : ENUM -> VARCHAR(30) NOT NULL DEFAULT 'PENDING'
--   - vote_session.closed_at / started_at : 진행 중 NULL 허용
--   - recommended_restaurant.provider 추가 (NAVER/KAKAO 구분)
--   - 모든 관계에 FK, M:N 매핑에 UNIQUE, 카운트/좌표에 CHECK, 조회용 INDEX 부여
--   - enum류는 MySQL ENUM으로 허용값을 제한하고 JPA @Enumerated(STRING)과 정합
--   - UUID PK/FK : Hibernate 기본 UUID 매핑과 일치하도록 BINARY(16) 사용
--   - user.text(=user.Field) : 용도 미확정 임시 컬럼, NULL 유지·로직 미연결
--   - dining_group.search_radius_m : 미터 단위 식당 검색 반경
--
-- 환경: MySQL 8.0.16+ / InnoDB / utf8mb4
-- 생성 순서: 부모 -> 자식 (FK 참조 순서 보장)
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
USE gm;

-- =====================================================================
-- 1. 마스터 테이블 (참조 대상, 최상위)
-- =====================================================================

CREATE TABLE `food_category` (
  `id`         BINARY(16)      NOT NULL COMMENT '식별자',
  `name`       VARCHAR(100)  NOT NULL COMMENT '카테고리명',
  `created_at` DATETIME(6)   NOT NULL COMMENT '생성일시',
  `updated_at` DATETIME(6)   NOT NULL COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_food_category_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='음식 카테고리 마스터';

CREATE TABLE `food_tag` (
  `id`         BINARY(16)      NOT NULL COMMENT '식별자',
  `name`       VARCHAR(100)  NOT NULL COMMENT '태그명',
  `created_at` DATETIME(6)   NOT NULL COMMENT '생성일시',
  `updated_at` DATETIME(6)   NOT NULL COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_food_tag_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='음식 속성 태그 마스터';

CREATE TABLE `allergen` (
  `id`          BINARY(16)       NOT NULL COMMENT '식별자',
  `name`        VARCHAR(100)   NOT NULL COMMENT '알레르기 성분명',
  `description` VARCHAR(1000)  NULL     COMMENT '알레르기 설명',
  `created_at`  DATETIME(6)    NOT NULL COMMENT '생성일시',
  `updated_at`  DATETIME(6)    NOT NULL COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_allergen_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='알레르기 성분 마스터';

CREATE TABLE `menu` (
  `id`          BINARY(16)       NOT NULL COMMENT '식별자',
  `category_id` BINARY(16)       NOT NULL COMMENT '음식 카테고리 식별자',
  `name`        VARCHAR(150)   NOT NULL COMMENT '메뉴명',
  `image_url`   VARCHAR(1000)  NULL     COMMENT '메뉴 이미지 URL',
  `created_at`  DATETIME(6)    NOT NULL COMMENT '생성일시',
  `updated_at`  DATETIME(6)    NOT NULL COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_menu_category_name` (`category_id`, `name`),
  KEY `IX_menu_category` (`category_id`),
  CONSTRAINT `FK_menu_category`
    FOREIGN KEY (`category_id`) REFERENCES `food_category`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='메뉴 마스터';

CREATE TABLE `menu_tag` (
  `id`         BINARY(16)     NOT NULL COMMENT '식별자',
  `menu_id`    BINARY(16)     NOT NULL COMMENT '메뉴 식별자',
  `tag_id`     BINARY(16)     NOT NULL COMMENT '음식 태그 식별자',
  `created_at` DATETIME(6)  NOT NULL COMMENT '생성일시',
  `updated_at` DATETIME(6)  NOT NULL COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_menu_tag` (`menu_id`, `tag_id`),
  KEY `IX_menu_tag_tag` (`tag_id`),
  CONSTRAINT `FK_menu_tag_menu`
    FOREIGN KEY (`menu_id`) REFERENCES `menu`(`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_menu_tag_tag`
    FOREIGN KEY (`tag_id`) REFERENCES `food_tag`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='메뉴-태그 매핑(M:N)';

CREATE TABLE `menu_allergen` (
  `id`          BINARY(16)     NOT NULL COMMENT '식별자',
  `menu_id`     BINARY(16)     NOT NULL COMMENT '메뉴 식별자',
  `allergen_id` BINARY(16)     NOT NULL COMMENT '알레르기 성분 식별자',
  `created_at`  DATETIME(6)  NOT NULL COMMENT '생성일시',
  `updated_at`  DATETIME(6)  NOT NULL COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_menu_allergen` (`menu_id`, `allergen_id`),
  KEY `IX_menu_allergen_allergen` (`allergen_id`),
  CONSTRAINT `FK_menu_allergen_menu`
    FOREIGN KEY (`menu_id`) REFERENCES `menu`(`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_menu_allergen_allergen`
    FOREIGN KEY (`allergen_id`) REFERENCES `allergen`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='메뉴-알레르기 매핑(사전 검증 데이터만)';


-- =====================================================================
-- 2. 계정 테이블
-- =====================================================================

CREATE TABLE `user` (
  `id`           BINARY(16)      NOT NULL COMMENT '식별자',
  `name`         VARCHAR(30)   NOT NULL COMMENT '이름',
  `nickname`     VARCHAR(100)  NOT NULL COMMENT '닉네임',
  `status`       ENUM('ONBOARDING','ACTIVE','WITHDRAWN') NOT NULL COMMENT '회원 상태',
  `provider`     ENUM('NAVER') NOT NULL COMMENT '소셜 제공자',
  `provider_id`  VARCHAR(255)  NOT NULL COMMENT '소셜 식별',
  `phone`        VARCHAR(20)   NOT NULL COMMENT '핸드폰 번호',
  `email`        VARCHAR(255)  NOT NULL COMMENT '이메일',
  `terms_agreed` BOOLEAN       NOT NULL COMMENT '정보 통합 동의',
  `custom_allergen_text` VARCHAR(500)  NULL     COMMENT '비표준 알레르기(자유텍스트, AI 하드 제외 지시)',
  `preference_text`      VARCHAR(500)  NULL     COMMENT '마스터 매핑 선호도 음식(자유텍스트)',
  `exclude_food_text`      VARCHAR(500)  NULL     COMMENT '마스터 매핑 싫어하는 음식(자유텍스트)',
  `created_at`   DATETIME(6)   NOT NULL COMMENT '생성일시',
  `updated_at`   DATETIME(6)   NOT NULL COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_user_provider` (`provider`, `provider_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원';

CREATE TABLE `user_allergen` (
  `id`          BINARY(16)     NOT NULL COMMENT '식별자',
  `user_id`     BINARY(16)     NOT NULL COMMENT '회원 식별자',
  `allergen_id` BINARY(16)     NOT NULL COMMENT '알레르기 성분 식별자',
  `created_at`  DATETIME(6)  NOT NULL COMMENT '생성일시',
  `updated_at`  DATETIME(6)  NOT NULL COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_user_allergen` (`user_id`, `allergen_id`),
  KEY `IX_user_allergen_allergen` (`allergen_id`),
  CONSTRAINT `FK_user_allergen_user`
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_user_allergen_allergen`
    FOREIGN KEY (`allergen_id`) REFERENCES `allergen`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원-알레르기 매핑(M:N)';

CREATE TABLE `user_menu_preference` (
  `id`         BINARY(16)     NOT NULL COMMENT '식별자',
  `user_id`    BINARY(16)     NOT NULL COMMENT '회원 식별자',
  `menu_id`    BINARY(16)     NOT NULL COMMENT '메뉴 식별자',
  `preference`     ENUM('LIKE','EXCLUDE')         NOT NULL COMMENT '선호 비선호 구분(LIKE/EXCLUDE)',
  `created_at` DATETIME(6)    NOT NULL COMMENT '생성일시',
  `updated_at` DATETIME(6)    NOT NULL COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_user_menu_preference` (`user_id`, `menu_id`),
  KEY `IX_ump_menu` (`menu_id`),
  CONSTRAINT `FK_ump_user`
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_ump_menu`
    FOREIGN KEY (`menu_id`) REFERENCES `menu`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원 메뉴 선호(가중치, +선호/−불호)';

CREATE TABLE `user_category_preference` (
  `id`          BINARY(16)     NOT NULL COMMENT '식별자',
  `user_id`     BINARY(16)     NOT NULL COMMENT '회원 식별자',
  `category_id` BINARY(16)     NOT NULL COMMENT '음식 카테고리 식별자',
  `preference`     ENUM('LIKE','DISLIKE')         NOT NULL COMMENT '선호 비선호 구분(LIKE/DISLIKE)',
  `created_at`  DATETIME(6)    NOT NULL COMMENT '생성일시',
  `updated_at`  DATETIME(6)    NOT NULL COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_user_category_preference` (`user_id`, `category_id`),
  KEY `IX_ucp_category` (`category_id`),
  CONSTRAINT `FK_ucp_user`
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_ucp_category`
    FOREIGN KEY (`category_id`) REFERENCES `food_category`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원 카테고리 선호(가중치, +선호/−불호)';

-- =====================================================================
-- 3. 그룹 테이블
-- =====================================================================

CREATE TABLE `dining_group` (
  `id`                  BINARY(16)       NOT NULL COMMENT '식별자',
  `owner_user_id`       BINARY(16)       NOT NULL COMMENT '그룹장 회원 식별자',
  `name`                VARCHAR(150)   NOT NULL COMMENT '그룹명',
  `location_address`    VARCHAR(500)   NOT NULL COMMENT '기준 주소',
  `latitude`            DOUBLE         NOT NULL COMMENT '기준 위도',
  `longitude`           DOUBLE         NOT NULL COMMENT '기준 경도',
  `search_radius_m`     INT            NOT NULL COMMENT '식당 검색 반경(m)',
  `recommendation_time` TIME           NOT NULL COMMENT '추천 시간',
  `max_member_count`    INT            NOT NULL COMMENT '최대 멤버 수',
  `created_at`          DATETIME(6)    NOT NULL COMMENT '생성일시',
  `updated_at`          DATETIME(6)    NOT NULL COMMENT '수정일시',
  PRIMARY KEY (`id`),
  KEY `IX_dining_group_owner` (`owner_user_id`),
  CONSTRAINT `FK_dining_group_owner`
    FOREIGN KEY (`owner_user_id`) REFERENCES `user`(`id`),
  CONSTRAINT `CK_dining_group_max_member` CHECK (`max_member_count` > 0),
  CONSTRAINT `CK_dining_group_lat` CHECK (`latitude`  BETWEEN -90  AND 90),
  CONSTRAINT `CK_dining_group_lng` CHECK (`longitude` BETWEEN -180 AND 180)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='그룹';

CREATE TABLE `group_member` (
  `id`         BINARY(16)     NOT NULL COMMENT '식별자',
  `dining_group_id` BINARY(16)     NOT NULL COMMENT '그룹 식별자',
  `user_id`    BINARY(16)     NOT NULL COMMENT '회원 식별자',
  `status`     ENUM('ACTIVE','LEFT','KICKED') NOT NULL COMMENT '멤버 상태',
  `joined_at`  DATETIME(6)  NOT NULL COMMENT '가입일시',
  `left_at`    DATETIME(6)  NULL     COMMENT '이탈일시',
  `role`       ENUM('OWNER','MEMBER') NOT NULL COMMENT '역할',
  `created_at` DATETIME(6)  NOT NULL COMMENT '생성일시',
  `updated_at` DATETIME(6)  NOT NULL COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_group_member` (`dining_group_id`, `user_id`),
  KEY `IX_group_member_dining_group` (`dining_group_id`, `status`),
  KEY `IX_group_member_user` (`user_id`),
  CONSTRAINT `FK_group_member_dining_group`
    FOREIGN KEY (`dining_group_id`) REFERENCES `dining_group`(`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_group_member_user`
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='그룹-회원 관계';


-- =====================================================================
-- 4. 세션 / 투표 테이블
-- =====================================================================

CREATE TABLE `vote_session` (
  `id`              BINARY(16)      NOT NULL COMMENT '투표 회차 식별자',
  `dining_group_id` BINARY(16)      NOT NULL COMMENT '그룹 식별자',
  `title`           VARCHAR(150)  NOT NULL COMMENT '투표 제목',
  `status`          ENUM('PREFERENCE_INPUT','MENU_RECOMMENDING','MENU_VOTING','MENU_SELECTION','RESTAURANT_SEARCHING','RESTAURANT_SELECTION','COMPLETED','CANCELLED','FAILED') NOT NULL COMMENT '투표 세션 상태',
  `like_keyword`    VARCHAR(255)  NULL     COMMENT '종합 선호 키워드',
  `dislike_keyword` VARCHAR(255)  NULL     COMMENT '종합 비선호 키워드',
  `started_at`      DATETIME(6)   NULL     COMMENT '투표 시작일시(시작 전 NULL)',
  `closed_at`       DATETIME(6)   NULL     COMMENT '투표 마감일시(진행 중 NULL)',
  `created_at`      DATETIME(6)   NOT NULL COMMENT '생성일시(캘린더 기준일)',
  `updated_at`      DATETIME(6)   NOT NULL COMMENT '수정일시',
  PRIMARY KEY (`id`),
  KEY `IX_vote_session_dining_group`   (`dining_group_id`, `status`),
  KEY `IX_vote_session_created` (`dining_group_id`, `created_at`),
  CONSTRAINT `FK_vote_session_dining_group`
    FOREIGN KEY (`dining_group_id`) REFERENCES `dining_group`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='한 끼 추천·투표 세션';

CREATE TABLE `vote_candidate` (
  `id`               BINARY(16)      NOT NULL COMMENT '투표 후보 식별자',
  `vote_session_id`  BINARY(16)      NOT NULL COMMENT '투표 회차 식별자',
  `menu_id`          BINARY(16)      NOT NULL COMMENT '메뉴 식별자',
  `display_order`    INT           NOT NULL COMMENT '후보 노출 순서',
  `selected`         BOOLEAN       NOT NULL DEFAULT FALSE COMMENT '최종 메뉴 선택 여부',
  `selected_session_id` BINARY(16) GENERATED ALWAYS AS
      (IF(`selected`, `vote_session_id`, NULL)) STORED COMMENT '세션당 단일 선택 제약용',
  `go_count`         INT           NULL     COMMENT '갈래 투표 수',
  `maybe_count`      INT           NULL     COMMENT '애매하긴해 투표 수',
  `no_count`         INT           NULL     COMMENT '말래 투표 수',
  `respondent_count` INT           NULL     COMMENT '응답자 수',
  `result_status`    ENUM('PENDING','CONFIRMED','KEPT','REJECTED','UNRESOLVED') NOT NULL DEFAULT 'PENDING' COMMENT '후보 결과 상태',
  `description`      VARCHAR(255)  NULL     COMMENT '추천 이유',
  `created_at`       DATETIME(6)   NOT NULL COMMENT '생성일시',
  `updated_at`       DATETIME(6)   NOT NULL COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_vote_candidate_session_menu` (`vote_session_id`, `menu_id`),
  UNIQUE KEY `UK_vote_candidate_session_order` (`vote_session_id`, `display_order`),
  UNIQUE KEY `UK_vote_candidate_one_selected` (`selected_session_id`),
  KEY `IX_vote_candidate_session` (`vote_session_id`),
  KEY `IX_vote_candidate_menu` (`menu_id`),
  CONSTRAINT `FK_vote_candidate_session`
    FOREIGN KEY (`vote_session_id`) REFERENCES `vote_session`(`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_vote_candidate_menu`
    FOREIGN KEY (`menu_id`) REFERENCES `menu`(`id`),
  CONSTRAINT `CK_vc_go`    CHECK (`go_count`         IS NULL OR `go_count`         >= 0),
  CONSTRAINT `CK_vc_maybe` CHECK (`maybe_count`      IS NULL OR `maybe_count`      >= 0),
  CONSTRAINT `CK_vc_no`    CHECK (`no_count`         IS NULL OR `no_count`         >= 0),
  CONSTRAINT `CK_vc_resp`  CHECK (`respondent_count` IS NULL OR `respondent_count` >= 0),
  CONSTRAINT `CK_vc_order` CHECK (`display_order` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='세션별 메뉴 후보';

CREATE TABLE `recommended_restaurant` (
  `id`                BINARY(16)       NOT NULL COMMENT '추천 식당 식별자',
  `vote_session_id`   BINARY(16)       NOT NULL COMMENT '투표 회차 식별자',
  `selected`          BOOLEAN        NOT NULL DEFAULT FALSE COMMENT '최종 식당 선택 여부',
  `name`              VARCHAR(150)   NOT NULL COMMENT '가게 이름',
  `url`               VARCHAR(255)   NOT NULL COMMENT '가게 링크',
  `address`           VARCHAR(255)   NOT NULL COMMENT '주소',
  `latitude`          DOUBLE         NOT NULL COMMENT '위도',
  `longitude`         DOUBLE         NOT NULL COMMENT '경도',
  `distance_m`        INT            NOT NULL COMMENT '식당까지 거리(m)',
  `external_place_id` VARCHAR(255)   NOT NULL COMMENT '외부 API 식당 ID',
  `provider`          ENUM('KAKAO') NOT NULL COMMENT '외부 장소 제공자',
  `created_at`        DATETIME(6)    NOT NULL COMMENT '생성일시',
  `updated_at`        DATETIME(6)    NOT NULL COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_recommended_restaurant_session_place` (`vote_session_id`, `provider`, `external_place_id`),
  KEY `IX_recommended_rest_session` (`vote_session_id`),
  CONSTRAINT `FK_recommended_restaurant_session`
    FOREIGN KEY (`vote_session_id`) REFERENCES `vote_session`(`id`) ON DELETE CASCADE,
  CONSTRAINT `CK_rr_distance` CHECK (`distance_m` >= 0),
  CONSTRAINT `CK_rr_lat` CHECK (`latitude`  BETWEEN -90  AND 90),
  CONSTRAINT `CK_rr_lng` CHECK (`longitude` BETWEEN -180 AND 180)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='세션별 식당 API 스냅샷';

-- ---------------------------------------------------------------------
-- 메시지 멱등 처리 (inbox)
-- ---------------------------------------------------------------------
-- 브로커가 같은 메시지를 재전달해도 한 번만 처리되도록, 처리 기록을
-- 비즈니스 트랜잭션과 같은 트랜잭션에 남긴다.
-- eventId는 발행 시점에 생성된 UUID 문자열이며 도메인 FK가 아니다.
CREATE TABLE `processed_events` (
  `event_id`     CHAR(36)    NOT NULL COMMENT '이벤트 식별자(EventEnvelope.eventId)',
  `processed_at` DATETIME(6) NOT NULL COMMENT '처리 완료 일시',
  PRIMARY KEY (`event_id`),
  KEY `IX_processed_events_processed_at` (`processed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='처리 완료한 이벤트 기록';
-- 정리: DELETE FROM processed_events WHERE processed_at < NOW() - INTERVAL 30 DAY;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 끝. 총 15개 테이블
-- 마스터 6 (food_category, food_tag, allergen, menu, menu_tag, menu_allergen)
-- 계정 4   (user, user_allergen, user_menu_preference, user_category_preference)
-- 그룹 2   (dining_group, group_member)
-- 세션 3   (vote_session, vote_candidate, recommended_restaurant)
-- =====================================================================

-- =====================================================================
-- 변경
-- 2026-07-26: 동시 선택 요청에도 세션별 최종 메뉴가 하나만 저장되도록 생성 컬럼과 UNIQUE 제약을 추가
-- 2026-07-26: 메시지 멱등 처리를 위한 processed_events 테이블 추가
-- 2026-07-23: user_category_preference 테이블 preference enum 값 exclude -> dislike 변경
-- 2026-07-23: User 테이블에 exclude_food_text 추가
-- 2026-07-21: user_menu_preference, user_category_preference 테이블 컬럼 weight -> preference(ENUM타입) 변경
-- 2026-07-20: MySQL 예약어 충돌을 피하기 위해 `group`/`group_id`를 `dining_group`/`dining_group_id`로 변경.
-- 2026-07-20: Hibernate UUID 기본 매핑과 저장·인덱스 효율을 맞추기 위해 UUID PK/FK를 CHAR(36)에서 BINARY(16)으로 변경.
-- 2026-07-20: 좌표를 부동소수점 타입으로 저장하기 위해 latitude/longitude를 DECIMAL(10,7)에서 DOUBLE로 변경.
-- 2026-07-20: 메뉴·카테고리 호불호를 단일 점수로 반영하기 위해 user_menu_exclusion/user_category_exclusion을 제거하고, user_food_preference를 user_category_preference로 변경한 뒤 두 선호 테이블에 DOUBLE weight(+선호/−불호)를 추가.
-- 2026-07-20: 상태·역할·제공자의 허용값을 DB에서 강제하고 JPA EnumType.STRING과 일치시키기 위해 관련 VARCHAR 컬럼 7개를 MySQL ENUM으로 변경.
-- 2026-07-20: user 테이블 text -> custom_allergen_text preference_text VARCHAR(500)  NULL로 변경
-- =====================================================================
