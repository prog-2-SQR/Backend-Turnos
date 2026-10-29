package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.prog2.quiroga.turnos.TestcontainersConfiguration;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.mapper.ProcessedEventMapper;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.repository.JpaProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaProcessedEventRepositoryAdapter.class, ProcessedEventMapper.class})
class JpaProcessedEventRepositoryAdapterTest {

    @Autowired
    private JpaProcessedEventRepositoryAdapter adapter;

    @Autowired
    private JpaProcessedEventRepository jpaProcessedEventRepository;

    @Test
    void aSavedEventIsRecognizedByItsId() {
        assertThat(adapter.existsByEventId("request-event-8a31")).isFalse();

        adapter.saveProcessedEvent("request-event-8a31", "AdditionalInformationRequested");

        assertThat(adapter.existsByEventId("request-event-8a31")).isTrue();
        assertThat(jpaProcessedEventRepository.findById("request-event-8a31")).get().satisfies(entity -> {
            assertThat(entity.getEventType()).isEqualTo("AdditionalInformationRequested");
            assertThat(entity.getProcessedAt()).isNotNull();
        });
    }
}
