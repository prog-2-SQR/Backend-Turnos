package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.web.controller;

import ar.edu.um.prog2.quiroga.turnos.user.application.service.UserService;
import ar.edu.um.prog2.quiroga.turnos.user.infrastructure.web.dto.AccountResponse;
import ar.edu.um.prog2.quiroga.turnos.user.infrastructure.web.dto.RegisterUserRequest;
import ar.edu.um.prog2.quiroga.turnos.user.infrastructure.web.mapper.UserDtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserDtoMapper userDtoMapper;

    // Como JHipster: 201 Created sin cuerpo.
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerUser(@Valid @RequestBody RegisterUserRequest request) {
        userService.registerUser(userDtoMapper.toDomain(request), request.getPassword());
    }

    // El usuario sale del token (claim "sub"), nunca de un parámetro: cada uno solo ve su propia cuenta.
    @GetMapping("/account")
    public AccountResponse getAccount(@AuthenticationPrincipal Jwt jwt) {
        return userDtoMapper.toAccountResponse(userService.findUserByLogin(jwt.getSubject()));
    }
}
