package Backend133.backend_msal.config;

import Backend133.backend_msal.entity.EstadoTramite;
import Backend133.backend_msal.entity.TipoTramite;
import Backend133.backend_msal.entity.Tramite;
import Backend133.backend_msal.repository.TipoTramiteRepository;
import Backend133.backend_msal.repository.TramiteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(
            TipoTramiteRepository tipoTramiteRepository,
            TramiteRepository tramiteRepository
    ) {
        return args -> {
            if (tipoTramiteRepository.count() == 0) {
                tipoTramiteRepository.saveAll(List.of(
                        new TipoTramite("luminarias", "Reparación de luminarias",
                                "Solicitud de revisión y recambio de alumbrado público dañado.", 5, true),
                        new TipoTramite("retiro", "Retiro de residuos voluminosos",
                                "Solicitud de retiro de escombros, muebles o electrodomésticos en desuso.", 3, true),
                        new TipoTramite("areas-verdes", "Mantención de áreas verdes",
                                "Poda preventiva, riego y corte de pasto en plazas o parques municipales.", 4, true)
                ));
            }

            if (tramiteRepository.count() == 0) {
                tramiteRepository.saveAll(List.of(
                        new Tramite(
                                "solicitud-demo-001",
                                "luminarias",
                                "Reparación de luminarias",
                                "Luminaria intermitente en Pasaje Los Alerces #432",
                                "El foco del poste 14 frente a la plaza parpadea continuamente durante la noche.",
                                "usuario-vecino-demo",
                                "Carlos González (Vecino)",
                                EstadoTramite.INGRESADO
                        ),
                        new Tramite(
                                "solicitud-demo-002",
                                "retiro",
                                "Retiro de residuos voluminosos",
                                "Retiro de colchón y muebles en desuso",
                                "Muebles viejos coordinados para retiro municipal en vereda exterior.",
                                "usuario-vecino-demo",
                                "Carlos González (Vecino)",
                                EstadoTramite.ADMITIDO
                        ),
                        new Tramite(
                                "solicitud-demo-003",
                                "areas-verdes",
                                "Mantención de áreas verdes",
                                "Poda de ramas bajas en Plaza Central",
                                "Ramas de sauce bloquean la visibilidad del sendero peatonal.",
                                "usuario-admin-demo",
                                "Admin Municipal",
                                EstadoTramite.EN_GESTION
                        )
                ));
            }
        };
    }
}
