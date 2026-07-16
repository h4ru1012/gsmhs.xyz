package xyz.gsmhs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.themoment.datagsm.sdk.oauth.DataGsmOAuthClient;

/**
 * DataGSM OAuth SDK 클라이언트를 Spring Bean으로 등록.
 * 설정값은 application.yml의 datagsm.oauth.* 에서 읽어온다.
 * (client-id/secret은 환경변수 DATAGSM_CLIENT_ID / DATAGSM_CLIENT_SECRET로 주입)
 */
@Configuration
public class DataGsmOAuthConfig {

    @Bean
    @ConfigurationProperties(prefix = "datagsm.oauth")
    public DataGsmOAuthProperties dataGsmOAuthProperties() {
        return new DataGsmOAuthProperties();
    }

    @Bean
    public DataGsmOAuthClient dataGsmOAuthClient(DataGsmOAuthProperties properties) {
        return DataGsmOAuthClient.builder(properties.getClientId(), properties.getClientSecret())
                .authorizationBaseUrl(properties.getAuthorizationBaseUrl())
                .userInfoBaseUrl(properties.getUserInfoBaseUrl())
                .build();
    }

    public static class DataGsmOAuthProperties {
        private String clientId;
        private String clientSecret;
        private String authorizationBaseUrl;
        private String userInfoBaseUrl;
        private String redirectUri;
        private String scope;

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getAuthorizationBaseUrl() { return authorizationBaseUrl; }
        public void setAuthorizationBaseUrl(String url) { this.authorizationBaseUrl = url; }
        public String getUserInfoBaseUrl() { return userInfoBaseUrl; }
        public void setUserInfoBaseUrl(String url) { this.userInfoBaseUrl = url; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
    }
}
