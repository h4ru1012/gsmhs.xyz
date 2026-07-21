package xyz.gsmhs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 애플리케이션 부팅 + 주요 페이지 렌더링 스모크 테스트.
 * 실제 시크릿 없이 돌 수 있도록 더미 OAuth 값과 테스트용 SQLite 파일을 주입한다.
 */
@SpringBootTest(properties = {
        "DATAGSM_CLIENT_ID=test-client-id",
        "DATAGSM_CLIENT_SECRET=test-client-secret",
        "spring.datasource.url=jdbc:sqlite:build/test-gsmhs.db"
})
@AutoConfigureMockMvc
class GsmhsApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 컨텍스트가_부팅된다() {
        // @SpringBootTest 로딩 자체가 검증 대상 (설정·빈 배선·JPA 매핑)
    }

    @Test
    void 홈_페이지가_렌더링된다() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("GSMHS")));
    }

    @Test
    void 이용약관_페이지가_렌더링된다() throws Exception {
        mockMvc.perform(get("/tos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이용약관")));
    }

    @Test
    void 개인정보_페이지가_렌더링된다() throws Exception {
        mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("개인정보")));
    }

    @Test
    void 비로그인_등록_폼은_로그인으로_리다이렉트된다() throws Exception {
        mockMvc.perform(get("/projects/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void 비로그인_관리자_페이지는_로그인으로_리다이렉트된다() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void 로그아웃은_POST만_허용된다() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void robots와_favicon이_서빙된다() throws Exception {
        mockMvc.perform(get("/robots.txt")).andExpect(status().isOk());
        mockMvc.perform(get("/favicon.svg")).andExpect(status().isOk());
    }
}
