-- Spring Security RememberMe (Persistent Token) 표준 스키마
-- JdbcTokenRepositoryImpl 이 기대하는 컬럼 이름/타입이라 변경 금지
-- 실행: MariaDB(smartlog DB) 에 1회만 실행
CREATE TABLE IF NOT EXISTS persistent_logins (
    username   VARCHAR(64)  NOT NULL,                    -- 로그인 ID (USER_INFO.USER_EMAIL)
    series     VARCHAR(64)  NOT NULL PRIMARY KEY,        -- 디바이스 식별용 시리즈 ID
    token      VARCHAR(64)  NOT NULL,                    -- 매 요청마다 회전되는 토큰
    last_used  TIMESTAMP    NOT NULL                     -- 마지막 사용 시각 (만료 판단용)
);
