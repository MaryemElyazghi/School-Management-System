package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * ✅ DTO pour Department - VERSION CORRIGÉE
 *
 * Champs:
 * - id, code, name, description (obligatoires)
 * - studentCount, courseCount (statistiques)
 * - createdAt, updatedAt (audit)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDTO {

    private Long id;

    @NotBlank(message = "Le code de la filière est obligatoire")
    @Size(max = 10, message = "Le code ne peut pas dépasser 10 caractères")
    private String code;

    @NotBlank(message = "Le nom de la filière est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String name;

    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    private String description;

    // Champs optionnels pour statistiques (affichage uniquement)
    private Integer studentCount;
    private Integer courseCount;

    // ✅ AJOUT : Champs d'audit pour l'affichage
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}