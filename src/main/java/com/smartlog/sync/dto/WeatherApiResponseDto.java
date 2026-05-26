package com.smartlog.sync.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// 기상청 단기예보 API 응답 파싱용 DTO (immutable record + 중첩 record)
// 응답 구조: { response: { body: { items: { item: [...] } } } }
@JsonIgnoreProperties(ignoreUnknown = true)
public record WeatherApiResponseDto(Response response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(Body body) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(Items items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(List<Item> item) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String category,    // T1H(기온), REH(습도), WSD(풍속), PTY(강수형태), SKY(하늘상태)
            String obsrValue,   // 초단기실황 값
            String fcstValue    // 초단기예보 값
    ) {}
}