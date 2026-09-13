package Backend133.backend_msal.repository;

import Backend133.backend_msal.entity.EstadoTramite;
import Backend133.backend_msal.entity.Tramite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TramiteRepository extends JpaRepository<Tramite, String> {

    List<Tramite> findAllByOrderByFechaCreacionDesc();

    List<Tramite> findByPropietarioIdOrderByFechaCreacionDesc(String propietarioId);

    List<Tramite> findByEstado(EstadoTramite estado);

    long countByTipoIdAndFechaAdmisionBetween(String tipoId, LocalDateTime inicio, LocalDateTime fin);

    long countByTipoId(String tipoId);
}
