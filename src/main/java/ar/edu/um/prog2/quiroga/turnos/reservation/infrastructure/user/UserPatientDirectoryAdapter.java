package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.user;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Patient;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PatientDirectory;
import ar.edu.um.prog2.quiroga.turnos.user.application.service.UserService;
import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Único punto de contacto de reservation con el slice user (ADR 0004, puerto de salida): usa su service. */
@Component
@RequiredArgsConstructor
public class UserPatientDirectoryAdapter implements PatientDirectory {

    private final UserService userService;

    @Override
    public Patient findPatient(String login) {
        User user = userService.findUserByLogin(login);
        return Patient.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }
}
