package Backend133.backend_msal;

import Backend133.backend_msal.entity.EstadoTramite;
import Backend133.backend_msal.entity.TipoTramite;
import Backend133.backend_msal.entity.Tramite;
import Backend133.backend_msal.repository.TipoTramiteRepository;
import Backend133.backend_msal.repository.TramiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@DisplayName("Simulación de Producción E2E con Datos Reales")
class SimulacionProduccionE2ETest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TramiteRepository tramiteRepository;

    @Autowired
    private TipoTramiteRepository tipoTramiteRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tramiteRepository.deleteAll();

        if (tipoTramiteRepository.count() == 0) {
            tipoTramiteRepository.saveAll(List.of(
                    new TipoTramite("luminarias", "Reparación de luminarias",
                            "Solicitud de revisión y recambio de alumbrado público dañado.", 2, true),
                    new TipoTramite("retiro", "Retiro de residuos voluminosos",
                            "Solicitud de retiro de escombros, muebles o electrodomésticos en desuso.", 2, true),
                    new TipoTramite("areas-verdes", "Mantención de áreas verdes",
                            "Poda preventiva, riego y corte de pasto en plazas o parques municipales.", 2, true)
            ));
        }
    }

    private String extractId(String json) {
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("ID no encontrado en JSON: " + json);
    }

    @Test
    @DisplayName("Escenario 1: Flujo Completo Vecino -> Creación de Solicitud con Token Azure AD")
    void flujoCreacionVecino() throws Exception {
        String jsonSolicitud = """
                {
                    "tipoId": "luminarias",
                    "asunto": "Luminaria intermitente en Pasaje Los Alerces #432",
                    "descripcion": "El poste 14 frente a la plaza tiene el foco encendiéndose y apagándose de forma continua.",
                    "clienteEmail": "vecino.alerces@barriodigital.cl"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/requests")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("azure-ad-vecino-9876")
                                .claim("name", "Carlos González (Vecino)")
                                .claim("roles", List.of("ROLE_CLIENTE", "Vecino"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSolicitud))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.estado", is("INGRESADO")))
                .andExpect(jsonPath("$.propietarioId", is("azure-ad-vecino-9876")))
                .andExpect(jsonPath("$.propietarioNombre", is("Carlos González (Vecino)")))
                .andExpect(jsonPath("$.asunto", is("Luminaria intermitente en Pasaje Los Alerces #432")))
                .andReturn();

        String tramiteId = extractId(result.getResponse().getContentAsString());

        // Vecino consulta sus solicitudes
        mockMvc.perform(get("/api/requests")
                        .param("propietarioId", "azure-ad-vecino-9876")
                        .with(jwt().jwt(jwt -> jwt.subject("azure-ad-vecino-9876"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(tramiteId)));
    }

    @Test
    @DisplayName("Escenario 2: Ciclo de Vida Funcionario (ADMITIDO -> EN_GESTION -> EN_TERRENO -> RESUELTO)")
    void cicloDeVidaFuncionario() throws Exception {
        // 1. Crear solicitud inicial
        String jsonSolicitud = """
                {
                    "tipoId": "retiro",
                    "asunto": "Retiro de colchón y muebles viejos",
                    "descripcion": "Dejados en la vereda de Av. Principal 1020 para retiro municipal programado.",
                    "propietarioId": "vecina-maria-321",
                    "propietarioNombre": "María Tapia"
                }
                """;

        MvcResult resultCreacion = mockMvc.perform(post("/api/requests")
                        .with(jwt().jwt(jwt -> jwt.subject("vecina-maria-321").claim("name", "María Tapia")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSolicitud))
                .andExpect(status().isCreated())
                .andReturn();

        String id = extractId(resultCreacion.getResponse().getContentAsString());

        // 2. Funcionario ADMITE la solicitud
        mockMvc.perform(put("/api/requests/" + id + "/estado")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("funcionario-admin-01")
                                .claim("name", "Operador Juan Municipal")
                                .claim("roles", List.of("ROLE_OPERADOR", "Funcionario"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ADMITIDO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("ADMITIDO")))
                .andExpect(jsonPath("$.fechaAdmision", notNullValue()));

        // 3. Funcionario pasa a EN_GESTION
        mockMvc.perform(put("/api/requests/" + id + "/estado")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_GESTION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("EN_GESTION")));

        // 4. Cuadrilla pasa a EN_TERRENO
        mockMvc.perform(put("/api/requests/" + id + "/estado")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_TERRENO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("EN_TERRENO")));

        // 5. Finalización: RESUELTO
        mockMvc.perform(put("/api/requests/" + id + "/estado")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"RESUELTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("RESUELTO")));

        // 6. Verificar persistencia real en base de datos
        Tramite persistido = tramiteRepository.findById(id).orElseThrow();
        assertEquals(EstadoTramite.RESUELTO, persistido.getEstado());
        assertNotNull(persistido.getFechaAdmision());
    }

    @Test
    @DisplayName("Escenario 3: Control de Cupo Diario Agotado (Simulación Producción)")
    void controlCupoDiarioAgotado() throws Exception {
        // Asegurar tipo con cupo diario = 2
        tipoTramiteRepository.save(new TipoTramite("poda-urgente", "Poda Urgente", "Poda de árboles en peligro", 2, true));

        // Crear trámite 1 y admitir
        String sol1 = "{\"tipoId\":\"poda-urgente\",\"asunto\":\"Árbol 1\",\"descripcion\":\"Descripción amplia de árbol 1\",\"propietarioId\":\"v1\",\"propietarioNombre\":\"Vecino 1\"}";
        MvcResult r1 = mockMvc.perform(post("/api/requests").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(sol1)).andReturn();
        String id1 = extractId(r1.getResponse().getContentAsString());
        mockMvc.perform(put("/api/requests/" + id1 + "/estado").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ADMITIDO\"}")).andExpect(status().isOk());

        // Crear trámite 2 y admitir (cupo 2/2 alcanzado)
        String sol2 = "{\"tipoId\":\"poda-urgente\",\"asunto\":\"Árbol 2\",\"descripcion\":\"Descripción amplia de árbol 2\",\"propietarioId\":\"v2\",\"propietarioNombre\":\"Vecino 2\"}";
        MvcResult r2 = mockMvc.perform(post("/api/requests").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(sol2)).andReturn();
        String id2 = extractId(r2.getResponse().getContentAsString());
        mockMvc.perform(put("/api/requests/" + id2 + "/estado").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ADMITIDO\"}")).andExpect(status().isOk());

        // Crear trámite 3 e intentar admitir (debe rebotar por cupo agotado -> 409 Conflict)
        String sol3 = "{\"tipoId\":\"poda-urgente\",\"asunto\":\"Árbol 3\",\"descripcion\":\"Descripción amplia de árbol 3\",\"propietarioId\":\"v3\",\"propietarioNombre\":\"Vecino 3\"}";
        MvcResult r3 = mockMvc.perform(post("/api/requests").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(sol3)).andReturn();
        String id3 = extractId(r3.getResponse().getContentAsString());

        mockMvc.perform(put("/api/requests/" + id3 + "/estado").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ADMITIDO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Conflicto en la operación")))
                .andExpect(jsonPath("$.message", containsString("Cupo diario agotado")));
    }

    @Test
    @DisplayName("Escenario 4: Auditoría de Seguridad OAuth2 (Token Ausente o Malformado)")
    void auditoriaSeguridadTokens() throws Exception {
        // Intento anónimo de inyección de datos -> 401
        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoId\":\"luminarias\",\"asunto\":\"Hack\",\"descripcion\":\"Intento sin autenticar\"}"))
                .andExpect(status().isUnauthorized());

        // Consulta anónima de pedidos -> 401
        mockMvc.perform(get("/api/requests"))
                .andExpect(status().isUnauthorized());

        // Endpoint de ordenes con scope correcto
        mockMvc.perform(get("/api/ordenes")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_OT.Create"))))
                .andExpect(status().isOk())
                .andExpect(content().string("API protegida - acceso autorizado con OT.Create"));
    }
}
