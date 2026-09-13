package Backend133.backend_msal.dto;

import Backend133.backend_msal.entity.EstadoTramite;
import jakarta.validation.constraints.NotNull;

public class ActualizarEstadoDto {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoTramite estado;

    public ActualizarEstadoDto() {
    }

    public ActualizarEstadoDto(EstadoTramite estado) {
        this.estado = estado;
    }

    public EstadoTramite getEstado() {
        return estado;
    }

    public void setEstado(EstadoTramite estado) {
        this.estado = estado;
    }
}
