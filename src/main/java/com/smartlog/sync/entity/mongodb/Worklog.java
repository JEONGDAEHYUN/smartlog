package com.smartlog.sync.entity.mongodb;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;

// 업무일지 Document (MongoDB)
@Document(collection = "WORKLOG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Worklog {

    @Id
    @Field("LOG_ID")
    private String logId; // 로그 ID (UUID v4, PK) — MariaDB SCH_INFO.LOG_ID와 Cross-DB 연결

    @Field("USER_ID")
    private Long userId; // 사용자 ID (MariaDB USER_INFO.USER_ID와 타입 통일)

    @Field("RAW_CONTENT")
    private String rawContent; // 사용자 원본 입력 내용

    @Field("REFINED_CONTENT")
    private String refinedContent; // Gemini API 정제 결과

    @Field("STATUS")
    private String status; // 처리 상태 (RAW / PROCESSING / SUCCESS / FAILED)

    @Field("CREATED_AT")
    private LocalDateTime createdAt; // 생성 일시

    @Field("UPDATED_AT")
    private LocalDateTime updatedAt; // 수정 일시
}
