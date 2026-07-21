package xyz.gsmhs.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionUserTest {

    private SessionUser user(String name, String major, boolean admin) {
        return new SessionUser(name, 2, 3, 4, 2304, major, "s25041@gsm.hs.kr", admin);
    }

    @Test
    void displayName은_학년_반_번호_이름_형식이다() {
        assertEquals("2학년 3반 4번 홍길동", user("홍길동", "SMART_IOT", false).getDisplayName());
    }

    @Test
    void initial은_이름의_첫_글자다() {
        assertEquals("홍", user("홍길동", "SMART_IOT", false).getInitial());
        assertEquals("", user("", "SMART_IOT", false).getInitial());
        assertEquals("", user(null, "SMART_IOT", false).getInitial());
    }

    @Test
    void majorLabel은_학과_코드를_한글로_변환한다() {
        assertEquals("스마트IoT과", user("홍길동", "SMART_IOT", false).getMajorLabel());
        assertEquals("소프트웨어개발과", user("홍길동", "SW_DEVELOPMENT", false).getMajorLabel());
    }

    @Test
    void admin_플래그를_보관한다() {
        assertTrue(user("홍길동", "AI", true).isAdmin());
        assertFalse(user("홍길동", "AI", false).isAdmin());
    }
}
