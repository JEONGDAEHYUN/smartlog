package com.smartlog.sync.repository.mariadb;

import com.smartlog.sync.entity.mariadb.NotiInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// 알림정보 Repository (MariaDB)
@Repository
public interface NotiInfoRepository extends JpaRepository<NotiInfo, Long> {

    // 특정 사용자의 알림 목록 조회
    List<NotiInfo> findByUserInfoUserId(Long userId);

    // 읽지 않은 알림 목록 조회
    List<NotiInfo> findByUserInfoUserIdAndIsRead(Long userId, String isRead);

    // 미발송 알림 목록 조회 (스케줄러용)
    List<NotiInfo> findByIsSent(String isSent);
}
