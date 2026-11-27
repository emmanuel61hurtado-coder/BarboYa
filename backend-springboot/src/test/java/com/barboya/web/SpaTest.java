package com.barboya.web;

import com.barboya.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.web.resources.static-locations=classpath:/spa-test/")
class SpaTest extends IntegrationTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/ingresar", "/comercios/abc", "/pedidos", "/mi-comercio", "/entregas", "/admin/finanzas"})
    void everyScreenServesTheApplication(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void filesAreServedWithSafeHeaders() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"root\"")))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("script-src 'self';")))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
        mvc.perform(get("/assets/app.js"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"));
    }

    @Test
    void servingTheApplicationDoesNotOpenAnythingElse() throws Exception {
        for (String path : new String[]{"/api/v1/users/me", "/api/v1/admin/users", "/admin/drivers",
                "/application.yml", "/desconocida"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/ingresar")).andExpect(status().isUnauthorized());
        assertThat(mvc.perform(get("/assets/../application.yml")).andReturn().getResponse().getContentAsString())
                .doesNotContain("jwt-secret");
    }
}
