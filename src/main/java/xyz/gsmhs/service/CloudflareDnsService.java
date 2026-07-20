package xyz.gsmhs.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import xyz.gsmhs.config.CloudflareConfig.CloudflareProperties;

import java.util.List;
import java.util.Map;

/**
 * Cloudflare DNS 레코드 관리.
 * 서브도메인 등록/수정/삭제 시 {subdomain}.gsmhs.xyz CNAME 레코드를 함께 관리한다.
 * 토큰이 설정되지 않은 로컬 환경에서는 아무것도 하지 않는다.
 */
@Service
public class CloudflareDnsService {

    private static final Logger log = LoggerFactory.getLogger(CloudflareDnsService.class);

    private final CloudflareProperties properties;
    private final RestClient restClient;

    public CloudflareDnsService(CloudflareProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.cloudflare.com/client/v4")
                .defaultHeader("Authorization", "Bearer " + (properties.getApiToken() == null ? "" : properties.getApiToken()))
                .build();
    }

    public boolean isEnabled() {
        return properties.isConfigured();
    }

    /** CNAME 레코드 생성. 성공 시 Cloudflare 레코드 ID 반환, 미설정 환경이면 null. */
    public String createCname(String subdomain, String targetHost) {
        if (!isEnabled()) {
            log.info("Cloudflare 미설정 — DNS 레코드 생성 생략: {}.{} -> {}",
                    subdomain, properties.getBaseDomain(), targetHost);
            return null;
        }
        CfResponse response = execute(() -> restClient.post()
                .uri("/zones/{zoneId}/dns_records", properties.getZoneId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(recordBody(subdomain, targetHost))
                .retrieve()
                .body(CfResponse.class));
        return response.result().id();
    }

    /** 기존 CNAME 레코드 갱신. 레코드 ID가 없으면(로컬에서 등록된 건) 새로 생성한다. */
    public String updateCname(String recordId, String subdomain, String targetHost) {
        if (!isEnabled()) {
            log.info("Cloudflare 미설정 — DNS 레코드 갱신 생략: {}.{} -> {}",
                    subdomain, properties.getBaseDomain(), targetHost);
            return recordId;
        }
        if (recordId == null || recordId.isBlank()) {
            return createCname(subdomain, targetHost);
        }
        CfResponse response = execute(() -> restClient.put()
                .uri("/zones/{zoneId}/dns_records/{recordId}", properties.getZoneId(), recordId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(recordBody(subdomain, targetHost))
                .retrieve()
                .body(CfResponse.class));
        return response.result().id();
    }

    /** 레코드 삭제. 프로젝트 삭제를 막지 않도록 실패해도 예외를 던지지 않고 로그만 남긴다. */
    public void deleteRecord(String recordId) {
        if (!isEnabled() || recordId == null || recordId.isBlank()) {
            return;
        }
        try {
            execute(() -> restClient.delete()
                    .uri("/zones/{zoneId}/dns_records/{recordId}", properties.getZoneId(), recordId)
                    .retrieve()
                    .body(CfResponse.class));
        } catch (DnsException e) {
            log.warn("Cloudflare 레코드 삭제 실패(무시하고 진행): {}", e.getMessage());
        }
    }

    private Map<String, Object> recordBody(String subdomain, String targetHost) {
        return Map.of(
                "type", "CNAME",
                "name", subdomain + "." + properties.getBaseDomain(),
                "content", targetHost,
                "ttl", 1, // 1 = auto
                "proxied", properties.isProxied());
    }

    private CfResponse execute(CfCall call) {
        CfResponse response;
        try {
            response = call.run();
        } catch (RestClientResponseException e) {
            CfResponse errorBody = null;
            try {
                errorBody = e.getResponseBodyAs(CfResponse.class);
            } catch (Exception ignored) {
            }
            throw new DnsException(firstErrorMessage(errorBody, "Cloudflare API 오류 (HTTP " + e.getStatusCode().value() + ")"), e);
        } catch (Exception e) {
            throw new DnsException("Cloudflare API 호출 실패: " + e.getMessage(), e);
        }
        if (response == null || !response.success()) {
            throw new DnsException(firstErrorMessage(response, "Cloudflare API가 실패를 반환했습니다."));
        }
        return response;
    }

    private String firstErrorMessage(CfResponse response, String fallback) {
        if (response != null && response.errors() != null && !response.errors().isEmpty()) {
            CfError error = response.errors().get(0);
            return error.message() + " (code " + error.code() + ")";
        }
        return fallback;
    }

    @FunctionalInterface
    private interface CfCall {
        CfResponse run();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CfResponse(boolean success, List<CfError> errors, CfResult result) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CfError(int code, String message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CfResult(String id) {
    }
}
