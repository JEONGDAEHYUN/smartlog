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

    // 특정 일정의 미발송 알림 조회 (수정 시 기존 알림 삭제용)
    List<NotiInfo> findBySchInfoSchIdAndIsSent(Long schId, String isSent);

    // 발송완료된 알림 조회
    List<NotiInfo> findByUserInfoUserIdAndIsSent(Long userId, String isSent);

    // 발송완료 + 미읽음 알림 조회
    List<NotiInfo> findByUserInfoUserIdAndIsSentAndIsRead(Long userId, String isSent, String isRead);

    // 특정 일정의 모든 알림 삭제 (일정 삭제 시 FK 제약 회피용)
    void deleteBySchInfoSchId(Long schId);
}
