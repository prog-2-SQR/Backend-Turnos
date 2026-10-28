package ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JSON de GET /api/professionals/{id} del catálogo (contrato turnos-catalogo). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogProfessionalResponse {

    private Long id;
    private Long categoryId;
    private String firstName;
    private String lastName;
    private Boolean enabled;
}
