package com.smartlog.sync.service;

import java.util.Map;

// 공휴일 조회 서비스 (공공데이터포털 한국천문연구원 특일 정보 API)
public interface HolidayService {

    // 해당 연·월의 공휴일 반환 — key: 일(1~31), value: 공휴일명
    Map<Integer, String> getHolidays(int year, int month);
}