package Backend133.backend_msal.service;

import Backend133.backend_msal.entity.TipoTramite;
import Backend133.backend_msal.repository.TipoTramiteRepository;
import Backend133.backend_msal.repository.TramiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CatalogoService {

    private final TipoTramiteRepository tipoTramiteRepository;
    private final TramiteRepository tramiteRepository;

    public CatalogoService(TipoTramiteRepository tipoTramiteRepository, TramiteRepository tramiteRepository) {
        this.tipoTramiteRepository = tipoTramiteRepository;
        this.tramiteRepository = tramiteRepository;
    }

    @Transactional(readOnly = true)
    public List<TipoTramite> listarTodos() {
        return tipoTramiteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<TipoTramite> listarActivos() {
        return tipoTramiteRepository.findByActivoTrue();
    }

    @Transactional(readOnly = true)
    public Optional<TipoTramite> obtenerPorId(String id) {
        return tipoTramiteRepository.findById(id);
    }

    public TipoTramite guardarTipo(TipoTramite tipo) {
        if (tipo.getId() == null || tipo.getId().isBlank()) {
            tipo.setId(UUID.randomUUID().toString());
        }

        tipoTramiteRepository.findByNombreIgnoreCase(tipo.getNombre())
                .ifPresent(existente -> {
                    if (!existente.getId().equals(tipo.getId())) {
                        throw new IllegalArgumentException("Ya existe un tipo de trámite con el nombre: " + tipo.getNombre());
                    }
                });

        return tipoTramiteRepository.save(tipo);
    }

    public void eliminarTipo(String id) {
        TipoTramite tipo = tipoTramiteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Tipo de trámite con ID " + id + " no encontrado"));

        long tramitesAsociados = tramiteRepository.countByTipoId(id);
        if (tramitesAsociados > 0) {
            throw new IllegalStateException("No se puede eliminar el tipo de trámite porque tiene solicitudes asociadas. Desactívelo en su lugar.");
        }

        tipoTramiteRepository.delete(tipo);
    }
}
