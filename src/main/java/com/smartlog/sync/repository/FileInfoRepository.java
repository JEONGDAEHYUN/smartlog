package com.smartlog.sync.repository;

import com.smartlog.sync.repository.entity.FileInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileInfoRepository extends JpaRepository<FileInfo, Long> {

    // 특정 보고서에 첨부된 파일 목록 조회
    List<FileInfo> findByRepIdOrderByRegDtAsc(Long repId);

    // 보고서 삭제 시 관련 파일 레코드 일괄 삭제
    void deleteByRepId(Long repId);
}