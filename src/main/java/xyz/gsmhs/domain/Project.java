package xyz.gsmhs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String subdomain;

    /** CNAME 대상 호스트 (예: username.github.io) */
    @Column(nullable = false)
    private String cnameTarget;

    /** 허브에 표시할 프로젝트 이름. (초기 버전 데이터는 null일 수 있어 표시 시 서브도메인으로 대체) */
    private String name;

    /** 소개는 선택 입력. 컬럼이 NOT NULL로 이미 생성돼 있어 빈 값은 ""로 저장한다. */
    @Column(nullable = false)
    private String description;

    /** 허브 목록 공개 여부. null이면 공개로 취급 (초기 버전 데이터 호환). */
    private Boolean visible;

    private String githubUrl;

    /** 표시용 이름 (예: 2학년 3반 4번 홍길동) */
    @Column(nullable = false)
    private String ownerName;

    /** 소유자 판별용 DataGSM 계정 이메일 */
    @Column(nullable = false)
    private String ownerEmail;

    /** Cloudflare DNS 레코드 ID (미연동 환경에서 등록된 경우 null) */
    private String cfRecordId;

    @Column(nullable = false)
    private Instant createdAt;

    protected Project() {
    }

    public Project(String subdomain, String cnameTarget, String name, String description,
                   String githubUrl, String ownerName, String ownerEmail, boolean visible) {
        this.subdomain = subdomain;
        this.cnameTarget = cnameTarget;
        this.name = name;
        this.description = description;
        this.githubUrl = githubUrl;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
        this.visible = visible;
        this.createdAt = Instant.now();
    }

    public boolean isOwnedBy(String email) {
        return ownerEmail != null && ownerEmail.equalsIgnoreCase(email);
    }

    /** 허브 목록에 공개할지 여부 (초기 버전 데이터는 visible이 null이라 공개로 취급) */
    public boolean isPublic() {
        return visible == null || visible;
    }

    /** 표시용 이름 — 이름이 없던 초기 데이터는 서브도메인으로 대체 */
    public String getDisplayTitle() {
        return name == null || name.isBlank() ? subdomain : name;
    }

    public Long getId() { return id; }
    public String getSubdomain() { return subdomain; }
    public String getCnameTarget() { return cnameTarget; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getGithubUrl() { return githubUrl; }
    public String getOwnerName() { return ownerName; }
    public String getOwnerEmail() { return ownerEmail; }
    public String getCfRecordId() { return cfRecordId; }
    public Instant getCreatedAt() { return createdAt; }

    public void setSubdomain(String subdomain) { this.subdomain = subdomain; }
    public void setCnameTarget(String cnameTarget) { this.cnameTarget = cnameTarget; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }
    public void setVisible(Boolean visible) { this.visible = visible; }
    public void setCfRecordId(String cfRecordId) { this.cfRecordId = cfRecordId; }
}
