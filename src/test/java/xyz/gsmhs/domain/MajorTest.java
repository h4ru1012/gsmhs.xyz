package xyz.gsmhs.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MajorTest {

    @Test
    void 알려진_학과_코드는_한글_학과명으로_변환된다() {
        assertEquals("소프트웨어개발과", Major.labelOf("SW_DEVELOPMENT"));
        assertEquals("스마트IoT과", Major.labelOf("SMART_IOT"));
        assertEquals("인공지능과", Major.labelOf("AI"));
    }

    @Test
    void 알_수_없는_코드는_원본을_그대로_반환한다() {
        assertEquals("UNKNOWN_MAJOR", Major.labelOf("UNKNOWN_MAJOR"));
    }

    @Test
    void null이나_빈_값은_빈_문자열을_반환한다() {
        assertEquals("", Major.labelOf(null));
        assertEquals("", Major.labelOf(""));
        assertEquals("", Major.labelOf("   "));
    }
}
