package com.smartlog.sync.repository;

import com.smartlog.sync.repository.entity.SchCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// 반복 일정 완료 기록 Repository
@Repository
public interface SchCompletionRepository extends JpaRepository<SchCompletion, Long> {

    // 특정 일정의 특정 날짜 완료 기록 조회 (토글 판정용)
    Optional<SchCompletion> findBySchIdAndCompletionDate(Long schId, LocalDate completionDate);

    // 여러 일정의 완료 기록 일괄 조회 (apiEvents 에서 활용 — N+1 회피)
    List<SchCompletion> findBySchIdIn(List<Long> schIds);

    // 특정 일정의 모든 완료 기록 삭제 (일정 삭제 시 cleanup, FK CASCADE 보조)
    void deleteBySchId(Long schId);
}