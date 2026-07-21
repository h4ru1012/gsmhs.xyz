package xyz.gsmhs.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminServiceTest {

    @Test
    void 쉼표로_구분된_목록에서_관리자를_판별한다() {
        AdminService service = new AdminService("admin@gsm.hs.kr, second@gsm.hs.kr");
        assertTrue(service.isAdmin("admin@gsm.hs.kr"));
        assertTrue(service.isAdmin("second@gsm.hs.kr"));
        assertFalse(service.isAdmin("student@gsm.hs.kr"));
    }

    @Test
    void 이메일_대소문자를_무시한다() {
        AdminService service = new AdminService("Admin@GSM.hs.kr");
        assertTrue(service.isAdmin("admin@gsm.hs.kr"));
        assertTrue(service.isAdmin("ADMIN@GSM.HS.KR"));
    }

    @Test
    void 빈_설정이면_아무도_관리자가_아니다() {
        AdminService service = new AdminService("");
        assertFalse(service.isAdmin("admin@gsm.hs.kr"));
        assertFalse(service.isAdmin(null));
    }
}
