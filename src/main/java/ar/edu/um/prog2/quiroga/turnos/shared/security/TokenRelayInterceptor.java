package ar.edu.um.prog2.quiroga.turnos.shared.security;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Token relay: copia en el pedido saliente el mismo JWT con el que el usuario llamó a Turnos. El catálogo lo valida
 * con el secreto compartido, así que no hace falta otra credencial. Sin usuario autenticado no agrega nada
 * (y el catálogo responde 401). El JWT técnico de la cátedra nunca pasa por acá.
 */
public class TokenRelayInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication) {
            request.getHeaders().setBearerAuth(authentication.getToken().getTokenValue());
        }
        return execution.execute(request, body);
    }
}
