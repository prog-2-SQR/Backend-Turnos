package ar.edu.um.prog2.quiroga.turnos.auth.infrastructure.web.controller;

import ar.edu.um.prog2.quiroga.turnos.auth.application.service.AuthService;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AccessToken;
import ar.edu.um.prog2.quiroga.turnos.auth.infrastructure.web.dto.LoginRequest;
import ar.edu.um.prog2.quiroga.turnos.auth.infrastructure.web.dto.TokenResponse;
import ar.edu.um.prog2.quiroga.turnos.auth.infrastructure.web.mapper.AuthDtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthDtoMapper authDtoMapper;

    // Como JHipster: el token va en el cuerpo (id_token) y también en el header Authorization.
    @PostMapping("/authenticate")
    public ResponseEntity<TokenResponse> authenticate(@Valid @RequestBody LoginRequest request) {
        AccessToken token = authService.authenticate(request.getUsername(), request.getPassword(), Boolean.TRUE.equals(request.getRememberMe()));
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getValue())
                .body(authDtoMapper.toResponse(token));
    }
}
