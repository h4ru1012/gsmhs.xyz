package xyz.gsmhs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 서비스에 로그인한 학생 기록. OAuth 콜백 성공 시마다 upsert된다.
 * (관리자 페이지에서 누가 서비스를 쓰는지 파악하는 용도)
 */
@Entity
public class AppUser {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Seoul"));

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    private int grade;
    private int classNum;
    private int number;
    private int studentNumber;
    private String major;

    @Column(nullable = false)
    private Instant firstLoginAt;

    @Column(nullable = false)
    private Instant lastLoginAt;

    @Column(nullable = false)
    private long loginCount;

    protected AppUser() {
    }

    public AppUser(String email, String name, int grade, int classNum, int number,
                   int studentNumber, String major, Instant now) {
        this.email = email;
        this.name = name;
        this.grade = grade;
        this.classNum = classNum;
        this.number = number;
        this.studentNumber = studentNumber;
        this.major = major;
        this.firstLoginAt = now;
        this.lastLoginAt = now;
        this.loginCount = 1;
    }

    /** 재로그인 시 호출 — 학년/반 등은 해가 바뀌면 달라지므로 매번 갱신한다. */
    public void recordLogin(String name, int grade, int classNum, int number,
                            int studentNumber, String major, Instant now) {
        this.name = name;
        this.grade = grade;
        this.classNum = classNum;
        this.number = number;
        this.studentNumber = studentNumber;
        this.major = major;
        this.lastLoginAt = now;
        this.loginCount++;
    }

    public String getDisplayName() {
        return grade + "학년 " + classNum + "반 " + number + "번 " + name;
    }

    public String getFirstLoginAtText() {
        return DATE_FORMAT.format(firstLoginAt);
    }

    public String getLastLoginAtText() {
        return DATE_FORMAT.format(lastLoginAt);
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public int getGrade() { return grade; }
    public int getClassNum() { return classNum; }
    public int getNumber() { return number; }
    public int getStudentNumber() { return studentNumber; }
    public String getMajor() { return major; }
    public Instant getFirstLoginAt() { return firstLoginAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public long getLoginCount() { return loginCount; }
}
