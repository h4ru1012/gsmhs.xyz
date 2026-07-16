package xyz.gsmhs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cloudflare DNS 연동 설정.
 * api-token/zone-id가 비어 있으면 DNS 연동 없이 동작한다(로컬 개발용).
 */
@Configuration
public class CloudflareConfig {

    @Bean
    @ConfigurationProperties(prefix = "cloudflare")
    public CloudflareProperties cloudflareProperties() {
        return new CloudflareProperties();
    }

    public static class CloudflareProperties {
        private String apiToken;
        private String zoneId;
        private String baseDomain = "gsmhs.xyz";
        private boolean proxied = false;

        public boolean isConfigured() {
            return apiToken != null && !apiToken.isBlank()
                    && zoneId != null && !zoneId.isBlank();
        }

        public String getApiToken() { return apiToken; }
        public void setApiToken(String apiToken) { this.apiToken = apiToken; }
        public String getZoneId() { return zoneId; }
        public void setZoneId(String zoneId) { this.zoneId = zoneId; }
        public String getBaseDomain() { return baseDomain; }
        public void setBaseDomain(String baseDomain) { this.baseDomain = baseDomain; }
        public boolean isProxied() { return proxied; }
        public void setProxied(boolean proxied) { this.proxied = proxied; }
    }
}
