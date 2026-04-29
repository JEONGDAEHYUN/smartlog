package com.smartlog.sync.service.impl;

import com.smartlog.sync.dto.WorklogDto;
import com.smartlog.sync.repository.entity.Worklog;
import com.smartlog.sync.repository.WorklogRepository;
import com.smartlog.sync.service.GeminiService;
import com.smartlog.sync.service.WorklogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// 업무일지 비즈니스 로직 구현체
@Service
@RequiredArgsConstructor
@Slf4j
public class WorklogServiceImpl implements WorklogService {

    private final WorklogRepository worklogRepository;
    private final GeminiService geminiService;

    @Override
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

    @Override
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

    @Override
    public Worklog saveAndRefine(Long userId, String rawContent) {
        Worklog worklog = saveRaw(userId, rawContent);
        return refine(worklog.getLogId());
    }

    @Override
    public List<WorklogDto> getByUserId(Long userId) {
        return WorklogDto.fromList(worklogRepository.findByUserId(userId));
    }

    @Override
    public WorklogDto getByLogId(String logId) {
        Worklog worklog = worklogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("업무일지를 찾을 수 없습니다"));
        return WorklogDto.from(worklog);
    }

    @Override
    public void delete(String logId) {
        worklogRepository.deleteById(logId);
    }
}
