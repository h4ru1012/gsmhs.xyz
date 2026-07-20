package xyz.gsmhs.domain;

/**
 * 학과 코드(DataGSM SDK Major enum 이름)를 화면 표시용 한글 학과명으로 매핑한다.
 * 전공은 세션/DB에 문자열 코드로 저장되므로, 코드→한글 변환을 한 곳에 모아둔다.
 */
public enum Major {
    SW_DEVELOPMENT("소프트웨어개발과"),
    SMART_IOT("스마트IoT과"),
    AI("인공지능과");

    private final String label;

    Major(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 저장된 전공 코드 문자열을 한글 학과명으로. 알 수 없는 값이면 원본 코드를 그대로 반환. */
    public static String labelOf(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        try {
            return valueOf(code).label;
        } catch (IllegalArgumentException e) {
            return code;
        }
    }
}
