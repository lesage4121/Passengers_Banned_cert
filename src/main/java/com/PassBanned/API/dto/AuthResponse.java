package com.PassBanned.API.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthResponse {
    private boolean mfaRequired = false;
    private String token;
    private String type = "Bearer";
    private UserDto user;
    private boolean mfaSetup = false;
    private String otpAuthUrl; // data pour QR (si mfaSetup true)

    // Constructors
    public AuthResponse(String token, UserDto user, String otpAuthUrl) {
        this.token = token;
        this.user = user;
        this.otpAuthUrl = otpAuthUrl;
    }

}