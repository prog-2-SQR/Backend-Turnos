package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.adapter;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.HOLD_EXPIRES_AT;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.NOW;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.hold;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.juan;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.mondayAtTwelveThirty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.TestcontainersConfiguration;
import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Hold;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationPage;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.mapper.ReservationProcessMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Sin la transacción del test: cada guardado se confirma, como en la app, y el índice se comprueba de verdad.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({TestcontainersConfiguration.class, JpaReservationProcessRepositoryAdapter.class, ReservationProcessMapper.class})
class JpaReservationProcessRepositoryAdapterTest {

    @Autowired
    private JpaReservationProcessRepositoryAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clean() {
        jdbcTemplate.execute("TRUNCATE reservation_process CASCADE");
    }

    private ReservationProcess newRequest(long userId) {
        return ReservationProcess.request(juan().toBuilder().userId(userId).build(), mondayAtTwelveThirty(), NOW);
    }

    @Test
    void savesAndReadsTheWholeProcess() {
        ReservationProcess process = adapter.saveReservationProcess(newRequest(42L));
        process.hold(hold(), NOW);
        adapter.saveReservationProcess(process);

        ReservationProcess read = adapter.findReservationProcessById(process.getId()).orElseThrow();
        assertThat(read.getStatus()).isEqualTo(ReservationStatus.HELD);
        assertThat(read.getHoldId()).isEqualTo("hold-6c0c2f41");
        assertThat(read.getCatedraProcessId()).isEqualTo("process-a92b47d0");
        assertThat(read.getExpiresAt()).isEqualTo(HOLD_EXPIRES_AT);
        assertThat(read.getExternalPatientId()).isEqualTo("user-42");
        assertThat(read.getVersion()).isEqualTo(1L);
    }

    @Test
    void aSecondPendingProcessOfTheSameUserIsRejectedByTheDatabase() {
        adapter.saveReservationProcess(newRequest(42L));

        assertThatThrownBy(() -> adapter.saveReservationProcess(newRequest(42L)))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.RESERVATION_ALREADY_PENDING);
    }

    @Test
    void finishedProcessesDoNotCount() {
        ReservationProcess failed = adapter.saveReservationProcess(newRequest(42L));
        failed.fail("SLOT_ALREADY_HELD", NOW);
        adapter.saveReservationProcess(failed);

        ReservationProcess next = adapter.saveReservationProcess(newRequest(42L));

        assertThat(adapter.findPendingReservationProcess(42L)).get().extracting(ReservationProcess::getId).isEqualTo(next.getId());
    }

    @Test
    void findsTheProcessByTheIdOfTheCatedra() {
        ReservationProcess process = adapter.saveReservationProcess(newRequest(42L));
        process.hold(hold(), NOW);
        adapter.saveReservationProcess(process);

        assertThat(adapter.findReservationProcessByCatedraProcessId("process-a92b47d0")).get()
                .extracting(ReservationProcess::getId).isEqualTo(process.getId());
        assertThat(adapter.findReservationProcessByCatedraProcessId("process-otro")).isEmpty();
    }

    @Test
    void otherUsersCanHaveTheirOwnPendingProcess() {
        adapter.saveReservationProcess(newRequest(42L));

        adapter.saveReservationProcess(newRequest(7L));

        assertThat(adapter.findPendingReservationProcess(7L)).isPresent();
        assertThat(adapter.findPendingReservationProcess(99L)).isEmpty();
    }

    // Tres procesos terminados de Juan (uno por minuto) y uno de otro usuario.
    @Test
    void listsOnlyTheProcessesOfTheUserNewestFirst() {
        List<UUID> juans = new ArrayList<>();
        for (int minute = 0; minute < 3; minute++) {
            ReservationProcess process = ReservationProcess.request(juan(), mondayAtTwelveThirty(), NOW.plusSeconds(60L * minute));
            process.fail("SLOT_ALREADY_HELD", NOW);
            juans.add(adapter.saveReservationProcess(process).getId());
        }
        adapter.saveReservationProcess(newRequest(99L));

        ReservationPage first = adapter.findReservationProcessesByUser(42L, 0, 2);
        ReservationPage second = adapter.findReservationProcessesByUser(42L, 1, 2);

        assertThat(first.getContent()).extracting(ReservationProcess::getId).containsExactly(juans.get(2), juans.get(1));
        assertThat(second.getContent()).extracting(ReservationProcess::getId).containsExactly(juans.get(0));
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(adapter.findReservationProcessesByUser(7L, 0, 2).getContent()).isEmpty();
    }

    @Test
    void onlyProcessesInProgressWithAnIdOfTheCatedraAreReconcilable() {
        ReservationProcess requested = adapter.saveReservationProcess(newRequest(1L));
        ReservationProcess held = adapter.saveReservationProcess(newRequest(2L));
        held.hold(hold(), NOW);
        held = adapter.saveReservationProcess(held);
        ReservationProcess failed = adapter.saveReservationProcess(newRequest(3L));
        failed.hold(Hold.builder().holdId("hold-2").catedraProcessId("process-2").expiresAt(HOLD_EXPIRES_AT).build(), NOW);
        failed.fail("X", NOW);
        adapter.saveReservationProcess(failed);

        assertThat(adapter.findReconcilableReservationProcesses())
                .extracting(ReservationProcess::getId)
                .containsExactly(held.getId())
                .doesNotContain(requested.getId());
    }
}
