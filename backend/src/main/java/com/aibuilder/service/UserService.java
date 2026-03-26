package com.aibuilder.service;

import com.aibuilder.dto.CreateUserRequest;
import com.aibuilder.dto.LoginRequest;
import com.aibuilder.dto.UserResponse;
import com.aibuilder.dto.AuthResponse;
import com.aibuilder.entity.User;
import com.aibuilder.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.aibuilder.security.JwtService;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream().map(this::mapUser).toList();
    }

    public UserResponse createUser(CreateUserRequest request) {
        return register(request).user();
    }

    public AuthResponse register(CreateUserRequest request) {
        userRepository.findByEmail(request.email().trim().toLowerCase())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("An account with this email already exists.");
                });

        User user = User.builder()
                .name(request.name().trim())
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password().trim()))
                .build();

        User savedUser = userRepository.save(user);
        return new AuthResponse(
                mapUser(savedUser),
                jwtService.generateToken(savedUser.getEmail()),
                "Registration successful."
        );
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Account not found for this email."));

        if (!passwordEncoder.matches(request.password().trim(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        return new AuthResponse(
                mapUser(user),
                jwtService.generateToken(user.getEmail()),
                "Login successful."
        );
    }

    public User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    public long countUsers() {
        return userRepository.count();
    }

    public UserResponse getProfile(User user) {
        return mapUser(user);
    }

    public User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Authentication required.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        if (principal instanceof UserDetails userDetails) {
            return userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));
        }
        if (principal instanceof String username && !"anonymousUser".equals(username)) {
            return userRepository.findByEmail(username)
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));
        }

        throw new IllegalArgumentException("Authentication required.");
    }

    private UserResponse mapUser(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
