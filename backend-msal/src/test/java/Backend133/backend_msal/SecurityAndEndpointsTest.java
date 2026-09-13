package Backend133.backend_msal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SecurityAndEndpointsTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void endpointPublico_DebeRetornar200SinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/public"))
                .andExpect(status().isOk())
                .andExpect(content().string("API funcionando correctamente"));
    }

    @Test
    void endpointOrdenes_SinToken_DebeRetornar401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/ordenes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointOrdenes_ConScopeCorrecto_DebeRetornar200Ok() throws Exception {
        mockMvc.perform(get("/api/ordenes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_OT.Create"))))
                .andExpect(status().isOk())
                .andExpect(content().string("API protegida - acceso autorizado con OT.Create"));
    }

    @Test
    void endpointOrdenes_ConScopeIncorrecto_DebeRetornar403Forbidden() throws Exception {
        mockMvc.perform(get("/api/ordenes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_OT.Read"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void configuracionCORS_DebePermitirOrigenAngular() throws Exception {
        mockMvc.perform(options("/api/ordenes")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }
}
