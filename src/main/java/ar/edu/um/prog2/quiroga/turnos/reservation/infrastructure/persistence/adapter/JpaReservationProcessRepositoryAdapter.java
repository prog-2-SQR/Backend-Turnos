package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationPage;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.mapper.ReservationProcessMapper;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.repository.JpaReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaReservationProcessRepositoryAdapter implements ReservationProcessRepository {

    // Índice único parcial de reservation.yaml: un solo proceso pendiente por usuario.
    static final String ONE_PENDING_PER_USER = "ux_reservation_one_pending_per_user";

    private final JpaReservationProcessRepository jpaReservationProcessRepository;
    private final ReservationProcessMapper reservationProcessMapper;

    // saveAndFlush: el choque con el índice aparece acá y no recién al terminar la transacción. Si dos pedidos del
    // mismo usuario llegan juntos, la base deja pasar uno; el otro se traduce a RESERVATION_ALREADY_PENDING.
    @Override
    public ReservationProcess saveReservationProcess(ReservationProcess process) {
        try {
            return reservationProcessMapper.toDomain(
                    jpaReservationProcessRepository.saveAndFlush(reservationProcessMapper.toEntity(process)));
        } catch (DataIntegrityViolationException e) {
            if (isOnePendingPerUser(e)) {
                throw new ReservationException(ReservationException.RESERVATION_ALREADY_PENDING,
                        "El usuario ya tiene una reserva en curso", e);
            }
            throw e;
        }
    }

    @Override
    public Optional<ReservationProcess> findReservationProcessById(UUID id) {
        return jpaReservationProcessRepository.findById(id).map(reservationProcessMapper::toDomain);
    }

    @Override
    public Optional<ReservationProcess> findPendingReservationProcess(Long userId) {
        return jpaReservationProcessRepository.findFirstByUserIdAndStatusIn(userId, ReservationStatus.pendingStatuses())
                .map(reservationProcessMapper::toDomain);
    }

    @Override
    public Optional<ReservationProcess> findReservationProcessByCatedraProcessId(String catedraProcessId) {
        return jpaReservationProcessRepository.findByCatedraProcessId(catedraProcessId).map(reservationProcessMapper::toDomain);
    }

    // Del más nuevo al más viejo; el id desempata para que una misma página devuelva siempre lo mismo.
    @Override
    public ReservationPage findReservationProcessesByUser(Long userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
        Page<ReservationProcessEntity> found = jpaReservationProcessRepository.findByUserId(userId, pageRequest);
        return ReservationPage.builder()
                .content(found.getContent().stream().map(reservationProcessMapper::toDomain).toList())
                .page(found.getNumber())
                .size(found.getSize())
                .totalElements(found.getTotalElements())
                .build();
    }

    private static boolean isOnePendingPerUser(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(ONE_PENDING_PER_USER);
    }
}
