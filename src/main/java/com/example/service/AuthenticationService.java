package com.example.service;

import com.example.dto.AuthenticationRequest;
import com.example.dto.AuthenticationResponse;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.exception.BusinessException;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.UserRepository;
import com.example.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // Constantes pour la validation du mot de passe
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");

    public AuthenticationResponse register(String username, String email, String password, Role role) {
        // Validation du nom d'utilisateur
        if (username == null || username.trim().isEmpty()) {
            throw new BusinessException("Username is required");
        }

        if (username.length() < 3) {
            throw new BusinessException("Username must be at least 3 characters long");
        }

        // Validation de l'email
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException("Email is required");
        }

        if (!isValidEmail(email)) {
            throw new BusinessException("Invalid email format");
        }

        // Vérifier si l'utilisateur existe déjà
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("Username already exists: " + username);
        }

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email already exists: " + email);
        }

        // Validation du mot de passe
        validatePassword(password);

        // Créer l'utilisateur
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);

        userRepository.save(user);

        // Générer le token
        String token = jwtService.generateToken(user);

        return new AuthenticationResponse(token, username, role.name());
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // Validation des entrées
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BusinessException("Username is required");
        }

        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new BusinessException("Password is required");
        }

        try {
            // Authentifier
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new BusinessException("Invalid username or password");
        }

        // Récupérer l'utilisateur
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", request.getUsername()));

        // Vérifier si le compte est activé
        if (!user.isEnabled()) {
            throw new BusinessException("Account is disabled. Please contact an administrator.");
        }

        // Générer le token
        String token = jwtService.generateToken(user);

        return new AuthenticationResponse(token, user.getUsername(), user.getRole().name());
    }

    /**
     * Valider le format de l'email
     */
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
            throw new BusinessException("Password is required");
        }

        StringBuilder errors = new StringBuilder();

        if (password.length() < MIN_PASSWORD_LENGTH) {
            errors.append("Password must be at least ").append(MIN_PASSWORD_LENGTH).append(" characters long. ");
        }

        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            errors.append("Password must contain at least one uppercase letter. ");
        }

        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            errors.append("Password must contain at least one lowercase letter. ");
        }

        if (!DIGIT_PATTERN.matcher(password).find()) {
            errors.append("Password must contain at least one digit. ");
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            errors.append("Password must contain at least one special character (!@#$%^&*(),.?\":{}|<>). ");
        }

        if (errors.length() > 0) {
            throw new BusinessException("Password validation failed: " + errors.toString().trim());
        }
    }
}