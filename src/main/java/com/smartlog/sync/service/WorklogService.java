package com.smartlog.sync.service;

import com.smartlog.sync.entity.mongodb.Worklog;
import com.smartlog.sync.repository.mongodb.WorklogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// 업무일지 비즈니스 로직
@Service
@RequiredArgsConstructor
@Slf4j
public class WorklogService {

    private final WorklogRepository worklogRepository;
    private final GeminiService geminiService;

    // 원본 메모 저장 (RAW 상태)
    public Worklog saveRaw(Long userId, String rawContent) {
        Worklog worklog = Worklog.builder()
                .logId(UUID.randomUUID().toString())
                .userId(userId)
                .rawContent(rawContent)
                .status("RAW")
                .createdAt(LocalDateTime.now())
                .build();
        return worklogRepository.save(worklog);
    }

    // AI 정제 요청 (RAW → PROCESSING → SUCCESS/FAILED)
    public Worklog refine(String logId) {
        Worklog worklog = worklogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("업무일지를 찾을 수 없습니다"));

        worklog.setStatus("PROCESSING");
        worklog.setUpdatedAt(LocalDateTime.now());
        worklogRepository.save(worklog);

        try {
            String refined = geminiService.refineWorklog(worklog.getRawContent());
            worklog.setRefinedContent(refined);
            worklog.setStatus("SUCCESS");
        } catch (Exception e) {
            log.error("AI 정제 실패 [logId={}]: {}", logId, e.getMessage());
            worklog.setStatus("FAILED");
        }

        worklog.setUpdatedAt(LocalDateTime.now());
        return worklogRepository.save(worklog);
    }

    // 원본 메모 저장 + 즉시 AI 정제
    public Worklog saveAndRefine(Long userId, String rawContent) {
        Worklog worklog = saveRaw(userId, rawContent);
        return refine(worklog.getLogId());
    }

    // 사용자의 업무일지 목록 조회
    public List<Worklog> getByUserId(Long userId) {
        return worklogRepository.findByUserId(userId);
    }

    // 업무일지 상세 조회
    public Worklog getByLogId(String logId) {
        return worklogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("업무일지를 찾을 수 없습니다"));
    }

    // 업무일지 삭제
    public void delete(String logId) {
        worklogRepository.deleteById(logId);
    }
}
