package dev.ccosta.aisha.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class SecurityIntegrationTest {

    private MockMvc mockMvc;
    private final WebApplicationContext webApplicationContext;

    SecurityIntegrationTest(WebApplicationContext webApplicationContext) {
        this.webApplicationContext = webApplicationContext;
    }

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }

    @Test
    void shouldRedirectProtectedRouteToLoginWhenAnonymous() throws Exception {
        mockMvc.perform(get("/dashboard"))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void shouldRenderAdminPageWhenAuthenticated() throws Exception {
        HttpSession session = loginAndGetSession();

        mockMvc.perform(get("/admin").session((MockHttpSession) session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Administração de Modelos")));
    }

    @Test
    void shouldRejectProtectedPostWithoutCsrf() throws Exception {
        HttpSession session = loginAndGetSession();

        mockMvc.perform(post("/date-filter")
                .session((MockHttpSession) session)
                .param("action", "SET_MONTH")
                .param("redirectTo", "/dashboard"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldAcceptProtectedPostWithCsrf() throws Exception {
        HttpSession session = loginAndGetSession();

        mockMvc.perform(post("/date-filter")
                .session((MockHttpSession) session)
                .with(csrf())
                .param("action", "SET_MONTH")
                .param("redirectTo", "/dashboard"))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void shouldAllowRetrainRequestFromAdminPageWithCsrf() throws Exception {
        HttpSession session = loginAndGetSession();

        mockMvc.perform(post("/admin/category-model/retrain")
                .session((MockHttpSession) session)
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Retreino solicitado")));
    }

    @Test
    void shouldRegenerateSessionIdAfterLogin() throws Exception {
        MockHttpSession unauthenticatedSession = new MockHttpSession();
        String previousSessionId = unauthenticatedSession.getId();

        MvcResult result = mockMvc.perform(post("/login")
                .session(unauthenticatedSession)
                .with(csrf())
                .param("username", "admin")
                .param("password", "admin"))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/dashboard"))
            .andReturn();

        MockHttpSession authenticatedSession = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(authenticatedSession).isNotNull();
        assertThat(authenticatedSession.getId()).isNotEqualTo(previousSessionId);
    }

    @Test
    void shouldInvalidateSessionOnLogout() throws Exception {
        MockHttpSession session = (MockHttpSession) loginAndGetSession();

        mockMvc.perform(post("/logout")
                .session(session)
                .with(csrf()))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/login?logout"));

        mockMvc.perform(get("/dashboard").session(session))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/login"));
    }

    private HttpSession loginAndGetSession() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/login")
                .with(csrf())
                .param("username", "admin")
                .param("password", "admin"))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/dashboard"))
            .andReturn();

        return loginResult.getRequest().getSession(false);
    }
}
