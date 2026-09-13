package Backend133.backend_msal.controller;

import Backend133.backend_msal.entity.TipoTramite;
import Backend133.backend_msal.service.CatalogoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping
    public ResponseEntity<List<TipoTramite>> listar(@RequestParam(required = false, defaultValue = "true") boolean soloActivos) {
        List<TipoTramite> lista = soloActivos
                ? catalogoService.listarActivos()
                : catalogoService.listarTodos();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoTramite> obtenerPorId(@PathVariable String id) {
        return catalogoService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TipoTramite> guardar(@RequestBody TipoTramite tipo) {
        TipoTramite guardado = catalogoService.guardarTipo(tipo);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        catalogoService.eliminarTipo(id);
        return ResponseEntity.noContent().build();
    }
}
