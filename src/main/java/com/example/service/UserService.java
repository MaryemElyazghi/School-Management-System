package com.example.service;

import com.example.dto.UserDTO;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.exception.BusinessException;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des utilisateurs
 * Accessible uniquement aux ADMIN
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Constantes pour la validation du mot de passe
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");

    // ════════════════════════════════════════════════════════════════════════════
    // LECTURE (READ)
    // ════════════════════════════════════════════════════════════════════════════

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return convertToDTO(user);
    }

    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return convertToDTO(user);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CRÉATION (CREATE)
    // ════════════════════════════════════════════════════════════════════════════

    public UserDTO createUser(UserDTO userDTO) {
        log.info("📝 Création d'un nouvel utilisateur: {}", userDTO.getUsername());

        // Validations
        validateUserData(userDTO, null);

        // Vérifier que le mot de passe est fourni lors de la création
        if (userDTO.getPassword() == null || userDTO.getPassword().trim().isEmpty()) {
            throw new BusinessException("Le mot de passe est obligatoire pour la création d'un utilisateur");
        }

        // Valider la force du mot de passe
        validatePassword(userDTO.getPassword());

        // Créer l'utilisateur
        User user = convertToEntity(userDTO);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setEnabled(userDTO.getEnabled() != null ? userDTO.getEnabled() : true);

        User savedUser = userRepository.save(user);
        userRepository.flush();

        log.info("✅ Utilisateur créé avec succès: {} (ID={})", savedUser.getUsername(), savedUser.getId());

        return convertToDTO(savedUser);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // MODIFICATION (UPDATE)
    // ════════════════════════════════════════════════════════════════════════════

    public UserDTO updateUser(Long id, UserDTO userDTO) {
        log.info("📝 Modification de l'utilisateur ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Validations
        validateUserData(userDTO, id);

        // Mise à jour des champs
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setRole(userDTO.getRole());
        
        if (userDTO.getEnabled() != null) {
            user.setEnabled(userDTO.getEnabled());
        }

        // Si un nouveau mot de passe est fourni, le valider et l'encoder
        if (userDTO.getPassword() != null && !userDTO.getPassword().trim().isEmpty()) {
            validatePassword(userDTO.getPassword());
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            log.info("🔑 Mot de passe mis à jour pour l'utilisateur: {}", user.getUsername());
        }

        User updatedUser = userRepository.save(user);
        userRepository.flush();

        log.info("✅ Utilisateur modifié avec succès");

        return convertToDTO(updatedUser);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SUPPRESSION (DELETE)
    // ════════════════════════════════════════════════════════════════════════════

    @Transactional
    public void deleteUser(Long id) {
        log.info("🗑️ ══════════════════════════════════════════════════════════");
        log.info("🗑️ SUPPRESSION UTILISATEUR - ID: {}", id);
        log.info("🗑️ ══════════════════════════════════════════════════════════");

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        String username = user.getUsername();
        log.info("📋 Utilisateur trouvé: {} (ID={})", username, id);

        // Vérification: ne pas permettre la suppression du dernier admin
        if (user.getRole() == Role.ADMIN) {
            long adminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.ADMIN)
                    .count();

            if (adminCount <= 1) {
                log.warn("⛔ SUPPRESSION BLOQUÉE - C'est le dernier administrateur");
                throw new BusinessException(
                        "Impossible de supprimer le dernier administrateur du système");
            }
        }

        // Vérification: ne pas permettre la suppression si l'utilisateur a des relations
        if (user.getStudent() != null) {
            throw new BusinessException(
                    "Impossible de supprimer cet utilisateur car il est lié à un étudiant. " +
                            "Veuillez d'abord supprimer l'étudiant.");
        }

        log.info("🔄 Suppression de l'utilisateur...");
        userRepository.delete(user);
        userRepository.flush();

        log.info("✅ ══════════════════════════════════════════════════════════");
        log.info("✅ UTILISATEUR SUPPRIMÉ AVEC SUCCÈS: {} (ID={})", username, id);
        log.info("✅ ══════════════════════════════════════════════════════════");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // STATISTIQUES
    // ════════════════════════════════════════════════════════════════════════════

    public long countByRole(Role role) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == role)
                .count();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // VALIDATIONS
    // ════════════════════════════════════════════════════════════════════════════

    private void validateUserData(UserDTO dto, Long excludeId) {
        // Username obligatoire
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            throw new BusinessException("Le nom d'utilisateur est obligatoire");
        }

        // Username unique
        userRepository.findByUsername(dto.getUsername()).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new BusinessException(
                        "Le nom d'utilisateur '" + dto.getUsername() + "' est déjà utilisé");
            }
        });

        // Email obligatoire
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new BusinessException("L'email est obligatoire");
        }

        // Email valide
        if (!isValidEmail(dto.getEmail())) {
            throw new BusinessException("Format d'email invalide");
        }

        // Email unique
        userRepository.findByEmail(dto.getEmail()).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new BusinessException(
                        "L'email '" + dto.getEmail() + "' est déjà utilisé");
            }
        });

        // Rôle obligatoire
        if (dto.getRole() == null) {
            throw new BusinessException("Le rôle est obligatoire");
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * Valider la force du mot de passe
     * Règles:
     * - Minimum 8 caractères
     * - Au moins une majuscule
     * - Au moins une minuscule
     * - Au moins un chiffre
     * - Au moins un caractère spécial
     */
    private void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new BusinessException("Le mot de passe est obligatoire");
        }

        StringBuilder errors = new StringBuilder();

        if (password.length() < MIN_PASSWORD_LENGTH) {
            errors.append("Le mot de passe doit contenir au moins ")
                    .append(MIN_PASSWORD_LENGTH).append(" caractères. ");
        }

        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            errors.append("Le mot de passe doit contenir au moins une majuscule. ");
        }

        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            errors.append("Le mot de passe doit contenir au moins une minuscule. ");
        }

        if (!DIGIT_PATTERN.matcher(password).find()) {
            errors.append("Le mot de passe doit contenir au moins un chiffre. ");
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            errors.append("Le mot de passe doit contenir au moins un caractère spécial (!@#$%^&*(),.?\":{}|<>). ");
        }

        if (errors.length() > 0) {
            throw new BusinessException("Validation du mot de passe échouée: " + errors.toString().trim());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CONVERSIONS
    // ════════════════════════════════════════════════════════════════════════════

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setRole(user.getRole());
        dto.setEnabled(user.isEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        // Champs calculés
        if (user.getFirstName() != null && user.getLastName() != null) {
            dto.setFullName(user.getFirstName() + " " + user.getLastName());
        } else {
            dto.setFullName(user.getUsername());
        }

        // Affichage du rôle
        switch (user.getRole()) {
            case ADMIN:
                dto.setRoleDisplay("Administrateur");
                break;
            case STUDENT:
                dto.setRoleDisplay("Étudiant");
                break;
            default:
                dto.setRoleDisplay(user.getRole().name());
        }

        return dto;
    }

    private User convertToEntity(UserDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setRole(dto.getRole());
        user.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        return user;
    }
}
