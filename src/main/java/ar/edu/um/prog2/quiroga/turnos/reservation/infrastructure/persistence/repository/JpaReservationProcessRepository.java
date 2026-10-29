package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaReservationProcessRepository extends JpaRepository<ReservationProcessEntity, UUID> {

    Optional<ReservationProcessEntity> findFirstByUserIdAndStatusIn(Long userId, Collection<ReservationStatus> statuses);

    Optional<ReservationProcessEntity> findByCatedraProcessId(String catedraProcessId);
}
