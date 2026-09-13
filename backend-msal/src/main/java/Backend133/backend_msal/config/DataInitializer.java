package Backend133.backend_msal.config;

import Backend133.backend_msal.entity.TipoTramite;
import Backend133.backend_msal.repository.TipoTramiteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(TipoTramiteRepository tipoTramiteRepository) {
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
        };
    }
}
