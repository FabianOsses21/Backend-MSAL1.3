package Backend133.backend_msal;

import Backend133.backend_msal.dto.CrearTramiteDto;
import Backend133.backend_msal.entity.EstadoTramite;
import Backend133.backend_msal.entity.TipoTramite;
import Backend133.backend_msal.entity.Tramite;
import Backend133.backend_msal.repository.TipoTramiteRepository;
import Backend133.backend_msal.repository.TramiteRepository;
import Backend133.backend_msal.service.TramiteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TramiteServiceTest {

    @Mock
    private TramiteRepository tramiteRepository;

    @Mock
    private TipoTramiteRepository tipoTramiteRepository;

    private TramiteService tramiteService;

    @BeforeEach
    void setUp() {
        tramiteService = new TramiteService(tramiteRepository, tipoTramiteRepository);
    }

    @Test
    void crearTramite_ConDatosValidos_DebeRetornarTramiteIngresado() {
        TipoTramite tipo = new TipoTramite("luminarias", "Reparación luminarias", "Desc", 5, true);
        when(tipoTramiteRepository.findById("luminarias")).thenReturn(Optional.of(tipo));
        when(tramiteRepository.save(any(Tramite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CrearTramiteDto dto = new CrearTramiteDto("luminarias", "Lámpara apagada", "La lámpara del poste 5 está apagada.");
        Tramite resultado = tramiteService.crearTramite(dto, "user-1", "Axel Moraga");

        assertNotNull(resultado);
        assertEquals(EstadoTramite.INGRESADO, resultado.getEstado());
        assertEquals("Lámpara apagada", resultado.getAsunto());
        assertEquals("user-1", resultado.getPropietarioId());
        verify(tramiteRepository).save(any(Tramite.class));
    }

    @Test
    void crearTramite_ConTipoInactivo_DebeLanzarExcepcion() {
        TipoTramite tipoInactivo = new TipoTramite("retiro", "Retiro escombros", "Desc", 3, false);
        when(tipoTramiteRepository.findById("retiro")).thenReturn(Optional.of(tipoInactivo));

        CrearTramiteDto dto = new CrearTramiteDto("retiro", "Retiro urgente", "Necesito retirar escombros acumulados.");

        assertThrows(IllegalStateException.class, () ->
                tramiteService.crearTramite(dto, "user-1", "Axel Moraga"));
    }

    @Test
    void actualizarEstado_TransicionValida_DebeActualizarEstado() {
        Tramite tramite = new Tramite("uuid-1", "luminarias", "Reparación luminarias", "Asunto", "Desc", "user-1", "Axel", EstadoTramite.INGRESADO);
        when(tramiteRepository.findById("uuid-1")).thenReturn(Optional.of(tramite));
        when(tipoTramiteRepository.findById("luminarias")).thenReturn(Optional.of(new TipoTramite("luminarias", "Luminarias", "Desc", 5, true)));
        when(tramiteRepository.countByTipoIdAndFechaAdmisionBetween(any(), any(), any())).thenReturn(0L);
        when(tramiteRepository.save(any(Tramite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tramite actualizado = tramiteService.actualizarEstado("uuid-1", EstadoTramite.ADMITIDO);

        assertEquals(EstadoTramite.ADMITIDO, actualizado.getEstado());
        assertNotNull(actualizado.getFechaAdmision());
    }

    @Test
    void actualizarEstado_TransicionInvalida_DebeLanzarExcepcion() {
        Tramite tramite = new Tramite("uuid-1", "luminarias", "Reparación luminarias", "Asunto", "Desc", "user-1", "Axel", EstadoTramite.INGRESADO);
        when(tramiteRepository.findById("uuid-1")).thenReturn(Optional.of(tramite));

        assertThrows(IllegalStateException.class, () ->
                tramiteService.actualizarEstado("uuid-1", EstadoTramite.RESUELTO));
    }
}
