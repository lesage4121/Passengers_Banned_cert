package com.PassBanned.API.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;


    private String password;

    @NotNull
    private String role;

    @NotNull
    private String status;

    private String avatarurl;

    private boolean mfaEnabled;

}
