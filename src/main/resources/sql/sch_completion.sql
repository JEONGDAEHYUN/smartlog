-- 반복 일정의 "사용자가 직접 완료 표시한 날짜" 기록 테이블
-- 가상 이벤트는 DB row 가 없으므로, 어느 날짜를 DONE 처리했는지 별도 추적
-- 실행: MariaDB(smartlog DB) 에 1회만 실행
CREATE TABLE IF NOT EXISTS sch_completion (
    completion_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    sch_id          BIGINT NOT NULL,                  -- SCH_INFO(SCH_ID) FK 참조 (반복 일정)
    completion_date DATE NOT NULL,                    -- 사용자가 완료 처리한 날짜
    completed_at    DATETIME NOT NULL DEFAULT NOW(),  -- 클릭 시각 (감사용)
    UNIQUE KEY uk_sch_completion (sch_id, completion_date),
    CONSTRAINT fk_sch_completion_sch FOREIGN KEY (sch_id) REFERENCES sch_info(sch_id) ON DELETE CASCADE
);