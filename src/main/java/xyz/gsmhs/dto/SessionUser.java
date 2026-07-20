package xyz.gsmhs.dto;

import java.io.Serializable;

/**
 * 로그인한 학생 정보를 세션에 담아두는 용도.
 * (access token 전체를 세션에 오래 들고 있지 않기 위해 필요한 필드만 뽑아 저장)
 */
public class SessionUser implements Serializable {

    private final String name;
    private final int grade;
    private final int classNum;
    private final int number;
    private final int studentNumber;
    private final String major;
    private final String email; // 소유자 판별용 DataGSM 계정 이메일
    private final boolean admin; // 로그인 시점에 ADMIN_EMAILS 기준으로 판별

    public SessionUser(String name, int grade, int classNum, int number, int studentNumber,
                       String major, String email, boolean admin) {
        this.name = name;
        this.grade = grade;
        this.classNum = classNum;
        this.number = number;
        this.studentNumber = studentNumber;
        this.major = major;
        this.email = email;
        this.admin = admin;
    }

    public String getName() { return name; }
    public int getGrade() { return grade; }
    public int getClassNum() { return classNum; }
    public int getNumber() { return number; }
    public int getStudentNumber() { return studentNumber; }
    public String getMajor() { return major; }
    public String getEmail() { return email; }
    public boolean isAdmin() { return admin; }

    /** "1학년 2반 3번 홍길동" 형태의 표시용 문자열 */
    public String getDisplayName() {
        return grade + "학년 " + classNum + "반 " + number + "번 " + name;
    }
}
