package Backend133.backend_msal.repository;

import Backend133.backend_msal.entity.TipoTramite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoTramiteRepository extends JpaRepository<TipoTramite, String> {

    List<TipoTramite> findByActivoTrue();

    Optional<TipoTramite> findByNombreIgnoreCase(String nombre);
}
