package xyz.gsmhs.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import xyz.gsmhs.dto.SessionUser;
import xyz.gsmhs.repository.ProjectRepository;

@Controller
public class HomeController {

    private final ProjectRepository projectRepository;

    public HomeController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @GetMapping("/")
    public String home(HttpSession session,
                       @RequestParam(value = "error", required = false) String error,
                       Model model) {

        SessionUser user = (SessionUser) session.getAttribute(AuthController.SESSION_USER);
        model.addAttribute("user", user); // 로그인 안 했으면 null

        // 공개 프로젝트 + 본인이 등록한 비공개 프로젝트만 노출
        model.addAttribute("projects", projectRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(p -> p.isPublic() || (user != null && p.isOwnedBy(user.getEmail())))
                .toList());

        if (error != null) {
            model.addAttribute("errorMessage", switch (error) {
                case "state_mismatch" -> "로그인 요청이 유효하지 않습니다. 다시 시도해 주세요.";
                case "not_student" -> "학생 계정으로만 로그인할 수 있습니다.";
                case "oauth_failed" -> "로그인 중 문제가 발생했습니다. 다시 시도해 주세요.";
                case "not_owner" -> "본인이 등록한 프로젝트만 수정하거나 삭제할 수 있어요.";
                case "forbidden" -> "접근 권한이 없어요.";
                default -> "알 수 없는 오류가 발생했습니다.";
            });
        }

        return "index";
    }
}
