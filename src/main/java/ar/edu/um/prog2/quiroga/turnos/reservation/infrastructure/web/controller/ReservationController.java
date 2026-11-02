package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.controller;

import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.service.ReservationService;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.dto.ReservationPageResponse;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.dto.ReservationResponse;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.dto.StartReservationRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.dto.SubmitPhoneRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.mapper.ReservationDtoMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// El usuario sale del token (claim "sub"), nunca del body: cada uno reserva y consulta solo lo suyo.
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationDtoMapper reservationDtoMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse startReservation(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody StartReservationRequest request) {
        return reservationDtoMapper.toResponse(
                reservationService.startReservation(jwt.getSubject(), reservationDtoMapper.toDomain(request)));
    }

    // Las reservas propias, de la más nueva a la más vieja (enunciado §5 punto 13).
    @GetMapping
    public ReservationPageResponse listReservations(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestParam(defaultValue = "0") @Min(0) int page,
                                                    @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return reservationDtoMapper.toPageResponse(reservationService.listReservations(jwt.getSubject(), page, size));
    }

    @GetMapping("/{id}")
    public ReservationResponse getReservation(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return reservationDtoMapper.toResponse(reservationService.getReservation(jwt.getSubject(), id));
    }

    // 202: el teléfono quedó guardado para enviar; el resultado llega después por Kafka y se ve con el GET.
    @PostMapping("/{id}/phone")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ReservationResponse submitPhone(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                           @Valid @RequestBody SubmitPhoneRequest request) {
        return reservationDtoMapper.toResponse(reservationService.submitPhone(jwt.getSubject(), id, request.getPhoneNumber()));
    }
}
