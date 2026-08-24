package com.taskcenter.service;

import com.taskcenter.dto.AuthResponse;
import com.taskcenter.dto.LoginRequest;
import com.taskcenter.dto.RegisterRequest;
import com.taskcenter.model.User;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);

        return new AuthResponse(jwt, AuthResponse.UserDto.fromEntity(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();

        return new AuthResponse(jwt, AuthResponse.UserDto.fromEntity(user));
    }
}
