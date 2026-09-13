package Backend133.backend_msal.service;

import Backend133.backend_msal.dto.CrearTramiteDto;
import Backend133.backend_msal.entity.EstadoTramite;
import Backend133.backend_msal.entity.TipoTramite;
import Backend133.backend_msal.entity.Tramite;
import Backend133.backend_msal.repository.TipoTramiteRepository;
import Backend133.backend_msal.repository.TramiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@Transactional
public class TramiteService {

    private final TramiteRepository tramiteRepository;
    private final TipoTramiteRepository tipoTramiteRepository;

    private static final Map<EstadoTramite, Set<EstadoTramite>> TRANSICIONES_VALIDAS = Map.of(
            EstadoTramite.INGRESADO, Set.of(EstadoTramite.ADMITIDO, EstadoTramite.RECHAZADO),
            EstadoTramite.ADMITIDO, Set.of(EstadoTramite.EN_GESTION, EstadoTramite.RECHAZADO),
            EstadoTramite.EN_GESTION, Set.of(EstadoTramite.EN_TERRENO, EstadoTramite.RECHAZADO),
            EstadoTramite.EN_TERRENO, Set.of(EstadoTramite.RESUELTO, EstadoTramite.RECHAZADO),
            EstadoTramite.RESUELTO, Collections.emptySet(),
            EstadoTramite.RECHAZADO, Collections.emptySet()
    );

    public TramiteService(TramiteRepository tramiteRepository, TipoTramiteRepository tipoTramiteRepository) {
        this.tramiteRepository = tramiteRepository;
        this.tipoTramiteRepository = tipoTramiteRepository;
    }

    @Transactional(readOnly = true)
    public List<Tramite> listarTodos() {
        return tramiteRepository.findAllByOrderByFechaCreacionDesc();
    }

    @Transactional(readOnly = true)
    public List<Tramite> listarPorPropietario(String propietarioId) {
        if (propietarioId == null || propietarioId.isBlank()) {
            return listarTodos();
        }
        return tramiteRepository.findByPropietarioIdOrderByFechaCreacionDesc(propietarioId);
    }

    @Transactional(readOnly = true)
    public Optional<Tramite> obtenerPorId(String id) {
        return tramiteRepository.findById(id);
    }

    public Tramite crearTramite(CrearTramiteDto dto, String propietarioId, String propietarioNombre) {
        TipoTramite tipo = tipoTramiteRepository.findById(dto.getTipoId())
                .orElseThrow(() -> new IllegalArgumentException("El tipo de trámite especificado no existe: " + dto.getTipoId()));

        if (!Boolean.TRUE.equals(tipo.getActivo())) {
            throw new IllegalStateException("El tipo de trámite seleccionado se encuentra inactivo");
        }

        String finalPropietarioId = (dto.getPropietarioId() != null && !dto.getPropietarioId().isBlank())
                ? dto.getPropietarioId()
                : (propietarioId != null && !propietarioId.isBlank() ? propietarioId : UUID.randomUUID().toString());

        String finalPropietarioNombre = (dto.getPropietarioNombre() != null && !dto.getPropietarioNombre().isBlank())
                ? dto.getPropietarioNombre()
                : (propietarioNombre != null && !propietarioNombre.isBlank() ? propietarioNombre : "Usuario");

        Tramite tramite = new Tramite(
                UUID.randomUUID().toString(),
                tipo.getId(),
                tipo.getNombre(),
                dto.getAsunto().trim(),
                dto.getDescripcion().trim(),
                finalPropietarioId,
                finalPropietarioNombre,
                EstadoTramite.INGRESADO
        );

        if (dto.getClienteEmail() != null) {
            tramite.setClienteEmail(dto.getClienteEmail().trim());
        }

        return tramiteRepository.save(tramite);
    }

    public Tramite actualizarEstado(String id, EstadoTramite nuevoEstado) {
        Tramite tramite = tramiteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Trámite con ID " + id + " no encontrado"));

        Set<EstadoTramite> permitidos = TRANSICIONES_VALIDAS.getOrDefault(tramite.getEstado(), Collections.emptySet());
        if (!permitidos.contains(nuevoEstado)) {
            throw new IllegalStateException(String.format("Transición de estado no permitida: de %s a %s",
                    tramite.getEstado(), nuevoEstado));
        }

        LocalDateTime ahora = LocalDateTime.now();
        if (nuevoEstado == EstadoTramite.ADMITIDO) {
            LocalDate hoy = LocalDate.now();
            LocalDateTime inicioDia = hoy.atStartOfDay();
            LocalDateTime finDia = hoy.atTime(LocalTime.MAX);

            TipoTramite tipo = tipoTramiteRepository.findById(tramite.getTipoId())
                    .orElseThrow(() -> new IllegalStateException("Tipo de trámite no encontrado: " + tramite.getTipoId()));

            long admitidosHoy = tramiteRepository.countByTipoIdAndFechaAdmisionBetween(tipo.getId(), inicioDia, finDia);
            if (admitidosHoy >= tipo.getCupoDiario()) {
                throw new IllegalStateException("Cupo diario agotado para el tipo de trámite: " + tipo.getNombre());
            }

            tramite.setFechaAdmision(ahora);
        }

        tramite.setEstado(nuevoEstado);
        tramite.setFechaActualizacion(ahora);

        return tramiteRepository.save(tramite);
    }

    public void eliminarTramite(String id) {
        Tramite tramite = tramiteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Trámite con ID " + id + " no encontrado"));

        if (tramite.getEstado() != EstadoTramite.INGRESADO) {
            throw new IllegalStateException("Solo se pueden eliminar trámites en estado INGRESADO");
        }

        tramiteRepository.delete(tramite);
    }
}
