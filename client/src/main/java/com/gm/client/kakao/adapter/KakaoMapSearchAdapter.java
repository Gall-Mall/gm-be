package com.gm.client.kakao.adapter;

import com.gm.client.kakao.dto.KakaoPlaceResponse;
import com.gm.client.kakao.exception.KakaoApiException;
import com.gm.client.kakao.exception.KakaoErrorCode;
import com.gm.core.domain.store.StoreSearchPort;
import com.gm.core.domain.store.model.Coordinate;
import com.gm.core.domain.store.model.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;


@RequiredArgsConstructor
@Component
public class KakaoMapSearchAdapter implements StoreSearchPort {

    private final RestClient kakaoClient;

    @Override
    public List<Store> searchNearby(String keyword, Coordinate coordinate, int radius) {

        try {
            KakaoPlaceResponse kakaoPlaceResponse = kakaoClient
                    .get()
                    .uri(uriBuilder -> uriBuilder.path("/v2/local/search/keyword.json")
                            .queryParam("query", keyword)
                            .queryParam("x", coordinate.x())
                            .queryParam("y", coordinate.y())
                            .queryParam("radius", radius)
                            .queryParam("category_group_code", "FD6")
                            .build())
                    .retrieve()
                    .body(KakaoPlaceResponse.class);

            if (kakaoPlaceResponse == null || kakaoPlaceResponse.documents() == null) {
                return List.of();
            }

            return kakaoPlaceResponse
                    .documents()
                    .stream()
                    .map(KakaoPlaceResponse.Document::toStore)
                    .sorted((store1, store2) -> Integer.parseInt(store2.distance()) - Integer.parseInt(store1.distance()))
                    .toList();
        } catch (RestClientException e) {
            throw new KakaoApiException(KakaoErrorCode.KAKAO_API_ERROR);
        }

    }



}
