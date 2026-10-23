package ar.edu.um.prog2.quiroga.turnos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "security.jwt.base64-secret=" + TestSecrets.JWT_SECRET)
class TurnosApplicationTests {

	@Test
	void contextLoads() {
	}

}
