package com.smartlog.sync.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// 첨부파일 메타데이터 Entity (실제 파일은 서버 디스크에 저장)
@Entity
@Table(name = "FILE_INFO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FILE_ID")
    private Long fileId;

    @Column(name = "REP_ID", nullable = false)
    private Long repId; // REPORT_INFO 연결 FK

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "ORIGINAL_NAME", length = 255, nullable = false)
    private String originalName; // 사용자가 업로드한 원본 파일명

    @Column(name = "STORED_NAME", length = 255, nullable = false)
    private String storedName; // 디스크에 저장된 UUID 기반 파일명 (중복/보안 방지)

    @Column(name = "FILE_SIZE")
    private Long fileSize; // 바이트 단위

    @Column(name = "CONTENT_TYPE", length = 100)
    private String contentType; // MIME 타입 (image/jpeg, application/pdf 등)

    @Column(name = "REG_DT", nullable = false, updatable = false)
    private LocalDateTime regDt;

    @PrePersist
    protected void onCreate() {
        this.regDt = LocalDateTime.now();
    }
}