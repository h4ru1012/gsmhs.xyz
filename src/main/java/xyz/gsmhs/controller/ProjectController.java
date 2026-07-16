package xyz.gsmhs.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import xyz.gsmhs.domain.Project;
import xyz.gsmhs.dto.SessionUser;
import xyz.gsmhs.repository.ProjectRepository;
import xyz.gsmhs.service.CloudflareDnsService;
import xyz.gsmhs.service.DnsException;

import java.util.Set;
import java.util.regex.Pattern;

@Controller
public class ProjectController {

    private static final Pattern SUBDOMAIN_PATTERN = Pattern.compile("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$");
    private static final Pattern HOST_PATTERN =
            Pattern.compile("^([a-z0-9]([a-z0-9-]*[a-z0-9])?\\.)+[a-z]{2,}$");
    private static final Set<String> RESERVED_SUBDOMAINS = Set.of(
            "www", "oauth", "api", "admin", "login", "logout", "app", "static", "mail");

    private final ProjectRepository projectRepository;
    private final CloudflareDnsService dnsService;

    public ProjectController(ProjectRepository projectRepository, CloudflareDnsService dnsService) {
        this.projectRepository = projectRepository;
        this.dnsService = dnsService;
    }

    /* ---------- 신청 ---------- */

    @GetMapping("/projects/new")
    public String newProjectForm(HttpSession session, Model model) {
        if (currentUser(session) == null) {
            return "redirect:/login";
        }
        prepareForm(model, new ProjectForm(), "/projects", "서브도메인 신청", "신청하기");
        return "register";
    }

    @PostMapping("/projects")
    public String createProject(@RequestParam String subdomain,
                                @RequestParam String cnameTarget,
                                @RequestParam String name,
                                @RequestParam(required = false) String description,
                                @RequestParam(required = false) String githubUrl,
                                @RequestParam(defaultValue = "true") boolean visible,
                                HttpSession session,
                                Model model) {

        SessionUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        String normalizedSubdomain = normalizeSubdomain(subdomain);
        String normalizedHost = normalizeHost(cnameTarget);
        ProjectForm form = new ProjectForm(normalizedSubdomain, normalizedHost, name, description, githubUrl, visible);

        String errorMessage = validate(normalizedSubdomain, normalizedHost, name, null);
        if (errorMessage != null) {
            prepareForm(model, form, "/projects", "서브도메인 신청", "신청하기");
            model.addAttribute("errorMessage", errorMessage);
            return "register";
        }

        String recordId;
        try {
            recordId = dnsService.createCname(normalizedSubdomain, normalizedHost);
        } catch (DnsException e) {
            prepareForm(model, form, "/projects", "서브도메인 신청", "신청하기");
            model.addAttribute("errorMessage", "DNS 레코드 생성에 실패했어요: " + e.getMessage());
            return "register";
        }

        Project project = new Project(normalizedSubdomain, normalizedHost, name.trim(),
                blankToEmpty(description), blankToNull(githubUrl),
                user.getDisplayName(), user.getEmail(), visible);
        project.setCfRecordId(recordId);
        projectRepository.save(project);

        return "redirect:/";
    }

    /* ---------- 수정 ---------- */

    @GetMapping("/projects/{id}/edit")
    public String editProjectForm(@PathVariable Long id, HttpSession session, Model model) {
        SessionUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        Project project = projectRepository.findById(id).orElse(null);
        if (project == null || !project.isOwnedBy(user.getEmail())) {
            return "redirect:/?error=not_owner";
        }
        ProjectForm form = new ProjectForm(project.getSubdomain(), project.getCnameTarget(),
                project.getName(), project.getDescription(), project.getGithubUrl(), project.isPublic());
        prepareForm(model, form, "/projects/" + id + "/edit", "프로젝트 수정", "수정하기");
        return "register";
    }

    @PostMapping("/projects/{id}/edit")
    public String updateProject(@PathVariable Long id,
                                @RequestParam String subdomain,
                                @RequestParam String cnameTarget,
                                @RequestParam String name,
                                @RequestParam(required = false) String description,
                                @RequestParam(required = false) String githubUrl,
                                @RequestParam(defaultValue = "true") boolean visible,
                                HttpSession session,
                                Model model) {

        SessionUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        Project project = projectRepository.findById(id).orElse(null);
        if (project == null || !project.isOwnedBy(user.getEmail())) {
            return "redirect:/?error=not_owner";
        }

        String normalizedSubdomain = normalizeSubdomain(subdomain);
        String normalizedHost = normalizeHost(cnameTarget);
        ProjectForm form = new ProjectForm(normalizedSubdomain, normalizedHost, name, description, githubUrl, visible);
        String formAction = "/projects/" + id + "/edit";

        String errorMessage = validate(normalizedSubdomain, normalizedHost, name, id);
        if (errorMessage != null) {
            prepareForm(model, form, formAction, "프로젝트 수정", "수정하기");
            model.addAttribute("errorMessage", errorMessage);
            return "register";
        }

        boolean dnsChanged = !normalizedSubdomain.equals(project.getSubdomain())
                || !normalizedHost.equals(project.getCnameTarget())
                || project.getCfRecordId() == null;
        if (dnsChanged) {
            try {
                String recordId = dnsService.updateCname(
                        project.getCfRecordId(), normalizedSubdomain, normalizedHost);
                project.setCfRecordId(recordId);
            } catch (DnsException e) {
                prepareForm(model, form, formAction, "프로젝트 수정", "수정하기");
                model.addAttribute("errorMessage", "DNS 레코드 갱신에 실패했어요: " + e.getMessage());
                return "register";
            }
        }

        project.setSubdomain(normalizedSubdomain);
        project.setCnameTarget(normalizedHost);
        project.setName(name.trim());
        project.setDescription(blankToEmpty(description));
        project.setGithubUrl(blankToNull(githubUrl));
        project.setVisible(visible);
        projectRepository.save(project);

        return "redirect:/";
    }

    /* ---------- 삭제 ---------- */

    @PostMapping("/projects/{id}/delete")
    public String deleteProject(@PathVariable Long id, HttpSession session) {
        SessionUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        Project project = projectRepository.findById(id).orElse(null);
        if (project == null || !project.isOwnedBy(user.getEmail())) {
            return "redirect:/?error=not_owner";
        }

        dnsService.deleteRecord(project.getCfRecordId());
        projectRepository.delete(project);

        return "redirect:/";
    }

    /* ---------- 헬퍼 ---------- */

    private SessionUser currentUser(HttpSession session) {
        return (SessionUser) session.getAttribute(AuthController.SESSION_USER);
    }

    private void prepareForm(Model model, ProjectForm form, String action, String title, String submitLabel) {
        model.addAttribute("project", form);
        model.addAttribute("formAction", action);
        model.addAttribute("pageTitle", title);
        model.addAttribute("submitLabel", submitLabel);
    }

    private String normalizeSubdomain(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase();
    }

    /** 사용자가 https://... 형태로 붙여넣어도 호스트명만 추출한다. */
    private String normalizeHost(String raw) {
        if (raw == null) {
            return "";
        }
        String host = raw.trim().toLowerCase();
        host = host.replaceFirst("^https?://", "");
        int slash = host.indexOf('/');
        if (slash >= 0) {
            host = host.substring(0, slash);
        }
        return host;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** description 컬럼은 NOT NULL이라 선택 입력이 비면 ""로 저장 */
    private String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    private String validate(String subdomain, String host, String name, Long selfId) {
        if (subdomain.isEmpty() || !SUBDOMAIN_PATTERN.matcher(subdomain).matches()) {
            return "서브도메인은 영문 소문자, 숫자, 하이픈(-)만 사용할 수 있어요.";
        }
        if (RESERVED_SUBDOMAINS.contains(subdomain)) {
            return "이 서브도메인은 예약되어 있어 사용할 수 없어요.";
        }
        if (host.isEmpty() || !HOST_PATTERN.matcher(host).matches()) {
            return "연결할 호스트는 username.github.io 같은 도메인 형식이어야 해요.";
        }
        if (name == null || name.isBlank()) {
            return "프로젝트 이름을 입력해 주세요.";
        }
        if (name.trim().length() > 50) {
            return "프로젝트 이름은 50자 이내로 입력해 주세요.";
        }
        boolean duplicated = selfId == null
                ? projectRepository.existsBySubdomainIgnoreCase(subdomain)
                : projectRepository.existsBySubdomainIgnoreCaseAndIdNot(subdomain, selfId);
        if (duplicated) {
            return "이미 사용 중인 서브도메인이에요.";
        }
        return null;
    }

    public static class ProjectForm {
        private String subdomain;
        private String cnameTarget;
        private String name;
        private String description;
        private String githubUrl;
        private boolean visible = true;

        public ProjectForm() {
        }

        public ProjectForm(String subdomain, String cnameTarget, String name, String description,
                           String githubUrl, boolean visible) {
            this.subdomain = subdomain;
            this.cnameTarget = cnameTarget;
            this.name = name;
            this.description = description;
            this.githubUrl = githubUrl;
            this.visible = visible;
        }

        public String getSubdomain() { return subdomain; }
        public String getCnameTarget() { return cnameTarget; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getGithubUrl() { return githubUrl; }
        public boolean isVisible() { return visible; }
    }
}
