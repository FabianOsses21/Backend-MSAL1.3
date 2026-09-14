package Backend133.backend_msal;

import Backend133.backend_msal.entity.EstadoTramite;
import Backend133.backend_msal.entity.TipoTramite;
import Backend133.backend_msal.entity.Tramite;
import Backend133.backend_msal.repository.TipoTramiteRepository;
import Backend133.backend_msal.repository.TramiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class TramiteControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TramiteRepository tramiteRepository;

    @Autowired
    private TipoTramiteRepository tipoTramiteRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tramiteRepository.deleteAll();

        if (!tipoTramiteRepository.existsById("luminarias")) {
            tipoTramiteRepository.save(new TipoTramite("luminarias", "Reparación de luminarias", "Desc", 5, true));
        }
    }

    @Test
    void getRequests_DebeRetornarListaVaciaInicial() throws Exception {
        mockMvc.perform(get("/api/requests").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void postRequests_ConPayloadValido_DebeCrearYRetornar201() throws Exception {
        String jsonPayload = """
                {
                    "tipoId": "luminarias",
                    "asunto": "Foco quemado frente al parque",
                    "descripcion": "El poste 4 tiene la bombilla titilando desde anoche."
                }
                """;

        mockMvc.perform(post("/api/requests")
                        .with(jwt().jwt(jwt -> jwt.claim("name", "Axel Moraga").subject("user-uuid-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.asunto", is("Foco quemado frente al parque")))
                .andExpect(jsonPath("$.estado", is("INGRESADO")))
                .andExpect(jsonPath("$.propietarioNombre", is("Axel Moraga")));

        // Verificación directa en base de datos JPA
        org.junit.jupiter.api.Assertions.assertEquals(1, tramiteRepository.count());
        Tramite guardado = tramiteRepository.findAll().get(0);
        org.junit.jupiter.api.Assertions.assertEquals("Foco quemado frente al parque", guardado.getAsunto());
        org.junit.jupiter.api.Assertions.assertEquals("user-uuid-1", guardado.getPropietarioId());
        org.junit.jupiter.api.Assertions.assertEquals(EstadoTramite.INGRESADO, guardado.getEstado());
    }

    @Test
    void postRequests_ConPayloadInvalido_DebeRetornar400YDetalleErrores() throws Exception {
        String jsonInvalido = """
                {
                    "tipoId": "luminarias",
                    "asunto": "Corto",
                    "descripcion": "Breve"
                }
                """;

        mockMvc.perform(post("/api/requests")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Error de validación")))
                .andExpect(jsonPath("$.details", notNullValue()));
    }

    @Test
    void getCatalog_DebeRetornarTiposActivos() throws Exception {
        mockMvc.perform(get("/api/catalog").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", not(empty())));
    }
}
