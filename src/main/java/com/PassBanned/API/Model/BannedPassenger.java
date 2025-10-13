package com.PassBanned.API.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "banned_passengers")
@EntityListeners(AuditingEntityListener.class) // Permet la gestion automatique des dates (createdAt, updatedAt)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BannedPassenger {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50) // Limite la longueur
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank
    @Size(max = 50)
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @NotBlank
    @Email // Vérifie que c’est bien un email
    @Size(max = 100)
    @Column(nullable = false)
    private String email;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false)
    private String phone;

    @NotBlank
    @Size(max = 20)
    @Column(name = "passport_number", nullable = false, unique = true) // Unique = pas de doublons
    private String passportNumber;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false)
    private String nationality;

    @NotNull
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    // ----- Infos sur le bannissement -----
    @NotBlank
    @Size(max = 1000)
    @Column(name = "ban_reason", nullable = false)
    private String banReason;

    @NotNull
    @Column(name = "ban_date", nullable = false)
    private LocalDate banDate;

    @Enumerated(EnumType.STRING) // Sauvegarde l'énumération sous forme de texte
    @Column(name = "ban_duration", nullable = false)
    private BanDuration banDuration = BanDuration.TEMPORARY; // Valeur par défaut

    @Column(name = "ban_end_date") // Peut être null si durée permanente
    private LocalDate banEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity = Severity.MEDIUM; // Gravité par défaut

    @Column(columnDefinition = "TEXT") // Permet d’avoir un texte long
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    // ----- Relation avec l’utilisateur qui a créé l’enregistrement -----
    @ManyToOne(fetch = FetchType.LAZY) // Plusieurs passagers peuvent être créés par un même utilisateur
    @JoinColumn(name = "created_by_id", nullable = false) // Colonne clé étrangère
    private User createdBy;

    // ----- Dates automatiques -----
    @CreatedDate // Rempli automatiquement lors de la création
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate // Mis à jour automatiquement lors d'une modification
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;



    // ----- Getters et Setters -----
    // (permettent d’accéder et de modifier les champs)


    // ----- Énumérations internes -----
    // Durée du bannissement
    public enum BanDuration {
        TEMPORARY, PERMANENT
    }

    // Niveau de gravité
    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    // État actuel du bannissement
    public enum Status {
        ACTIVE, EXPIRED, UNDER_REVIEW
    }
}
