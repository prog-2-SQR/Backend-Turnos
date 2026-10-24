package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mismas tablas que JHipster: jhi_user y jhi_user_authority ("user" es palabra reservada en PostgreSQL). */
@Entity
@Table(name = "jhi_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String login;
    @Column(name = "password_hash", nullable = false, length = 128)
    private String passwordHash;
    @Column(length = 50)
    private String firstName;
    @Column(length = 50)
    private String lastName;
    @Column(unique = true, length = 254)
    private String email;
    @Column(length = 256)
    private String imageUrl;
    @Column(nullable = false)
    private Boolean activated;
    @Column(length = 10)
    private String langKey;
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "jhi_user_authority", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "authority_name", length = 50)
    private Set<String> authorities = new HashSet<>();
    @Column(nullable = false, length = 50)
    private String createdBy;
    private Instant createdDate;
    @Column(length = 50)
    private String lastModifiedBy;
    private Instant lastModifiedDate;
}
