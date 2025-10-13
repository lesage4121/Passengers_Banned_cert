package com.PassBanned.API.controller;


import com.PassBanned.API.Model.User;
import com.PassBanned.API.dto.CreateUserRequest;
import com.PassBanned.API.dto.PasswordChangeRequest;
import com.PassBanned.API.dto.UpdateUserRequest;
import com.PassBanned.API.dto.UserDto;
import com.PassBanned.API.service.SupabaseStorageService;
import com.PassBanned.API.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private SupabaseStorageService storageService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            UserDto userDto = new UserDto();
            userDto.setName(request.getName());
            userDto.setEmail(request.getEmail());
            userDto.setRole(User.Role.valueOf(request.getRole().toUpperCase()));
            userDto.setStatus(User.Status.valueOf(request.getStatus().toUpperCase()));
            userDto.setAvatarUrl(request.getAvatarUrl());

            String password = request.getPassword();
            UserDto createdUser = userService.createUser(userDto, password);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        try {
            UserDto userDto = new UserDto();
            userDto.setName(request.getName());
            userDto.setEmail(request.getEmail());
            userDto.setRole(User.Role.valueOf(request.getRole().toUpperCase()));
            userDto.setStatus(User.Status.valueOf(request.getStatus().toUpperCase()));
            userDto.setAvatarUrl(request.getAvatarurl());

            UserDto updatedUser = userService.updateUser(id, userDto, request.getPassword());
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @PathVariable Long id, 
            @RequestParam("avatar") MultipartFile file) {
        try {
            // Valider le fichier
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Fichier vide"));
            }

            if (!Objects.requireNonNull(file.getContentType()).startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le fichier doit être une image"));
            }

            if (file.getSize() > 5 * 1024 * 1024) { // 5MB
                return ResponseEntity.badRequest().body(Map.of("error", "Le fichier ne doit pas dépasser 5MB"));
            }

            // Récupérer l'utilisateur
            UserDto user = userService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Supprimer l'ancien avatar s'il existe
            if (user.getAvatarUrl() != null) {
                storageService.deleteAvatar(user.getAvatarUrl());
            }

            // Upload du nouvel avatar
            String avatarUrl = storageService.uploadAvatar(file, id.toString());

            // Mettre à jour l'utilisateur
            user.setAvatarUrl(avatarUrl);
            userService.updateUser(id, user,null);

            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de l'upload: " + e.getMessage()));
        }
    }

    @PostMapping("/avatar/upload")
    public ResponseEntity<Map<String, String>> uploadAvatarForNewUser(
            @RequestParam("avatar") MultipartFile file) {
        try {
            // Valider le fichier
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Fichier vide"));
            }

            if (!Objects.requireNonNull(file.getContentType()).startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le fichier doit être une image"));
            }

            if (file.getSize() > 5 * 1024 * 1024) { // 5MB
                return ResponseEntity.badRequest().body(Map.of("error", "Le fichier ne doit pas dépasser 5MB"));
            }

            // Upload de l'avatar avec un nom unique
            String avatarUrl = storageService.uploadAvatarForNewUser(file);

            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de l'upload: " + e.getMessage()));
        }
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getUserStats() {
        Map<String, Long> stats = Map.of(
            "totalUsers", userService.getTotalUsers(),
            "adminCount", userService.getAdminCount(),
            "activeUsers", userService.getActiveUserCount()
        );
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/{id}/password")
    public ResponseEntity<?> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordChangeRequest request) {
        try {
            userService.changePassword(id, request.getAvatarUrl(), request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}