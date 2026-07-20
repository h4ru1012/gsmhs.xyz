package xyz.gsmhs.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import xyz.gsmhs.domain.AppUser;
import xyz.gsmhs.domain.Project;
import xyz.gsmhs.dto.SessionUser;
import xyz.gsmhs.repository.AppUserRepository;
import xyz.gsmhs.repository.ProjectRepository;
import xyz.gsmhs.service.AdminService;
import xyz.gsmhs.service.CloudflareDnsService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    private final AppUserRepository appUserRepository;
    private final ProjectRepository projectRepository;
    private final AdminService adminService;
    private final CloudflareDnsService dnsService;

    public AdminController(AppUserRepository appUserRepository, ProjectRepository projectRepository,
                           AdminService adminService, CloudflareDnsService dnsService) {
        this.appUserRepository = appUserRepository;
        this.projectRepository = projectRepository;
        this.adminService = adminService;
        this.dnsService = dnsService;
    }

    @GetMapping("/admin")
    public String admin(HttpSession session, Model model) {
        SessionUser user = (SessionUser) session.getAttribute(AuthController.SESSION_USER);
        if (user == null) {
            return "redirect:/login";
        }
        if (!adminService.isAdmin(user.getEmail())) {
            return "redirect:/?error=forbidden";
        }

        List<AppUser> users = appUserRepository.findAllByOrderByLastLoginAtDesc();
        List<Project> projects = projectRepository.findAllByOrderByCreatedAtDesc();

        // 사용자별 등록 프로젝트 수
        Map<String, Long> projectCounts = projects.stream()
                .collect(Collectors.groupingBy(p -> p.getOwnerEmail().toLowerCase(), Collectors.counting()));
        List<AdminUserRow> userRows = users.stream()
                .map(u -> new AdminUserRow(u, projectCounts.getOrDefault(u.getEmail().toLowerCase(), 0L)))
                .toList();

        long publicCount = projects.stream().filter(Project::isPublic).count();

        model.addAttribute("user", user);
        model.addAttribute("userRows", userRows);
        model.addAttribute("projects", projects);
        model.addAttribute("publicCount", publicCount);
        model.addAttribute("privateCount", projects.size() - publicCount);
        model.addAttribute("dnsEnabled", dnsService.isEnabled());
        return "admin";
    }

    public record AdminUserRow(AppUser user, long projectCount) {
    }
}
