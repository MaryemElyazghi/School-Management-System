package com.example.dto;

import com.example.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour la gestion des utilisateurs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit contenir entre 3 et 50 caractères")
    private String username;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    private String email;

    // Le mot de passe n'est requis que pour la création
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;

    private String firstName;
    private String lastName;

    @NotNull(message = "Le rôle est obligatoire")
    private Role role;

    private Boolean enabled;

    // Champs d'audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Champs calculés
    private String fullName;
    private String roleDisplay;
}
