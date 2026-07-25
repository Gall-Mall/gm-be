package com.gm.api.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.util.ReflectionTestUtils;

import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserResult;
import com.gm.core.domain.user.model.UserStatus;
import com.gm.core.domain.user.service.UserService;
import com.gm.db.domain.menu.allergen.entity.AllergenEntity;
import com.gm.db.domain.menu.allergen.repository.AllergenJpaRepository;
import com.gm.db.domain.menu.category.entity.FoodCategoryEntity;
import com.gm.db.domain.menu.category.repository.FoodCategoryJpaRepository;
import com.gm.db.domain.menu.menu.entity.MenuEntity;
import com.gm.db.domain.menu.menu.repository.MenuJpaRepository;
import com.gm.db.domain.user.entity.UserEntity;
import com.gm.db.domain.user.repository.UserJpaRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-setting;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class UserSettingIntegrationTest {

    private static final String ONBOARDING_URI = "/api/users/me/onboarding";
    private static final String SETTINGS_URI = "/api/users/me/food-settings";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private AllergenJpaRepository allergenJpaRepository;

    @Autowired
    private FoodCategoryJpaRepository foodCategoryJpaRepository;

    @Autowired
    private MenuJpaRepository menuJpaRepository;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        String uniqueValue = UUID.randomUUID().toString();
        UserResult userResult = userService.findOrCreateWithId(
                "온보딩 사용자",
                Provider.NAVER,
                "provider-" + uniqueValue,
                "010-0000-0000",
                uniqueValue + "@example.com"
        );
        userId = userResult.userId();
        user = userResult.user();
    }

    @Test
    @DisplayName("약관에 동의하고 온보딩을 제출하면 회원을 활성화하고 설정을 저장한다")
    void submitOnboarding_savesSettingAndActivatesUser() throws Exception {
        mockMvc.perform(post(ONBOARDING_URI)
                        .with(authAs(userId, user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onboardingRequest(true, "새우", "매운 음식", "고수")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.onboardingCompleted").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        UserEntity savedUser = findUser();
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedUser.getTermsAgreed()).isTrue();
        assertThat(savedUser.getCustomAllergenText()).isEqualTo("새우");
        assertThat(savedUser.getPreferenceText()).isEqualTo("매운 음식");
        assertThat(savedUser.getExcludeFoodText()).isEqualTo("고수");
    }

    @Test
    @DisplayName("온보딩 자유 입력값은 null로 제출할 수 있다")
    void submitOnboarding_allowsNullableText() throws Exception {
        mockMvc.perform(post(ONBOARDING_URI)
                        .with(authAs(userId, user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onboardingRequest(true, null, null, null)))
                .andExpect(status().isOk());

        UserEntity savedUser = findUser();
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedUser.getCustomAllergenText()).isNull();
        assertThat(savedUser.getPreferenceText()).isNull();
        assertThat(savedUser.getExcludeFoodText()).isNull();
    }

    @Test
    @DisplayName("약관에 동의하지 않으면 온보딩을 완료하지 않는다")
    void submitOnboarding_withoutTermsAgreement_returnsBadRequest() throws Exception {
        mockMvc.perform(post(ONBOARDING_URI)
                        .with(authAs(userId, user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onboardingRequest(false, null, null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER-007"));

        UserEntity savedUser = findUser();
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ONBOARDING);
        assertThat(savedUser.getTermsAgreed()).isFalse();
    }

    @Test
    @DisplayName("이미 활성화된 회원이 온보딩을 다시 제출하면 거부한다")
    void submitOnboarding_whenAlreadyCompleted_returnsConflict() throws Exception {
        mockMvc.perform(post(ONBOARDING_URI)
                        .with(authAs(userId, user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onboardingRequest(true, null, null, null)))
                .andExpect(status().isOk());

        mockMvc.perform(post(ONBOARDING_URI)
                        .with(authAs(userId, user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onboardingRequest(true, null, null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER-002"));
    }

    @Test
    @DisplayName("같은 메뉴가 선호와 제외에 포함되면 온보딩 전체를 롤백한다")
    void submitOnboarding_withMenuConflict_rollsBack() throws Exception {
        UUID duplicatedMenuId = UUID.randomUUID();
        String requestBody = """
                {
                  "termsAgreed": true,
                  "userSetting": {
                    "allergenIds": [],
                    "preferredMenuIds": ["%s"],
                    "excludedMenuIds": ["%s"],
                    "preferredCategoryIds": [],
                    "excludedCategoryIds": [],
                    "allergenText": null,
                    "preferredText": null,
                    "excludedText": null
                  }
                }
                """.formatted(duplicatedMenuId, duplicatedMenuId);

        mockMvc.perform(post(ONBOARDING_URI)
                        .with(authAs(userId, user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER-004"));

        UserEntity savedUser = findUser();
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ONBOARDING);
        assertThat(savedUser.getTermsAgreed()).isFalse();
    }

    @Test
    @DisplayName("저장된 회원 설정을 조회한다")
    void getUserSetting_returnsStoredSetting() throws Exception {
        completeOnboarding("갑각류", "한식", "향신료");

        mockMvc.perform(get(SETTINGS_URI)
                        .with(authAs(userId, user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.allergenIds").isEmpty())
                .andExpect(jsonPath("$.data.preferredMenuIds").isEmpty())
                .andExpect(jsonPath("$.data.excludedMenuIds").isEmpty())
                .andExpect(jsonPath("$.data.preferredCategoryIds").isEmpty())
                .andExpect(jsonPath("$.data.excludedCategoryIds").isEmpty())
                .andExpect(jsonPath("$.data.allergenText").value("갑각류"))
                .andExpect(jsonPath("$.data.preferredText").value("한식"))
                .andExpect(jsonPath("$.data.excludedText").value("향신료"));
    }

    @Test
    @DisplayName("회원 설정 변경은 ID 목록과 자유 입력값 전체를 새로운 값으로 교체한다")
    void changeUserSetting_replacesSetting() throws Exception {
        SettingReferences references = createSettingReferences();
        completeOnboarding(userSettingRequest(
                new UUID[]{references.allergen1()},
                new UUID[]{references.menu1()},
                new UUID[]{references.menu2()},
                new UUID[]{references.category1()},
                new UUID[]{references.category2()},
                "기존 알레르기",
                "기존 선호",
                "기존 제외"
        ));

        mockMvc.perform(put(SETTINGS_URI)
                        .with(authAs(userId, user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userSettingRequest(
                                new UUID[]{references.allergen2()},
                                new UUID[]{references.menu3()},
                                new UUID[]{references.menu4()},
                                new UUID[]{references.category3()},
                                new UUID[]{references.category4()},
                                "새 알레르기",
                                "새 선호",
                                "새 제외"
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.allergenIds", containsInAnyOrder(
                        references.allergen2().toString()
                )))
                .andExpect(jsonPath("$.data.preferredMenuIds", containsInAnyOrder(
                        references.menu3().toString()
                )))
                .andExpect(jsonPath("$.data.excludedMenuIds", containsInAnyOrder(
                        references.menu4().toString()
                )))
                .andExpect(jsonPath("$.data.preferredCategoryIds", containsInAnyOrder(
                        references.category3().toString()
                )))
                .andExpect(jsonPath("$.data.excludedCategoryIds", containsInAnyOrder(
                        references.category4().toString()
                )))
                .andExpect(jsonPath("$.data.allergenText").value("새 알레르기"))
                .andExpect(jsonPath("$.data.preferredText").value("새 선호"))
                .andExpect(jsonPath("$.data.excludedText").value("새 제외"));

        mockMvc.perform(get(SETTINGS_URI)
                        .with(authAs(userId, user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allergenIds", containsInAnyOrder(
                        references.allergen2().toString()
                )))
                .andExpect(jsonPath("$.data.preferredMenuIds", containsInAnyOrder(
                        references.menu3().toString()
                )))
                .andExpect(jsonPath("$.data.excludedMenuIds", containsInAnyOrder(
                        references.menu4().toString()
                )))
                .andExpect(jsonPath("$.data.preferredCategoryIds", containsInAnyOrder(
                        references.category3().toString()
                )))
                .andExpect(jsonPath("$.data.excludedCategoryIds", containsInAnyOrder(
                        references.category4().toString()
                )))
                .andExpect(jsonPath("$.data.allergenText").value("새 알레르기"))
                .andExpect(jsonPath("$.data.preferredText").value("새 선호"))
                .andExpect(jsonPath("$.data.excludedText").value("새 제외"));
    }

    @Test
    @DisplayName("설정 변경에서 선호 메뉴와 제외 메뉴가 겹치면 기존 설정을 유지한다")
    void changeUserSetting_withMenuConflict_keepsExistingSetting() throws Exception {
        SettingReferences references = createSettingReferences();
        completeOnboarding(userSettingRequest(
                new UUID[]{references.allergen1()},
                new UUID[]{references.menu1()},
                new UUID[]{references.menu2()},
                new UUID[]{references.category1()},
                new UUID[]{references.category2()},
                "기존 알레르기",
                "기존 선호",
                "기존 제외"
        ));

        mockMvc.perform(put(SETTINGS_URI)
                        .with(authAs(userId, user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userSettingRequest(
                                new UUID[]{references.allergen2()},
                                new UUID[]{references.menu3()},
                                new UUID[]{references.menu3()},
                                new UUID[]{references.category3()},
                                new UUID[]{references.category4()},
                                "변경 알레르기",
                                "변경 선호",
                                "변경 제외"
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER-004"));

        assertSettingEquals(
                references.allergen1(),
                references.menu1(),
                references.menu2(),
                references.category1(),
                references.category2(),
                "기존 알레르기",
                "기존 선호",
                "기존 제외"
        );
    }

    @Test
    @DisplayName("설정 변경에서 선호 카테고리와 제외 카테고리가 겹치면 기존 설정을 유지한다")
    void changeUserSetting_withCategoryConflict_keepsExistingSetting() throws Exception {
        SettingReferences references = createSettingReferences();
        completeOnboarding(userSettingRequest(
                new UUID[]{references.allergen1()},
                new UUID[]{references.menu1()},
                new UUID[]{references.menu2()},
                new UUID[]{references.category1()},
                new UUID[]{references.category2()},
                "기존 알레르기",
                "기존 선호",
                "기존 제외"
        ));

        mockMvc.perform(put(SETTINGS_URI)
                        .with(authAs(userId, user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userSettingRequest(
                                new UUID[]{references.allergen2()},
                                new UUID[]{references.menu3()},
                                new UUID[]{references.menu4()},
                                new UUID[]{references.category3()},
                                new UUID[]{references.category3()},
                                "변경 알레르기",
                                "변경 선호",
                                "변경 제외"
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER-003"));

        assertSettingEquals(
                references.allergen1(),
                references.menu1(),
                references.menu2(),
                references.category1(),
                references.category2(),
                "기존 알레르기",
                "기존 선호",
                "기존 제외"
        );
    }

    @Test
    @DisplayName("존재하지 않는 알레르기가 포함되면 설정 변경 전체를 롤백한다")
    void changeUserSetting_whenAllergenDoesNotExist_rollsBackEntireChange() throws Exception {
        SettingReferences references = createSettingReferences();
        completeOnboarding(userSettingRequest(
                new UUID[]{references.allergen1()},
                new UUID[]{references.menu1()},
                new UUID[]{references.menu2()},
                new UUID[]{references.category1()},
                new UUID[]{references.category2()},
                "기존 알레르기",
                "기존 선호",
                "기존 제외"
        ));

        mockMvc.perform(put(SETTINGS_URI)
                        .with(authAs(userId, user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userSettingRequest(
                                new UUID[]{UUID.randomUUID()},
                                new UUID[]{references.menu3()},
                                new UUID[]{references.menu4()},
                                new UUID[]{references.category3()},
                                new UUID[]{references.category4()},
                                "변경 알레르기",
                                "변경 선호",
                                "변경 제외"
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER-008"));

        assertSettingEquals(
                references.allergen1(),
                references.menu1(),
                references.menu2(),
                references.category1(),
                references.category2(),
                "기존 알레르기",
                "기존 선호",
                "기존 제외"
        );
    }

    @Test
    @DisplayName("존재하지 않는 메뉴가 포함되면 설정 변경 전체를 롤백한다")
    void changeUserSetting_whenMenuDoesNotExist_rollsBackEntireChange() throws Exception {
        SettingReferences references = createSettingReferences();
        completeOnboarding(userSettingRequest(
                new UUID[]{references.allergen1()},
                new UUID[]{references.menu1()},
                new UUID[]{references.menu2()},
                new UUID[]{references.category1()},
                new UUID[]{references.category2()},
                "기존 알레르기",
                "기존 선호",
                "기존 제외"
        ));

        mockMvc.perform(put(SETTINGS_URI)
                        .with(authAs(userId, user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userSettingRequest(
                                new UUID[]{references.allergen2()},
                                new UUID[]{UUID.randomUUID()},
                                new UUID[]{references.menu4()},
                                new UUID[]{references.category3()},
                                new UUID[]{references.category4()},
                                "변경 알레르기",
                                "변경 선호",
                                "변경 제외"
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER-009"));

        assertSettingEquals(
                references.allergen1(),
                references.menu1(),
                references.menu2(),
                references.category1(),
                references.category2(),
                "기존 알레르기",
                "기존 선호",
                "기존 제외"
        );
    }

    @Test
    @DisplayName("설정 저장 도중 존재하지 않는 카테고리가 발견되면 변경 전체를 롤백한다")
    void changeUserSetting_whenCategoryDoesNotExist_rollsBackEntireChange() throws Exception {
        SettingReferences references = createSettingReferences();
        completeOnboarding(userSettingRequest(
                new UUID[]{references.allergen1()},
                new UUID[]{references.menu1()},
                new UUID[]{references.menu2()},
                new UUID[]{references.category1()},
                new UUID[]{references.category2()},
                "기존 알레르기",
                "기존 선호",
                "기존 제외"
        ));

        mockMvc.perform(put(SETTINGS_URI)
                        .with(authAs(userId, user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userSettingRequest(
                                new UUID[]{references.allergen2()},
                                new UUID[]{references.menu3()},
                                new UUID[]{references.menu4()},
                                new UUID[]{UUID.randomUUID()},
                                new UUID[]{},
                                "변경 알레르기",
                                "변경 선호",
                                "변경 제외"
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER-005"));

        assertSettingEquals(
                references.allergen1(),
                references.menu1(),
                references.menu2(),
                references.category1(),
                references.category2(),
                "기존 알레르기",
                "기존 선호",
                "기존 제외"
        );
    }

    @Test
    @DisplayName("존재하지 않는 회원의 설정 변경 요청은 404를 반환한다")
    void changeUserSetting_whenUserDoesNotExist_returnsNotFound() throws Exception {
        UUID unknownUserId = UUID.randomUUID();

        mockMvc.perform(put(SETTINGS_URI)
                        .with(authAs(unknownUserId, user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userSettingRequest("알레르기", "선호", "제외")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER-001"));
    }

    @Test
    @DisplayName("설정 변경에서 자유 입력값이 null이면 기존 USER-006 오류를 반환한다")
    void changeUserSetting_withNullableText_returnsBadRequest() throws Exception {
        completeOnboarding(null, null, null);

        mockMvc.perform(put(SETTINGS_URI)
                        .with(authAs(userId, user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userSettingRequest(null, null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER-006"));
    }

    @Test
    @DisplayName("중첩 설정의 필수 목록이 누락되면 400을 반환한다")
    void submitOnboarding_withoutRequiredSettingList_returnsBadRequest() throws Exception {
        String requestBody = """
                {
                  "termsAgreed": true,
                  "userSetting": {
                    "allergenIds": null,
                    "preferredMenuIds": [],
                    "excludedMenuIds": [],
                    "preferredCategoryIds": [],
                    "excludedCategoryIds": []
                  }
                }
                """;

        mockMvc.perform(post(ONBOARDING_URI)
                        .with(authAs(userId, user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-002"));

        assertThat(findUser().getStatus()).isEqualTo(UserStatus.ONBOARDING);
    }

    @Test
    @DisplayName("500자를 초과하는 자유 입력값은 400을 반환한다")
    void changeUserSetting_withTooLongText_returnsBadRequest() throws Exception {
        completeOnboarding(null, null, null);

        mockMvc.perform(put(SETTINGS_URI)
                        .with(authAs(userId, user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userSettingRequest("a".repeat(501), "선호", "제외")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-002"));
    }

    @Test
    @DisplayName("인증 없이 온보딩을 제출하면 401을 반환한다")
    void submitOnboarding_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(ONBOARDING_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onboardingRequest(true, null, null, null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증 없이 회원 설정을 조회하면 401을 반환한다")
    void getUserSetting_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(SETTINGS_URI))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증 없이 회원 설정을 변경하면 401을 반환한다")
    void changeUserSetting_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(put(SETTINGS_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userSettingRequest("알레르기", "선호", "제외")))
                .andExpect(status().isUnauthorized());
    }

    private void completeOnboarding(
            String allergenText,
            String preferredText,
            String excludedText
    ) throws Exception {
        completeOnboarding(userSettingRequest(allergenText, preferredText, excludedText));
    }

    private void completeOnboarding(String userSettingRequest) throws Exception {
        mockMvc.perform(post(ONBOARDING_URI)
                        .with(authAs(userId, user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onboardingRequest(true, userSettingRequest)))
                .andExpect(status().isOk());
    }

    private void assertSettingEquals(
            UUID allergenId,
            UUID preferredMenuId,
            UUID excludedMenuId,
            UUID preferredCategoryId,
            UUID excludedCategoryId,
            String allergenText,
            String preferredText,
            String excludedText
    ) throws Exception {
        mockMvc.perform(get(SETTINGS_URI)
                        .with(authAs(userId, user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allergenIds", containsInAnyOrder(
                        allergenId.toString()
                )))
                .andExpect(jsonPath("$.data.preferredMenuIds", containsInAnyOrder(
                        preferredMenuId.toString()
                )))
                .andExpect(jsonPath("$.data.excludedMenuIds", containsInAnyOrder(
                        excludedMenuId.toString()
                )))
                .andExpect(jsonPath("$.data.preferredCategoryIds", containsInAnyOrder(
                        preferredCategoryId.toString()
                )))
                .andExpect(jsonPath("$.data.excludedCategoryIds", containsInAnyOrder(
                        excludedCategoryId.toString()
                )))
                .andExpect(jsonPath("$.data.allergenText").value(allergenText))
                .andExpect(jsonPath("$.data.preferredText").value(preferredText))
                .andExpect(jsonPath("$.data.excludedText").value(excludedText));
    }

    private SettingReferences createSettingReferences() {
        UUID category1 = saveCategory("카테고리 1");
        UUID category2 = saveCategory("카테고리 2");
        UUID category3 = saveCategory("카테고리 3");
        UUID category4 = saveCategory("카테고리 4");

        return new SettingReferences(
                saveAllergen("알레르기 1"),
                saveAllergen("알레르기 2"),
                saveMenu(category1, "메뉴 1"),
                saveMenu(category2, "메뉴 2"),
                saveMenu(category3, "메뉴 3"),
                saveMenu(category4, "메뉴 4"),
                category1,
                category2,
                category3,
                category4
        );
    }

    private UUID saveAllergen(String name) {
        AllergenEntity entity = new AllergenEntity();
        ReflectionTestUtils.setField(entity, "name", name + "-" + UUID.randomUUID());
        return allergenJpaRepository.saveAndFlush(entity).getId();
    }

    private UUID saveCategory(String name) {
        FoodCategoryEntity entity = new FoodCategoryEntity();
        ReflectionTestUtils.setField(entity, "name", name + "-" + UUID.randomUUID());
        return foodCategoryJpaRepository.saveAndFlush(entity).getId();
    }

    private UUID saveMenu(UUID categoryId, String name) {
        MenuEntity entity = new MenuEntity();
        ReflectionTestUtils.setField(entity, "categoryId", categoryId);
        ReflectionTestUtils.setField(entity, "name", name + "-" + UUID.randomUUID());
        return menuJpaRepository.saveAndFlush(entity).getId();
    }

    private UserEntity findUser() {
        return userJpaRepository.findById(userId).orElseThrow();
    }

    private static String onboardingRequest(
            boolean termsAgreed,
            String allergenText,
            String preferredText,
            String excludedText
    ) {
        return """
                {
                  "termsAgreed": %s,
                  "userSetting": %s
                }
                """.formatted(
                termsAgreed,
                userSettingRequest(allergenText, preferredText, excludedText)
        );
    }

    private static String onboardingRequest(boolean termsAgreed, String userSettingRequest) {
        return """
                {
                  "termsAgreed": %s,
                  "userSetting": %s
                }
                """.formatted(termsAgreed, userSettingRequest);
    }

    private static String userSettingRequest(
            String allergenText,
            String preferredText,
            String excludedText
    ) {
        return userSettingRequest(
                new UUID[]{},
                new UUID[]{},
                new UUID[]{},
                new UUID[]{},
                new UUID[]{},
                allergenText,
                preferredText,
                excludedText
        );
    }

    private static String userSettingRequest(
            UUID[] allergenIds,
            UUID[] preferredMenuIds,
            UUID[] excludedMenuIds,
            UUID[] preferredCategoryIds,
            UUID[] excludedCategoryIds,
            String allergenText,
            String preferredText,
            String excludedText
    ) {
        return """
                {
                  "allergenIds": %s,
                  "preferredMenuIds": %s,
                  "excludedMenuIds": %s,
                  "preferredCategoryIds": %s,
                  "excludedCategoryIds": %s,
                  "allergenText": %s,
                  "preferredText": %s,
                  "excludedText": %s
                }
                """.formatted(
                uuidArray(allergenIds),
                uuidArray(preferredMenuIds),
                uuidArray(excludedMenuIds),
                uuidArray(preferredCategoryIds),
                uuidArray(excludedCategoryIds),
                jsonString(allergenText),
                jsonString(preferredText),
                jsonString(excludedText)
        );
    }

    private static String uuidArray(UUID[] ids) {
        return "[" + String.join(
                ",",
                Arrays.stream(ids)
                        .map(UUID::toString)
                        .map(UserSettingIntegrationTest::jsonString)
                        .toList()
        ) + "]";
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value + "\"";
    }

    private static RequestPostProcessor authAs(UUID authenticatedUserId, User authenticatedUser) {
        CustomUserPrincipal principal =
                new CustomUserPrincipal(authenticatedUserId, authenticatedUser);
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );
        return authentication(token);
    }

    private record SettingReferences(
            UUID allergen1,
            UUID allergen2,
            UUID menu1,
            UUID menu2,
            UUID menu3,
            UUID menu4,
            UUID category1,
            UUID category2,
            UUID category3,
            UUID category4
    ) {
    }
}
