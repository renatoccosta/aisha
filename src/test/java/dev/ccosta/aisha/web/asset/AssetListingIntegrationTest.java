package dev.ccosta.aisha.web.asset;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.ccosta.aisha.domain.asset.AssetRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = {
    "GOOGLE_CLIENT_ID=change-me-google-client-id",
    "GOOGLE_CLIENT_SECRET=change-me-google-client-secret"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AssetListingIntegrationTest {

    private final AssetRepository assetRepository;
    private final WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @Autowired
    AssetListingIntegrationTest(AssetRepository assetRepository, WebApplicationContext webApplicationContext) {
        this.assetRepository = assetRepository;
        this.webApplicationContext = webApplicationContext;
    }

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }

    @Test
    void shouldKeepRemainingAssetsVisibleAfterHtmxDelete() throws Exception {
        Long bitcoinId = assetRepository.findByTickerIgnoreCase("BTC").orElseThrow().getId();
        HttpSession session = loginAndGetSession();

        mockMvc.perform(post("/investments/assets/{id}/delete", bitcoinId)
                .session((MockHttpSession) session)
                .with(csrf())
                .header("HX-Request", "true")
                .param("page", "0")
                .param("size", "25"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Fundo XPTO Multimercado")))
            .andExpect(content().string(not(containsString("Bitcoin"))))
            .andExpect(content().string(not(containsString("Nenhum ativo cadastrado."))));
    }

    @Test
    void shouldShowUnfilteredAssetsAfterHtmxDeleteEmptiesCurrentFilter() throws Exception {
        Long bitcoinId = assetRepository.findByTickerIgnoreCase("BTC").orElseThrow().getId();
        HttpSession session = loginAndGetSession();

        mockMvc.perform(post("/investments/assets/{id}/delete", bitcoinId)
                .session((MockHttpSession) session)
                .with(csrf())
                .header("HX-Request", "true")
                .param("description", "Bitcoin")
                .param("page", "0")
                .param("size", "25"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Fundo XPTO Multimercado")))
            .andExpect(content().string(not(containsString("Bitcoin"))))
            .andExpect(content().string(not(containsString("Nenhum ativo cadastrado."))));
    }

    private HttpSession loginAndGetSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                .with(csrf())
                .param("username", "admin")
                .param("password", "admin"))
            .andReturn();
        return result.getRequest().getSession(false);
    }
}
