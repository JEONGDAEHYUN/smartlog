package com.smartlog.sync.dto;

// 보고서 종류 enum — 코드 ↔ 한글 제목 매핑 일원화
public enum ReportType {
    WEEKLY("weekly", "주간 업무요약 보고서"),
    MONTHLY("monthly", "월간 업무요약 보고서"),
    HANDOVER("handover", "인수인계 보고서");

    private final String code;
    private final String title;

    ReportType(String code, String title) {
        this.code = code;
        this.title = title;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    // 코드 문자열로 enum 조회 (없으면 null)
    public static ReportType fromCode(String code) {
        for (ReportType type : values()) {
            if (type.code.equals(code)) return type;
        }
        return null;
    }

    // 코드 문자열 → 보고서 제목 (없으면 코드 그대로 + " 보고서")
    public static String toTitle(String code) {
        ReportType type = fromCode(code);
        return type != null ? type.title : code + " 보고서";
    }
}
