package com.smartlog.sync.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

// 기상청 단기예보 API 응답 파싱용 DTO
// 응답 구조: { response: { body: { items: { item: [...] } } } }
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherApiResponseDto {

    private Response response;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private Body body;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private Items items;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        private List<Item> item;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String category;     // T1H(기온), REH(습도), WSD(풍속), PTY(강수형태), SKY(하늘상태)
        private String obsrValue;    // 초단기실황 값
        private String fcstValue;    // 초단기예보 값
    }
}
