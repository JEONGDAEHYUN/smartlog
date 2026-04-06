package com.smartlog.sync.repository.mariadb;

import com.smartlog.sync.entity.mariadb.ReportInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// 보고서정보 Repository (MariaDB)
@Repository
public interface ReportInfoRepository extends JpaRepository<ReportInfo, Long> {

    // 특정 사용자의 보고서 목록 조회
    List<ReportInfo> findByUserInfoUserId(Long userId);
}
