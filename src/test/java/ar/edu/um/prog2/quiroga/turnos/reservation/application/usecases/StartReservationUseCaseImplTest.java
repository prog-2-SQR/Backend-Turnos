package ar.edu.um.prog2.quiroga.turnos.reservation.application.usecases;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.HOLD_EXPIRES_AT;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.MONDAY;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.NOW;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.TWELVE_THIRTY;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.hold;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.juan;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.mondayAtTwelveThirty;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.requested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Hold;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Patient;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.HoldClient;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PatientDirectory;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.SlotAvailability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StartReservationUseCaseImplTest {

    @Mock
    private PatientDirectory patientDirectory;

    @Mock
    private SlotAvailability slotAvailability;

    @Mock
    private ReservationProcessRepository reservationProcessRepository;

    @Mock
    private HoldClient holdClient;

    private StartReservationUseCaseImpl useCase;

    // Estado del proceso en cada guardado, en orden (el mismo objeto cambia entre un guardado y otro).
    private final List<ReservationStatus> savedStatuses = new ArrayList<>();

    @BeforeEach
    void setUp() {
        useCase = new StartReservationUseCaseImpl(patientDirectory, slotAvailability, reservationProcessRepository, holdClient,
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(patientDirectory.findPatient("juan.perez")).thenReturn(juan());
        when(reservationProcessRepository.findPendingReservationProcess(42L)).thenReturn(Optional.empty());
        when(slotAvailability.isAvailable(201L, MONDAY, TWELVE_THIRTY)).thenReturn(true);
        when(reservationProcessRepository.saveReservationProcess(any())).thenAnswer(invocation -> {
            ReservationProcess process = invocation.getArgument(0);
            savedStatuses.add(process.getStatus());
            return process;
        });
        when(holdClient.createHold(any())).thenReturn(hold());
        when(holdClient.confirmHold(any(), any())).thenReturn(HOLD_EXPIRES_AT.plusSeconds(60));
    }

    @Test
    void holdsAndConfirmsTheSlotSavingEachStep() {
        ReservationProcess process = useCase.startReservation("juan.perez", mondayAtTwelveThirty());

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.WAITING_FOR_PHONE);
        assertThat(process.getHoldId()).isEqualTo("hold-6c0c2f41");
        assertThat(process.getExpiresAt()).isEqualTo(HOLD_EXPIRES_AT.plusSeconds(60));
        // Guardado antes de llamar a la cátedra, y después de cada respuesta.
        assertThat(savedStatuses).containsExactly(ReservationStatus.REQUESTED, ReservationStatus.HELD, ReservationStatus.WAITING_FOR_PHONE);
    }

    @Test
    void confirmsWithTheNamesOfTheRequestWhenTheyCome() {
        ReservationRequest request = mondayAtTwelveThirty();
        request.setPatientFirstName("Juan Carlos");

        useCase.startReservation("juan.perez", request);

        verify(holdClient).confirmHold(any(), argThat(
                (Patient patient) -> patient.getFirstName().equals("Juan Carlos") && patient.getLastName().equals("Perez")));
    }

    @Test
    void withoutNamesTheCatedraIsNotCalled() {
        when(patientDirectory.findPatient("juan.perez")).thenReturn(juan().toBuilder().firstName(null).lastName(null).build());

        assertThatThrownBy(() -> useCase.startReservation("juan.perez", mondayAtTwelveThirty()))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.PATIENT_NAME_REQUIRED);
        verifyNoInteractions(slotAvailability, holdClient);
    }

    @Test
    void aReservationInProgressBlocksANewOne() {
        ReservationProcess pending = requested();
        pending.hold(hold(), NOW);
        when(reservationProcessRepository.findPendingReservationProcess(42L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> useCase.startReservation("juan.perez", mondayAtTwelveThirty()))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.RESERVATION_ALREADY_PENDING);
        verifyNoInteractions(slotAvailability, holdClient);
    }

    @Test
    void anAbandonedReservationIsClosedAndANewOneStarts() {
        ReservationProcess expiredHold = ReservationProcess.request(juan(), mondayAtTwelveThirty(), NOW.minusSeconds(3600));
        expiredHold.hold(Hold.builder().holdId("hold-viejo").catedraProcessId("process-viejo").expiresAt(NOW.minusSeconds(1)).build(),
                NOW.minusSeconds(3600));
        when(reservationProcessRepository.findPendingReservationProcess(42L)).thenReturn(Optional.of(expiredHold));

        useCase.startReservation("juan.perez", mondayAtTwelveThirty());

        assertThat(expiredHold.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(savedStatuses).first().isEqualTo(ReservationStatus.EXPIRED);
    }

    @Test
    void aSlotThatIsNoLongerFreeIsNotRequested() {
        when(slotAvailability.isAvailable(201L, MONDAY, TWELVE_THIRTY)).thenReturn(false);

        assertThatThrownBy(() -> useCase.startReservation("juan.perez", mondayAtTwelveThirty()))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.SLOT_NOT_AVAILABLE);
        verify(reservationProcessRepository, never()).saveReservationProcess(any());
        verifyNoInteractions(holdClient);
    }

    @Test
    void aRejectedHoldEndsTheProcessWithTheCodeOfTheCatedra() {
        when(holdClient.createHold(any())).thenThrow(new ReservationException(ReservationException.SLOT_ALREADY_HELD, "ocupado"));

        assertThatThrownBy(() -> useCase.startReservation("juan.perez", mondayAtTwelveThirty()))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.SLOT_ALREADY_HELD);
        assertThat(savedStatuses).containsExactly(ReservationStatus.REQUESTED, ReservationStatus.FAILED);
        verify(holdClient, never()).confirmHold(any(), any());
    }

    @Test
    void anExpiredHoldOnConfirmationEndsAsExpired() {
        when(holdClient.confirmHold(any(), any())).thenThrow(new ReservationException(ReservationException.HOLD_EXPIRED, "vencido"));

        assertThatThrownBy(() -> useCase.startReservation("juan.perez", mondayAtTwelveThirty()))
                .isInstanceOf(ReservationException.class);
        assertThat(savedStatuses).containsExactly(ReservationStatus.REQUESTED, ReservationStatus.HELD, ReservationStatus.EXPIRED);
    }

    @Test
    void anotherConfirmationErrorEndsAsFailed() {
        when(holdClient.confirmHold(any(), any())).thenThrow(new ReservationException(ReservationException.CATEDRA_UNAVAILABLE, "caída"));

        assertThatThrownBy(() -> useCase.startReservation("juan.perez", mondayAtTwelveThirty()))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.CATEDRA_UNAVAILABLE);
        assertThat(savedStatuses).containsExactly(ReservationStatus.REQUESTED, ReservationStatus.HELD, ReservationStatus.FAILED);
    }

    @Test
    void theProcessRemembersWhenItStarted() {
        ReservationProcess process = useCase.startReservation("juan.perez", mondayAtTwelveThirty());

        assertThat(process.getCreatedAt()).isEqualTo(NOW);
        assertThat(process.getExternalPatientId()).isEqualTo("user-42");
        assertThat(process.getUpdatedAt()).isEqualTo(NOW);
    }
}
