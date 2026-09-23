package com.smartlog.sync.service;

import com.smartlog.sync.dto.FileInfoDto;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

// 첨부파일 업로드/다운로드/삭제 서비스
public interface FileService {

    // 파일 업로드 — 디스크 저장 + DB 메타데이터 저장
    FileInfoDto upload(Long repId, Long userId, MultipartFile file) throws IOException;

    // 파일 삭제 — 디스크 + DB 동시 삭제 (소유권 검증 포함)
    void delete(Long fileId, Long userId) throws IOException;

    // 보고서별 첨부파일 목록 조회
    List<FileInfoDto> getByRepId(Long repId);

    // 파일 다운로드용 Resource 로드 (소유권 검증 포함)
    Resource loadAsResource(Long fileId, Long userId);

    // 파일 메타데이터 단건 조회
    FileInfoDto getById(Long fileId);
}