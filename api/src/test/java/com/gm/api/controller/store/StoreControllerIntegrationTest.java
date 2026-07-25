package com.gm.api.controller.store;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalTime;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.NewGroup;
import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserStatus;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.session.service.VoteSessionService;
import com.gm.db.domain.store.repository.StoreJpaRepository;
import com.gm.db.domain.vote.session.entity.VoteSessionEntity;
import com.gm.db.domain.vote.session.repository.VoteSessionJpaRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:store-controller;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Import(StoreControllerIntegrationTest.KakaoMockConfiguration.class)
@Transactional
class StoreControllerIntegrationTest {

    private static final String SEARCH_URI = "/api/stores/search";
    private static final String VALID_REQUEST = """
            {
              "voteSessionId": "%s",
              "keyword": "삼겹살",
              "longitude": 127.0,
              "latitude": 37.5,
              "radiusM": %d
            }
            """;

    private static final String KAKAO_RESULTS = """
            {
              "documents": [
                {
                  "id": "far",
                  "place_name": "먼 식당",
                  "category_name": "음식점 > 한식",
                  "road_address_name": "서울시 먼길 1",
                  "place_url": "https://place.map.kakao.com/far",
                  "x": "127.003",
                  "y": "37.503",
                  "distance": "300"
                },
                {
                  "id": "near-a",
                  "place_name": "가까운 식당 A",
                  "category_name": "음식점 > 한식",
                  "road_address_name": "서울시 가까운길 1",
                  "place_url": "https://place.map.kakao.com/near-a",
                  "x": "127.001",
                  "y": "37.501",
                  "distance": "100"
                },
                {
                  "id": "near-b",
                  "place_name": "가까운 식당 B",
                  "category_name": "음식점 > 한식",
                  "road_address_name": "서울시 가까운길 2",
                  "place_url": "https://place.map.kakao.com/near-b",
                  "x": "127.001",
                  "y": "37.501",
                  "distance": "100"
                },
                {
                  "id": "middle",
                  "place_name": "중간 식당",
                  "category_name": "음식점 > 한식",
                  "road_address_name": "서울시 중간길 1",
                  "place_url": "https://place.map.kakao.com/middle",
                  "x": "127.002",
                  "y": "37.502",
                  "distance": "200"
                }
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GroupService groupService;

    @Autowired
    private VoteSessionService voteSessionService;

    @Autowired
    private VoteSessionJpaRepository voteSessionJpaRepository;

    @Autowired
    private StoreJpaRepository storeJpaRepository;

    @Autowired
    private KakaoMockServer kakaoMockServer;

    private UUID ownerUserId;
    private UUID groupId;
    private UUID voteSessionId;

    @BeforeEach
    void setUp() {
        kakaoMockServer.reset();
        ownerUserId = UUID.randomUUID();
        groupId = createGroup(ownerUserId).id();
        voteSessionId = createRestaurantSearchingSession().id();
    }

    @Test
    @DisplayName("그룹장이 식당을 검색하면 가까운 순으로 반환하고 저장한 뒤 식당 선택 단계로 변경한다")
    void searchNearby_asOwner_savesSortedStoresAndChangesStatus() throws Exception {
        kakaoMockServer.expectSuccess(KAKAO_RESULTS);

        mockMvc.perform(post(SEARCH_URI)
                        .with(authAs(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].distanceM").value(100))
                .andExpect(jsonPath("$.data[1].distanceM").value(100))
                .andExpect(jsonPath("$.data[2].distanceM").value(200))
                .andExpect(jsonPath("$.data[3].distanceM").value(300));

        kakaoMockServer.verify();
        org.assertj.core.api.Assertions.assertThat(storeJpaRepository.count()).isEqualTo(4);
        org.assertj.core.api.Assertions.assertThat(currentStatus())
                .isEqualTo(VoteSessionStatus.RESTAURANT_SELECTION);
    }

    @Test
    @DisplayName("인증되지 않은 요청은 식당 검색 전에 401로 차단한다")
    void searchNearby_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(SEARCH_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized());

        org.assertj.core.api.Assertions.assertThat(storeJpaRepository.count()).isZero();
        org.assertj.core.api.Assertions.assertThat(currentStatus())
                .isEqualTo(VoteSessionStatus.RESTAURANT_SEARCHING);
    }

    @Test
    @DisplayName("일반 그룹원은 식당을 검색할 수 없다")
    void searchNearby_asMember_returnsForbidden() throws Exception {
        UUID memberUserId = UUID.randomUUID();
        groupService.addMember(groupId, memberUserId);

        mockMvc.perform(post(SEARCH_URI)
                        .with(authAs(memberUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GROUP-006"));

        org.assertj.core.api.Assertions.assertThat(storeJpaRepository.count()).isZero();
        org.assertj.core.api.Assertions.assertThat(currentStatus())
                .isEqualTo(VoteSessionStatus.RESTAURANT_SEARCHING);
    }

    @ParameterizedTest
    @EnumSource(value = VoteSessionStatus.class, names = "RESTAURANT_SEARCHING", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("식당 검색 단계가 아니면 검색 요청을 거부한다")
    void searchNearby_inInvalidStatus_returnsConflict(VoteSessionStatus status) throws Exception {
        changeStatusDirectly(status);

        mockMvc.perform(post(SEARCH_URI)
                        .with(authAs(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION-003"));

        org.assertj.core.api.Assertions.assertThat(storeJpaRepository.count()).isZero();
        org.assertj.core.api.Assertions.assertThat(currentStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 목록을 반환하고 검색 단계에 머문다")
    void searchNearby_whenResultIsEmpty_returnsEmptyAndKeepsSearchingStatus() throws Exception {
        kakaoMockServer.expectSuccess("{\"documents\":[]}");

        mockMvc.perform(post(SEARCH_URI)
                        .with(authAs(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        kakaoMockServer.verify();
        org.assertj.core.api.Assertions.assertThat(storeJpaRepository.count()).isZero();
        org.assertj.core.api.Assertions.assertThat(currentStatus())
                .isEqualTo(VoteSessionStatus.RESTAURANT_SEARCHING);
    }

    @Test
    @DisplayName("빈 결과 후 반경을 넓혀 다시 검색하면 결과를 저장하고 식당 선택 단계로 변경한다")
    void searchNearby_afterEmptyResult_canRetryWithWiderRadius() throws Exception {
        kakaoMockServer.expectSuccess("{\"documents\":[]}", 1000);
        kakaoMockServer.expectSuccess(KAKAO_RESULTS, 3000);

        mockMvc.perform(post(SEARCH_URI)
                        .with(authAs(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithRadius(1000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        org.assertj.core.api.Assertions.assertThat(storeJpaRepository.count()).isZero();
        org.assertj.core.api.Assertions.assertThat(currentStatus())
                .isEqualTo(VoteSessionStatus.RESTAURANT_SEARCHING);

        mockMvc.perform(post(SEARCH_URI)
                        .with(authAs(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithRadius(3000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4));

        kakaoMockServer.verify();
        org.assertj.core.api.Assertions.assertThat(storeJpaRepository.count()).isEqualTo(4);
        org.assertj.core.api.Assertions.assertThat(currentStatus())
                .isEqualTo(VoteSessionStatus.RESTAURANT_SELECTION);
    }

    @Test
    @DisplayName("검색이 완료된 세션에 같은 요청을 다시 보내면 거부하고 중복 저장하지 않는다")
    void searchNearby_whenRepeated_returnsConflictWithoutDuplicateSave() throws Exception {
        kakaoMockServer.expectSuccess(KAKAO_RESULTS);

        mockMvc.perform(post(SEARCH_URI)
                        .with(authAs(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk());

        mockMvc.perform(post(SEARCH_URI)
                        .with(authAs(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION-003"));

        kakaoMockServer.verify();
        org.assertj.core.api.Assertions.assertThat(storeJpaRepository.count()).isEqualTo(4);
        org.assertj.core.api.Assertions.assertThat(currentStatus())
                .isEqualTo(VoteSessionStatus.RESTAURANT_SELECTION);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    @DisplayName("유효하지 않은 검색 요청은 400을 반환한다")
    void searchNearby_withInvalidRequest_returnsBadRequest(String description, String requestBody)
            throws Exception {
        mockMvc.perform(post(SEARCH_URI)
                        .with(authAs(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody.formatted(voteSessionId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-002"));

        org.assertj.core.api.Assertions.assertThat(storeJpaRepository.count()).isZero();
        org.assertj.core.api.Assertions.assertThat(currentStatus())
                .isEqualTo(VoteSessionStatus.RESTAURANT_SEARCHING);
    }

    @Test
    @DisplayName("카카오 API 오류가 발생하면 저장하지 않고 검색 단계에 머문다")
    void searchNearby_whenKakaoFails_returnsBadGatewayAndKeepsSearchingStatus() throws Exception {
        kakaoMockServer.expectServerError();

        mockMvc.perform(post(SEARCH_URI)
                        .with(authAs(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("KAKAOMAP-001"));

        kakaoMockServer.verify();
        org.assertj.core.api.Assertions.assertThat(storeJpaRepository.count()).isZero();
        org.assertj.core.api.Assertions.assertThat(currentStatus())
                .isEqualTo(VoteSessionStatus.RESTAURANT_SEARCHING);
    }

    private String validRequest() {
        return requestWithRadius(1000);
    }

    private String requestWithRadius(int radiusM) {
        return VALID_REQUEST.formatted(voteSessionId, radiusM);
    }

    private VoteSession createRestaurantSearchingSession() {
        VoteSession session = voteSessionService.createManualVoteSession(
                groupId,
                ownerUserId,
                "점심 메뉴 투표",
                "고기",
                null
        );
        voteSessionService.changeVoteSessionStatus(session.id(), VoteSessionStatus.MENU_RECOMMENDING);
        voteSessionService.changeVoteSessionStatus(session.id(), VoteSessionStatus.MENU_VOTING);
        return voteSessionService.changeVoteSessionStatus(
                session.id(),
                VoteSessionStatus.RESTAURANT_SEARCHING
        );
    }

    private Group createGroup(UUID ownerId) {
        return groupService.create(new NewGroup(
                ownerId,
                "식당 검색 테스트-" + UUID.randomUUID(),
                "서울특별시 강남구",
                37.5,
                127.0,
                1000,
                LocalTime.of(12, 0),
                6
        ));
    }

    private void changeStatusDirectly(VoteSessionStatus status) {
        VoteSessionEntity entity = voteSessionJpaRepository.findById(voteSessionId).orElseThrow();
        entity.updateStatus(status);
        voteSessionJpaRepository.flush();
    }

    private VoteSessionStatus currentStatus() {
        return voteSessionJpaRepository.findById(voteSessionId)
                .orElseThrow()
                .getVoteSessionStatus();
    }

    private static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("투표 ID 없음", """
                        {"voteSessionId":null,"keyword":"삼겹살","longitude":127.0,"latitude":37.5,"radiusM":1000}
                        """),
                Arguments.of("검색어 공백", """
                        {"voteSessionId":"%s","keyword":" ","longitude":127.0,"latitude":37.5,"radiusM":1000}
                        """),
                Arguments.of("검색어 100자 초과",
                        "{\"voteSessionId\":\"%s\",\"keyword\":\""
                                + "a".repeat(101)
                                + "\",\"longitude\":127.0,\"latitude\":37.5,\"radiusM\":1000}"),
                Arguments.of("경도 최솟값 미만", """
                        {"voteSessionId":"%s","keyword":"삼겹살","longitude":-180.1,"latitude":37.5,"radiusM":1000}
                        """),
                Arguments.of("경도 최댓값 초과", """
                        {"voteSessionId":"%s","keyword":"삼겹살","longitude":180.1,"latitude":37.5,"radiusM":1000}
                        """),
                Arguments.of("위도 최솟값 미만", """
                        {"voteSessionId":"%s","keyword":"삼겹살","longitude":127.0,"latitude":-90.1,"radiusM":1000}
                        """),
                Arguments.of("위도 최댓값 초과", """
                        {"voteSessionId":"%s","keyword":"삼겹살","longitude":127.0,"latitude":90.1,"radiusM":1000}
                        """),
                Arguments.of("검색 반경 0", """
                        {"voteSessionId":"%s","keyword":"삼겹살","longitude":127.0,"latitude":37.5,"radiusM":0}
                        """),
                Arguments.of("검색 반경 음수", """
                        {"voteSessionId":"%s","keyword":"삼겹살","longitude":127.0,"latitude":37.5,"radiusM":-1}
                        """),
                Arguments.of("검색 반경 최댓값 초과", """
                        {"voteSessionId":"%s","keyword":"삼겹살","longitude":127.0,"latitude":37.5,"radiusM":10001}
                        """)
        );
    }

    private static RequestPostProcessor authAs(UUID userId) {
        User user = new User(
                "테스터",
                "테스터",
                UserStatus.ACTIVE,
                Provider.NAVER,
                "provider-id",
                "010-0000-0000",
                "user@example.com",
                false,
                null,
                null,
                null
        );
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, user);
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );
        return authentication(token);
    }

    @TestConfiguration
    static class KakaoMockConfiguration {

        @Bean
        KakaoMockServer kakaoMockServer() {
            return new KakaoMockServer();
        }

        @Bean
        @Primary
        RestClient testKakaoClient(KakaoMockServer mockServer) {
            return mockServer.restClient();
        }
    }

    static class KakaoMockServer {

        private final MockRestServiceServer server;
        private final RestClient restClient;

        KakaoMockServer() {
            RestClient.Builder builder = RestClient.builder()
                    .baseUrl("https://dapi.kakao.com");
            this.server = MockRestServiceServer.bindTo(builder).build();
            this.restClient = builder.build();
        }

        RestClient restClient() {
            return restClient;
        }

        void reset() {
            server.reset();
        }

        void expectSuccess(String responseBody) {
            expectSuccess(responseBody, 1000);
        }

        void expectSuccess(String responseBody, int radiusM) {
            server.expect(method(HttpMethod.GET))
                    .andExpect(queryParam("query", "%EC%82%BC%EA%B2%B9%EC%82%B4"))
                    .andExpect(queryParam("x", "127.0"))
                    .andExpect(queryParam("y", "37.5"))
                    .andExpect(queryParam("radius", Integer.toString(radiusM)))
                    .andExpect(queryParam("category_group_code", "FD6"))
                    .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
        }

        void expectServerError() {
            server.expect(method(HttpMethod.GET))
                    .andRespond(withServerError());
        }

        void verify() {
            server.verify();
        }
    }
}
