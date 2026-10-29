package ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Patient;

/** El paciente que corresponde al usuario logueado. */
public interface PatientDirectory {

    Patient findPatient(String login);
}
