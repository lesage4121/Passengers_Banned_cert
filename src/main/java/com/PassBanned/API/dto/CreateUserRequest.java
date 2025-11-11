package com.PassBanned.API.dto;

import com.PassBanned.API.Model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    @NotNull
    private String role;

    @NotNull
    private String status;

    private String avatarUrl;

    private boolean mfaEnabled;

}
