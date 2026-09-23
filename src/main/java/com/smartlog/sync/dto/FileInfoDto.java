package com.smartlog.sync.dto;

import com.smartlog.sync.repository.entity.FileInfo;

import java.time.LocalDateTime;
import java.util.List;

// 첨부파일 응답 DTO
public record FileInfoDto(
        Long fileId,
        Long repId,
        Long userId,
        String originalName,
        String storedName,
        Long fileSize,
        String contentType,
        LocalDateTime regDt
) {
    public static FileInfoDto from(FileInfo entity) {
        return new FileInfoDto(
                entity.getFileId(),
                entity.getRepId(),
                entity.getUserId(),
                entity.getOriginalName(),
                entity.getStoredName(),
                entity.getFileSize(),
                entity.getContentType(),
                entity.getRegDt()
        );
    }

    public static List<FileInfoDto> fromList(List<FileInfo> entities) {
        return entities.stream().map(FileInfoDto::from).toList();
    }

    // 파일 크기를 사람이 읽기 쉬운 단위로 변환
    public String fileSizeDisplay() {
        if (fileSize == null || fileSize == 0) return "0 B";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024));
    }

    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    public boolean isPdf() {
        return "application/pdf".equals(contentType);
    }
}