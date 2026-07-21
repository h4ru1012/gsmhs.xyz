package xyz.gsmhs.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectTest {

    private Project project(String name, String ownerName, String ownerEmail, boolean visible) {
        return new Project("sub", "user.github.io", name, "설명", null, ownerName, ownerEmail, visible);
    }

    @Test
    void displayTitle은_이름이_없으면_서브도메인으로_대체한다() {
        assertEquals("내 포트폴리오", project("내 포트폴리오", "2학년 3반 4번 홍길동", "a@gsm.hs.kr", true).getDisplayTitle());
        assertEquals("sub", project(null, "2학년 3반 4번 홍길동", "a@gsm.hs.kr", true).getDisplayTitle());
        assertEquals("sub", project("  ", "2학년 3반 4번 홍길동", "a@gsm.hs.kr", true).getDisplayTitle());
    }

    @Test
    void isPublic은_visible이_null이면_공개로_취급한다() {
        Project p = project("이름", "2학년 3반 4번 홍길동", "a@gsm.hs.kr", true);
        p.setVisible(null); // 초기 버전 데이터 호환
        assertTrue(p.isPublic());
        p.setVisible(false);
        assertFalse(p.isPublic());
    }

    @Test
    void isOwnedBy는_이메일_대소문자를_무시한다() {
        Project p = project("이름", "2학년 3반 4번 홍길동", "Student@GSM.hs.kr", true);
        assertTrue(p.isOwnedBy("student@gsm.hs.kr"));
        assertFalse(p.isOwnedBy("other@gsm.hs.kr"));
        assertFalse(p.isOwnedBy(null));
    }

    @Test
    void ownerInitial은_표시명_마지막_어절_이름의_첫_글자다() {
        assertEquals("홍", project("이름", "2학년 3반 4번 홍길동", "a@gsm.hs.kr", true).getOwnerInitial());
        // 공백 없는 이례적 데이터는 첫 글자로 폴백
        assertEquals("홍", project("이름", "홍길동", "a@gsm.hs.kr", true).getOwnerInitial());
        assertEquals("", project("이름", "  ", "a@gsm.hs.kr", true).getOwnerInitial());
    }
}
