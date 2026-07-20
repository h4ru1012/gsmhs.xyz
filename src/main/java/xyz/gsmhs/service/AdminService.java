package xyz.gsmhs.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** ADMIN_EMAILS 환경변수(쉼표 구분)에 등록된 이메일을 관리자로 판별한다. */
@Service
public class AdminService {

    private final Set<String> adminEmails;

    public AdminService(@Value("${app.admin-emails:}") String adminEmails) {
        this.adminEmails = Arrays.stream(adminEmails.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAdmin(String email) {
        return email != null && adminEmails.contains(email.toLowerCase());
    }
}
