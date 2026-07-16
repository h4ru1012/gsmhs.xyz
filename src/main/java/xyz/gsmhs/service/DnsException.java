package xyz.gsmhs.service;

/** Cloudflare DNS API 호출 실패 시 던지는 예외. 사용자에게 보여줄 메시지를 담는다. */
public class DnsException extends RuntimeException {

    public DnsException(String message) {
        super(message);
    }

    public DnsException(String message, Throwable cause) {
        super(message, cause);
    }
}
