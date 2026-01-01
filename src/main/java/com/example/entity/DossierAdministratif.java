package com.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "dossiers_administratifs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DossierAdministratif extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String numeroInscription; // Format: FILIERE-ANNEE-ID

    @Column(nullable = false)
    private LocalDate dateCreation;

    @OneToOne
    @JoinColumn(name = "student_id", unique = true)
    private Student student;

    /**
     * Génère le numéro d'inscription selon le format: FILIERE-ANNEE-ID
     * Exemple: GINF-2025-1, GSTR-2025-2
     */
    public void generateNumeroInscription(String departmentCode, Long studentId) {
        int year = LocalDate.now().getYear();
        this.numeroInscription = String.format("%s-%d-%d", departmentCode, year, studentId);
    }
}