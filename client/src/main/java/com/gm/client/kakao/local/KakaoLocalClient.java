package com.gm.client.kakao.local;

import com.gm.client.kakao.local.dto.PlaceSearchRequest;
import com.gm.client.kakao.local.dto.PlaceSearchResponse;
import com.gm.client.kakao.local.util.SearchResponseWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;

import java.util.List;

@RequiredArgsConstructor
public class KakaoLocalClient {

    private final RestClient restClient;

    public List<PlaceSearchResponse> callLocal(PlaceSearchRequest searchRequest) {

        SearchResponseWrapper responseWrapper = restClient
                .get()
                .uri(uriBuilder -> uriBuilder.path("/v2/local/search/keyword.json")
                        .queryParam("query", searchRequest.menuName())
                        .queryParam("x", searchRequest.longitude())
                        .queryParam("y", searchRequest.latitude())
                        .queryParam("radius", searchRequest.radius())
                        .build())
                .retrieve()
                .body(SearchResponseWrapper.class);

        if(responseWrapper == null) {
            return List.of();
        }
        return responseWrapper.documents();
    }
}
