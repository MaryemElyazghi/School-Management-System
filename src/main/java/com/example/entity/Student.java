package com.example.entity;

import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ Entité Student - VERSION CORRIGÉE AVEC CASCADE
 *
 * Relations:
 * - OneToOne avec DossierAdministratif (cascade ALL + orphanRemoval)
 * - OneToMany avec Enrollment (cascade ALL pour permettre la suppression)
 * - ManyToOne avec Department
 * - OneToOne avec User
 */
@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Student extends BaseEntity {

    private String firstName;
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;
    private LocalDate dateOfBirth;
    private LocalDate enrollmentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * ✅ CORRECTION: Ajout de cascade ALL pour permettre la suppression
     * Avant: pas de cascade
     * Après: CascadeType.ALL pour que la suppression d'un student supprime ses enrollments
     */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Enrollment> enrollments = new ArrayList<>();

    /**
     * ✅ CASCADE ALL + orphanRemoval pour le dossier administratif
     * Quand on supprime le student, le dossier est automatiquement supprimé
     */
    @OneToOne(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private DossierAdministratif dossierAdministratif;

    @Transient
    public String getFullName() {
        return firstName + " " + lastName;
    }
}