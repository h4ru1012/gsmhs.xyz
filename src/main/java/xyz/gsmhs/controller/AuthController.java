package xyz.gsmhs.controller;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import team.themoment.datagsm.sdk.oauth.DataGsmOAuthClient;
import team.themoment.datagsm.sdk.oauth.exception.DataGsmException;
import team.themoment.datagsm.sdk.oauth.model.AuthorizationUrlBuilder;
import team.themoment.datagsm.sdk.oauth.model.Student;
import team.themoment.datagsm.sdk.oauth.model.TokenResponse;
import team.themoment.datagsm.sdk.oauth.model.UserInfo;
import xyz.gsmhs.config.DataGsmOAuthConfig.DataGsmOAuthProperties;
import xyz.gsmhs.domain.AppUser;
import xyz.gsmhs.dto.SessionUser;
import xyz.gsmhs.repository.AppUserRepository;
import xyz.gsmhs.service.AdminService;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private static final String SESSION_STATE = "oauth_state";
    private static final String SESSION_VERIFIER = "oauth_code_verifier";
    public static final String SESSION_USER = "user";

    private final DataGsmOAuthClient oauthClient;
    private final DataGsmOAuthProperties properties;
    private final AppUserRepository appUserRepository;
    private final AdminService adminService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthController(DataGsmOAuthClient oauthClient, DataGsmOAuthProperties properties,
                          AppUserRepository appUserRepository, AdminService adminService) {
        this.oauthClient = oauthClient;
        this.properties = properties;
        this.appUserRepository = appUserRepository;
        this.adminService = adminService;
    }

    /** 1단계: DataGSM 로그인 페이지로 리다이렉트 */
    @GetMapping("/login")
    public String login(HttpSession session) {
        // CSRF 방지용 state 생성 → 세션 저장
        String state = generateRandomToken();
        session.setAttribute(SESSION_STATE, state);

        // PKCE 활성화한 Authorization URL 생성
        AuthorizationUrlBuilder urlBuilder = oauthClient
                .createAuthorizationUrl(properties.getRedirectUri())
                .state(state)
                .scope(properties.getScope())
                .enablePkce();

        String authorizationUrl = urlBuilder.build();

        // code_verifier는 콜백에서 토큰 교환할 때 필요하므로 세션에 저장
        session.setAttribute(SESSION_VERIFIER, urlBuilder.getCodeVerifier());

        return "redirect:" + authorizationUrl;
    }

    /** 2단계: 콜백 — state 검증 → 토큰 교환 → 유저 정보 조회 → 세션 저장 */
    @GetMapping("/oauth/callback")
    public String callback(@RequestParam("code") String code,
                           @RequestParam("state") String state,
                           HttpSession session) {

        // state 검증 (CSRF 방지)
        String savedState = (String) session.getAttribute(SESSION_STATE);
        session.removeAttribute(SESSION_STATE);
        if (savedState == null || !savedState.equals(state)) {
            log.warn("OAuth state mismatch");
            return "redirect:/?error=state_mismatch";
        }

        String codeVerifier = (String) session.getAttribute(SESSION_VERIFIER);
        session.removeAttribute(SESSION_VERIFIER);

        try {
            // code + PKCE verifier로 토큰 교환
            TokenResponse token = oauthClient.exchangeCodeForToken(
                    code, properties.getRedirectUri(), codeVerifier);

            // access token으로 사용자 정보 조회
            UserInfo userInfo = oauthClient.getUserInfo(token.getAccessToken());

            if (!userInfo.isStudent()) {
                return "redirect:/?error=not_student";
            }

            Student student = userInfo.getStudent();
            String email = userInfo.getEmail();
            SessionUser user = new SessionUser(
                    student.getName(),
                    student.getGrade(),
                    student.getClassNum(),
                    student.getNumber(),
                    student.getStudentNumber(),
                    String.valueOf(student.getMajor()),
                    email,
                    adminService.isAdmin(email)
            );
            session.setAttribute(SESSION_USER, user);
            recordLogin(user);

            return "redirect:/";

        } catch (DataGsmException e) {
            log.error("DataGSM OAuth error: {}", e.getMessage(), e);
            return "redirect:/?error=oauth_failed";
        }
    }

    /** 로그아웃: 세션 파기 */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    /** 관리자 페이지용 사용자 기록 upsert. 실패해도 로그인 흐름은 막지 않는다. */
    private void recordLogin(SessionUser user) {
        try {
            Instant now = Instant.now();
            AppUser appUser = appUserRepository.findByEmailIgnoreCase(user.getEmail()).orElse(null);
            if (appUser == null) {
                appUser = new AppUser(user.getEmail(), user.getName(), user.getGrade(),
                        user.getClassNum(), user.getNumber(), user.getStudentNumber(),
                        user.getMajor(), now);
            } else {
                appUser.recordLogin(user.getName(), user.getGrade(), user.getClassNum(),
                        user.getNumber(), user.getStudentNumber(), user.getMajor(), now);
            }
            appUserRepository.save(appUser);
        } catch (Exception e) {
            log.warn("로그인 기록 저장 실패: {}", e.getMessage());
        }
    }

    private String generateRandomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
