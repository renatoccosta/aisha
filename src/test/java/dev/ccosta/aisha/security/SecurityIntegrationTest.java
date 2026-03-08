package dev.ccosta.aisha.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

@SpringBootTest(properties = {
    "GOOGLE_CLIENT_ID=change-me-google-client-id",
    "GOOGLE_CLIENT_SECRET=change-me-google-client-secret"
})
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
    void shouldAllowAnonymousAccessToPrivacyPolicy() throws Exception {
        mockMvc.perform(get("/privacy-policy"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Voltar para login")));
    }

    @Test
    void shouldAllowAnonymousAccessToTermsOfUse() throws Exception {
        mockMvc.perform(get("/terms-of-use"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Voltar para login")));
    }

    @Test
    void shouldRenderPortuguesePrivacyPolicyWhenLocaleIsPtBr() throws Exception {
        mockMvc.perform(get("/privacy-policy").header("Accept-Language", "pt-BR"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Escopo desta")));
    }

    @Test
    void shouldRenderEnglishPrivacyPolicyWhenLocaleIsNotPtBr() throws Exception {
        mockMvc.perform(get("/privacy-policy").header("Accept-Language", "en-US"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Scope of this policy")));
    }

    @Test
    void shouldRenderPortugueseTermsOfUseWhenLocaleIsPtBr() throws Exception {
        mockMvc.perform(get("/terms-of-use").header("Accept-Language", "pt-BR"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Sobre a AI$HA e estes termos")));
    }

    @Test
    void shouldRenderEnglishTermsOfUseWhenLocaleIsNotPtBr() throws Exception {
        mockMvc.perform(get("/terms-of-use").header("Accept-Language", "en-US"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("About AI$HA and these terms")));
    }

    @Test
    void shouldHideGoogleLoginOptionWhenClientCredentialsArePlaceholders() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/privacy-policy")))
            .andExpect(content().string(containsString("/terms-of-use")))
            .andExpect(content().string(not(containsString("Entrar com o Google"))))
            .andExpect(content().string(not(containsString("/oauth2/authorization/google"))));
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
    void shouldGenerateCorrelationIdHeaderWhenMissing() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void shouldPropagateProvidedCorrelationIdHeader() throws Exception {
        mockMvc.perform(get("/login").header("X-Correlation-Id", "test-correlation-id"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Correlation-Id", "test-correlation-id"));
    }

    @Test
    void shouldAllowRetrainRequestFromAdminPageWithCsrf() throws Exception {
        HttpSession session = loginAndGetSession();

        mockMvc.perform(post("/admin/category-model/retrain")
                .session((MockHttpSession) session)
                .with(csrf()))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/admin?manualTrainingRequested=true"));
    }

    @Test
    void shouldRenderInternalErrorPageWhenDebugErrorEndpointIsCalledAnonymously() throws Exception {
        mockMvc.perform(get("/debug/force-error"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string(containsString("Erro interno")))
            .andExpect(content().string(containsString("ID de correlação:")));
    }

    @Test
    void shouldRenderInternalErrorPageWhenDebugErrorEndpointIsCalled() throws Exception {
        HttpSession session = loginAndGetSession();

        mockMvc.perform(get("/debug/force-error").session((MockHttpSession) session))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string(containsString("Erro interno")))
            .andExpect(content().string(containsString("ID de correlação:")));
    }

    @Test
    void shouldRenderNotFoundPageWhenDebugNotFoundEndpointIsCalledAnonymously() throws Exception {
        mockMvc.perform(get("/debug/force-not-found"))
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("Recurso não encontrado")));
    }

    @Test
    void shouldRenderNotFoundPageWhenDebugNotFoundEndpointIsCalled() throws Exception {
        HttpSession session = loginAndGetSession();

        mockMvc.perform(get("/debug/force-not-found").session((MockHttpSession) session))
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("Recurso não encontrado")));
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
