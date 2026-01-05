package com.example.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * ✅ VERSION CORRIGÉE - StudentDTO sans studentNumber
 * L'identifiant unique est désormais UNIQUEMENT l'ID
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {
    private Long id; // ✅ IDENTIFIANT UNIQUE

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    private String phone;
    private LocalDate dateOfBirth;
    private LocalDate enrollmentDate;

    @NotNull(message = "Department is required")
    private Long departmentId;

    private String departmentName;
    private String departmentCode;
    private String fullName;

    // ✅ Numéro d'inscription du dossier administratif (format: FILIERE-ANNEE-ID)
    private String numeroInscription;
    private LocalDate dossierDateCreation;
}