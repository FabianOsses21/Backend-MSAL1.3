package Backend133.backend_msal.controller;

import Backend133.backend_msal.dto.ActualizarEstadoDto;
import Backend133.backend_msal.dto.CrearTramiteDto;
import Backend133.backend_msal.entity.Tramite;
import Backend133.backend_msal.service.TramiteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/requests", "/api/orders", "/api/pedidos"})
public class TramiteController {

    private final TramiteService tramiteService;

    public TramiteController(TramiteService tramiteService) {
        this.tramiteService = tramiteService;
    }

    @GetMapping
    public ResponseEntity<List<Tramite>> listar(@RequestParam(required = false) String propietarioId) {
        List<Tramite> lista = (propietarioId != null && !propietarioId.isBlank())
                ? tramiteService.listarPorPropietario(propietarioId)
                : tramiteService.listarTodos();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tramite> obtenerPorId(@PathVariable String id) {
        return tramiteService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Tramite> crear(
            @Valid @RequestBody CrearTramiteDto dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String propietarioId = (jwt != null) ? jwt.getSubject() : dto.getPropietarioId();
        String propietarioNombre = (jwt != null && jwt.hasClaim("name"))
                ? jwt.getClaimAsString("name")
                : dto.getPropietarioNombre();

        Tramite creado = tramiteService.crearTramite(dto, propietarioId, propietarioNombre);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<Tramite> actualizarEstado(
            @PathVariable String id,
            @Valid @RequestBody ActualizarEstadoDto dto
    ) {
        Tramite actualizado = tramiteService.actualizarEstado(id, dto.getEstado());
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        tramiteService.eliminarTramite(id);
        return ResponseEntity.noContent().build();
    }
}
