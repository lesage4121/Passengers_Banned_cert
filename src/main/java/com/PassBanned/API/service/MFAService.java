package com.PassBanned.API.service;

import com.PassBanned.API.Model.User;
import com.PassBanned.API.dto.AuthResponse;
import com.PassBanned.API.dto.UserDto;
import com.PassBanned.API.repository.UserRepository;
import com.PassBanned.API.security.JwtTokenProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MFAService {

 GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
            .setWindowSize(5) // accepte +/- 2 périodes de 30 secondes
            .build();

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator(config);

    @Autowired
    private UserService userService ;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    public String generateSimpleSecret() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    public String generateSecret(User user) {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        user.setMfaSecret(key.getKey());
        user.setMfaEnabled(true);
        userRepository.save(user);
        return key.getKey();
    }

    public String buildOtpAuthUrl(String email, String secret) {
        return String.format("otpauth://totp/PassBanned:%s?secret=%s&issuer=PassBanned", email, secret);
    }

    public AuthResponse verifyCode(String email, int code)  {
        UserDto user = userService.getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getMfaSecret() == null) {
            throw new RuntimeException("MFA non configuré");
        }

        boolean isValid = gAuth.authorize(user.getMfaSecret(), code);
        System.out.println(isValid);
        if (!isValid) {
            throw new RuntimeException("Code MFA invalide");
        }

        User user1 = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("user not found"));
        user1.setMfaConfigured(true);
        userRepository.save(user1);
        AuthResponse response = new AuthResponse();
        response.setToken(tokenProvider.generateTokenFromUsername(email));
        response.setUser(user);

        return response;
    }

}
