package com.smartlog.sync.repository.mariadb;

import com.smartlog.sync.entity.mariadb.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// 회원정보 Repository (MariaDB)
@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {

    // 이메일로 회원 조회 (로그인용)
    Optional<UserInfo> findByUserEmail(String userEmail);

    // 이메일 중복 확인 (회원가입용)
    boolean existsByUserEmail(String userEmail);

    // 이름 + 조직명으로 회원 조회 (아이디 찾기용)
    Optional<UserInfo> findByUserNameAndOrgName(String userName, String orgName);
}
