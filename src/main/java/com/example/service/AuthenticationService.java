package com.example.service;

import com.example.dto.AuthenticationRequest;
import com.example.dto.AuthenticationResponse;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.repository.UserRepository;
import com.example.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(String username, String email, String password, Role role) {
        // Vérifier si l'utilisateur existe déjà
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

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
        // Authentifier
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // Récupérer l'utilisateur
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Générer le token
        String token = jwtService.generateToken(user);

        return new AuthenticationResponse(token, user.getUsername(), user.getRole().name());
    }
}