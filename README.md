# SmartLog — AI 기반 업무일지 관리 시스템

> 한국폴리텍대학 빅데이터소프트웨어과 졸업작품
> 학번: 2520110199 / 이름: 정대현

---

## 프로젝트 개요

사용자가 입력한 원본 업무 메모를 Google Gemini AI가 자동으로 정제·요약하고,
일정 등록부터 보고서 생성까지 업무 기록 프로세스를 통합 관리하는 웹 기반 시스템입니다.

- **배포 URL**: https://smartlog-sync.kr
- **GitHub**: https://github.com/JEONGDAEHYUN/smartlog

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Spring Boot 4.0.5, Java 17, Gradle |
| Frontend | Thymeleaf, Bootstrap 5, Fetch API |
| DB (정형) | MariaDB 11.x (AWS RDS) |
| DB (비정형) | MongoDB 7.0 |
| 인증 | Spring Security (BCrypt, 로그인 5회 실패 잠금) |
| 외부 API | Google Gemini API, 기상청 단기예보 API, 공공데이터포털 공휴일 API |
| 이메일 | Gmail SMTP |
| 배포 | AWS EC2 + Nginx + Let's Encrypt SSL |

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| 업무일지 작성 | 원본 메모 입력 → Gemini AI 자동 정제·요약 |
| 일정 관리 | 월간/주간/일간 달력, 충돌 감지, 반복 일정 |
| 이메일 알림 | 마감 1시간·30분·15분 전 자동 이메일 발송 |
| 보고서 생성 | AI 기반 업무 보고서 자동 생성 + 파일 첨부 |
| 공휴일 표시 | 공공데이터포털 API 연동, 월간 달력 빨간색 표시 |
| 관리자 권한 | 회원 권한 전환(ROLE_USER↔ROLE_ADMIN), 계정 잠금/해제 |
| 일정 별표 | 중요 일정 금색 ★ 즐겨찾기, 달력 강조 표시 |

---

## 실행 방법

### 사전 요구사항
- Java 17
- MariaDB, MongoDB 실행 중
- `src/main/resources/application-local.yml` 작성 (아래 참고)

### application-local.yml 설정
```yaml
spring:
  datasource:
    url: jdbc:mariadb://[DB_HOST]:3306/smartlog
    username: [DB_USER]
    password: [DB_PASSWORD]
  mongodb:
    uri: mongodb://[MONGO_USER]:[MONGO_PASSWORD]@[MONGO_HOST]:27017/MyDB?authSource=MyDB
  mail:
    username: [GMAIL_ADDRESS]
    password: [GMAIL_APP_PASSWORD]

gemini:
  api-key: [GEMINI_API_KEY]

weather:
  api-key: [WEATHER_API_KEY]

holiday:
  api-key: [HOLIDAY_API_KEY]
```

### 로컬 실행
```bash
# Windows
.\gradlew bootRun

# Mac/Linux
./gradlew bootRun
```

접속: http://localhost:8080

### 빌드
```bash
.\gradlew clean bootJar -x test
```

---

## DB 구조

### MariaDB 테이블
| 테이블 | 설명 |
|--------|------|
| USER_INFO | 회원 정보, 권한, 잠금 상태 |
| SCH_INFO | 일정 정보, 별표, 이메일 알림 여부 |
| NOTI_INFO | 알림 정보, 발송 상태 |
| REPORT_INFO | AI 생성 보고서 |
| FILE_INFO | 보고서 첨부파일 |

### MongoDB 컬렉션
| 컬렉션 | 설명 |
|--------|------|
| WORKLOG | 원본 업무일지, AI 정제 결과 |

---

## 패키지 구조

```
com.smartlog.sync
├── controller       # URL 매핑, View 반환
├── service          # 비즈니스 로직 (인터페이스 + 구현체)
├── repository       # DB 접근 (JpaRepository / MongoRepository)
│   └── entity       # @Entity, @Document
├── dto              # 계층 간 데이터 전달 (Java 17 record)
└── config           # Security, RestClient, Gemini, Password 설정
```

---

## 커밋 이력 (추가 기능)

| 커밋 | 날짜 | 내용 |
|------|------|------|
| `b380fdf` | 2026-09-20 | feat: 일정별 이메일 알림 발송 기능 추가 |
| `bf27978` | 2026-09-20 | fix: 이메일 알림 토글 버그 수정, LazyInitializationException 방지 |
| `e8dfafa` | 2026-09-21 | feat: 관리자 권한 관리 기능 추가 |
| `8effa03` | 2026-09-22 | feat: 일정 별표(즐겨찾기) 기능 추가 |
| `aad93ad` | 2026-09-23 | feat: 보고서 파일 첨부 기능 추가 |
| `0de7f7e` | 2026-09-26 | feat: 월간 달력 공휴일 표시 |
| `810c8c8` | 2026-09-27 | fix: 공휴일 캐시 메모리 누수 방지 및 로그 개선 |