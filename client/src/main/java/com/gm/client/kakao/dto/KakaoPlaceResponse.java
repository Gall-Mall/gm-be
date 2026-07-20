package com.gm.client.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gm.client.kakao.exception.KakaoApiException;
import com.gm.client.kakao.exception.KakaoErrorCode;
import com.gm.core.domain.store.model.Coordinate;
import com.gm.core.domain.store.model.Provider;
import com.gm.core.domain.store.model.Store;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoPlaceResponse(
        List<Document> documents
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Document(
            String id,

            @JsonProperty("place_name")
            String placeName,

            @JsonProperty("category_name")
            String categoryName,

            @JsonProperty("road_address_name")
            String roadAddressName,

            @JsonProperty("place_url")
            String placeUrl,

            String x,
            String y,
            String distance
) {
        public Store toStore() {

            return new Store(
                    id,
                    placeName,
                    validated(roadAddressName),
                    categoryName,
                    validated(placeUrl),
                    new Coordinate(
                            Double.parseDouble(x),
                            Double.parseDouble(y)
                    ),
                    Provider.KAKAO,
                    validated(distance)
            );
        }

        private String validated(String field) {
            if(field == null || field.isBlank() ) {
                throw new KakaoApiException(KakaoErrorCode.KAKAO_RESPONSE_ERROR);
            }
            return field;
        }
    }



}
