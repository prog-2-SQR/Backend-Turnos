package ar.edu.um.prog2.quiroga.turnos.shared.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Arma las respuestas de error en formato {@code application/problem+json} con un {@code code} estable
 * (anexo §13: los clientes deciden por {@code status} y {@code code}, nunca por el texto de {@code detail}).
 * Es transversal: los slices la usan, pero no conoce a ningún slice.
 */
public final class ApiProblems {

    public static final String CODE = "code";
    public static final String FIELD_ERRORS = "fieldErrors";

    private ApiProblems() {
    }

    public static ProblemDetail of(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty(CODE, code);
        return problem;
    }

    public static ProblemDetail badRequest(String code, String detail) {
        return of(HttpStatus.BAD_REQUEST, code, detail);
    }
}
