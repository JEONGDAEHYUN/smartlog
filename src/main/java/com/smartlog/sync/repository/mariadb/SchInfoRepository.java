package com.smartlog.sync.repository.mariadb;

import com.smartlog.sync.entity.mariadb.SchInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// 일정정보 Repository (MariaDB)
@Repository
public interface SchInfoRepository extends JpaRepository<SchInfo, Long> {

    // 특정 사용자의 일정 목록 조회
    List<SchInfo> findByUserInfoUserId(Long userId);

    // MongoDB WORKLOG 연결용 LOG_ID로 일정 조회
    SchInfo findByLogId(String logId);

    // 반복 업무 목록 조회 (recurring이 NULL이 아닌 것)
    List<SchInfo> findByUserInfoUserIdAndRecurringIsNotNull(Long userId);
}
