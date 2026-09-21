package com.smartlog.sync.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// 회원정보 Entity, 이 클래스가 데이터베이스 테이블과 매핑되는
// JPA 엔티티 객체임을 선언
@Entity

// 엔티티와 실제 DB의 'USER_INFO' 테이블을 이름으로 매핑
@Table(name = "USER_INFO")

// 클래스 내 모든 필드에 대한 Getter 메서드를 자동으로 생성
// 롬복이 컴파일할 때 자동으로 get 함수를 만들어줌
// 예시는 이와 같음 -> getUserId(), getUserEmail(), getuserPwd()
@Getter

// JPA 필수 사항: 파라미터가 없는 기본 생성자를 자동으로 생성
// 객체를 생성할 때   아무런 값(인자)도 전달받지 않는   형태의  빈 생성자
// 생성자는  객체를 처음 메모리에 올릴 때(new 키워드를 쓸 때)  호출되는 함수
// 파라미터(매개변수) 가 없다는 것은   소괄호() 안이 비어있다는 뜻
// 왜 필요한가? -> JPA와의 관계 때문이다. -> 어떤 관계?
// DB에서 user정보를 조회 -> JPA는 매개변수가 없는 기본 생성자 (new UserInfo())를
// 호출 ->  객체를 만들고  -> Reflection이라는 기술로 필드 값을 채워 넣는다.
// 그렇기에 기본 생성자 구조가 반드시 필요하다. -> 왜? -> 그래야만 DB조회가 정상 작동함
@NoArgsConstructor

// 빌더 패턴 사용을 위해   모든 필드를 인자로 받는     생성자를 자동으로 생성
// 모든 변수를 채우면서    객체를 만들 수 있는         생성자를 확보해야
// 롬복의 @Builder가      오작동 없이                 빌더 코드를 조립
// 자바에선 원래          객체를 만들려면 필드값 채운   생성자가 필요함
// 이것이 없으면          컴파일 에러 또는 오작동 발생  따라서 빌더를 위해 필요
@AllArgsConstructor

// 디자인 패턴 중 하나인   '빌더(Builder) 패턴' 스타일로   객체 생성을 지원
// 디자인 패턴이란         소프트웨어 설계도 이다.  ->     구조적 공식 정의
// builder 패턴 적용시     필드의 순서가 바뀌어도 상관 없다.  가독성도 좋다.
@Builder

// UserInfo 필드 정의
public class UserInfo {

    // 이 필드가   테이블의   기본키(Primary Key)임을 선언
    @Id

    // 기본키   생성을   DB에   위임 -> MySQL의 Auto Increment 방식
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    // 실제 DB 테이블의   'USER_ID' 컬럼과     매핑
    @Column(name = "USER_ID")

    // 사용자 고유번호 (PK, Auto Increment)
    private Long userId;

    // 컬럼명 'SUER_EMAIL',     길이 100자 제한, 필수값(NOT NULL), 중복 불가능(UNIQUE)설정
    @Column(name = "USER_EMAIL", length = 100, nullable = false, unique = true)
    private String userEmail; // 로그인 ID (이메일)

    // 컬럼명 'USER_PWD',      길이 255자 제한 해시 암호화 텍스트가 저장됨으로 넉넉하게 길이 255자 지정
                                            // 필수값(NOT NULL)
    @Column(name = "USER_PWD", length = 255, nullable = false)
    private String userPwd; // 비밀번호 (BCrypt 암호화 저장)

    // 컬럼명 'USER_NAME',      길이 100자 제한,  필수값(NOT NULL)
    @Column(name = "USER_NAME", length = 100, nullable = false)
    private String userName; // 이름

    // 컬럼명 'ORG_NAME'        길이 100자 제한,  필수값(NOT NULL)
    @Column(name = "ORG_NAME", length = 100, nullable = false)
    private String orgName; // 조직명

    // 컬럼명 'USER_ROLE',      길이 20자 제한,   필수값(NOT NULL)
    @Column(name = "USER_ROLE", length = 20, nullable = false)
    private String userRole; // 권한 (ROLE_USER / ROLE_ADMIN)

    // 컬럼명 'REG_DT',         필수값(NOT NULL), 데이터 최초 저장 후
    //                                           SQL UPDATE문에 이 컬럼을
    //                                           포함하지 않음
    @Column(name = "REG_DT", nullable = false, updatable = false)
    private LocalDateTime regDt; // 등록일

    // 롬복 빌더 패턴으로    객체를 생성할 때도   이 필드의 기본값(0)을 강제로 유지하도록 보장
    @Builder.Default
    // 컬럼명 'FAIL_COUNT',     필수값(NOT NULL),   DB 테이블 생성(DDL) 시 직접 구문 지정 (기존 레코드 보정용 DEFAULT0)
    @Column(name = "FAIL_COUNT", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 0")
    // Integer타입은  래퍼클래스 객체 이다.  객체 타입은 DB나 빌더 패턴의 작동 방식에따라 null이 들어올 가능성이 있다.
    private Integer failCount = 0; // 로그인 연속 실패 횟수 (기존 레코드는 0으로 자동 보정)

    // 컬럼명 'LOCKED_UNTIL'
    @Column(name = "LOCKED_UNTIL")
    private LocalDateTime lockedUntil; // 계정 잠금 해제 시각 (null이면 잠금 아님)


    // Entity가  영속성 컨텍스트에 저장(INSERT)되기 직전에   메서드를 자동으로 실행함
    @PrePersist
    protected void onCreate() {
        // regDt에   현재 시간을 담는다.  -> 현재 시간으로 등록일 강제 설정
        this.regDt = LocalDateTime.now();
        // 실패 카운트가  비어있다면  0으로
        if (this.failCount == null) this.failCount = 0;
    }

    // 비즈니스 메서드 : 비밀번호 변경 — BCrypt 인코딩된 값만 받는다
    public void changePassword(String encodedPassword) {
        // userPwd에   BCrypt 인코딩된 값을 담는다.
        this.userPwd = encodedPassword;
    }

    // 비즈니스 메서드 : 프로필 일괄 수정  suerName(이름) orgName(조직명)
    public void updateProfile(String userName, String orgName) {
        // UserInfo의 userName에 수정된 userName값을 담는다.
        this.userName = userName;
        // UserInfo의 orgName에  수정된 orgName값을 담는다.
        this.orgName = orgName;
    }

    // 비즈니스 메서드 : 이메일(로그인 ID) 변경
    // 중복 검사는 Service 계층에서 선행
    public void changeEmail(String userEmail) {
        // UserInfo의 userEmail에   수정된 이메일(로그인 ID) 값을 담는다.
        this.userEmail = userEmail;
    }

    // 비즈니스 메서드 : 로그인 실패 카운트 1 증가
    public void incrementFailCount() {
        // null 일때(참일때) 0을 사용하겠다.  null이 아닐때 this.failCount 값을 사용하겠다.
        this.failCount = (this.failCount == null ? 0 : this.failCount) + 1;
    }

    // 비즈니스 메서드 : 계정을 minutes 분 동안 잠금
    public void lockFor(int minutes) {
        //                 현시간               n분 뒤로 잠금
        this.lockedUntil = LocalDateTime.now().plusMinutes(minutes);
    }

    // 비즈니스 메서드 : 관리자 권한 변경 (ROLE_USER ↔ ROLE_ADMIN)
    public void changeRole(String role) {
        this.userRole = role;
    }

    // 비즈니스 메서드 : 관리자에 의한 계정 장기 잠금 (365일)
    public void lockByAdmin() {
        this.lockedUntil = LocalDateTime.now().plusDays(365);
    }

    // 비즈니스 메서드 : 로그인 성공 또는 잠금 해제 시 — 실패 카운트/잠금 해제
    public void resetLoginFailures() {
        // 실패 휫수 리셋
        this.failCount = 0;
        // 잠금 해제
        this.lockedUntil = null;
    }
}
