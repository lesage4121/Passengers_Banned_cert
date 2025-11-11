package com.PassBanned.API.service;

import com.PassBanned.API.Model.User;
import com.PassBanned.API.dto.AuthRequest;
import com.PassBanned.API.dto.AuthResponse;
import com.PassBanned.API.dto.UserDto;
import com.PassBanned.API.repository.UserRepository;
import com.PassBanned.API.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private MFAService mfaService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserService userService;

    public AuthResponse authenticate(AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getEmail(),
                        authRequest.getPassword()
                )
        );

        UserDto user = userService.getUserByEmail(authRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String token = tokenProvider.generateToken(authentication);
        if (!user.isMfaEnabled()) {
            userService.updateLastLogin(authRequest.getEmail());
            return new AuthResponse(token, user,null);
        }

        if( user.isMfaEnabled() && !user.isMfaConfigured()) {
            AuthResponse authResponse = new AuthResponse();
            authResponse.setMfaRequired(true);
            authResponse.setMfaSetup(true);
            authResponse.setUser(user);
            authResponse.setOtpAuthUrl(mfaService.buildOtpAuthUrl(user.getEmail(), user.getMfaSecret()));
            return  authResponse;
        }

        AuthResponse response = new AuthResponse();
        response.setMfaRequired(true);
        response.setMfaSetup(false);
        response.setUser(user);
        return response;
    }

    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }

    public String getUsernameFromToken(String token) {
        return tokenProvider.getUsernameFromToken(token);
    }
}