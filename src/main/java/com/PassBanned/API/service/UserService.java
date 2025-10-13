package com.PassBanned.API.service;


import com.PassBanned.API.Model.User;
import com.PassBanned.API.config.SecurityConfig;
import com.PassBanned.API.dto.UserDto;
import com.PassBanned.API.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service // Cette annotation indique que cette classe est un service Spring (composant métier)
@Transactional // Assure que toutes les méthodes sont exécutées dans une transaction (rollback en cas d'erreur)
public class UserService implements UserDetailsService {
    // Implémente UserDetailsService pour l’authentification via Spring Security

    @Autowired
    private UserRepository userRepository; // Injection du repository pour interagir avec la BDD


    @Autowired
    private PasswordEncoder passwordEncoder;

    // 🔹 Méthode utilisée par Spring Security lors de la connexion
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // On cherche l'utilisateur dans la BDD par son email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // On retourne un objet UserDetails que Spring Security peut utiliser pour l'authentification
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail()) // identifiant
                .password(user.getPassword()) // mot de passe (déjà encodé)
                .authorities("ROLE_" + user.getRole().name()) // rôle (converti en ROLE_ADMIN, ROLE_USER, etc.)
                .accountExpired(false) // Compte jamais expiré
                .accountLocked(user.getStatus() == User.Status.INACTIVE) // Si inactif, compte verrouillé
                .credentialsExpired(false) // Les identifiants n’expirent pas
                .disabled(user.getStatus() == User.Status.INACTIVE) // Si inactif → utilisateur désactivé
                .build();
    }

    // 🔹 Récupérer la liste de tous les utilisateurs en DTO
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto) // conversion User → UserDto
                .collect(Collectors.toList());
    }

    // 🔹 Récupérer un utilisateur par son ID
    public Optional<UserDto> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::convertToDto);
    }

    // 🔹 Récupérer un utilisateur par son email
    public Optional<UserDto> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::convertToDto);
    }

    // 🔹 Créer un nouvel utilisateur
    public UserDto createUser(UserDto userDto, String password) {
        // Vérifier si l'email est déjà utilisé
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Construire l'objet User à partir du DTO
        User user = new User();
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(password)); // mot de passe encodé
        user.setRole(userDto.getRole() != null ? userDto.getRole() : User.Role.USER); // rôle par défaut USER
        user.setStatus(userDto.getStatus() != null ? userDto.getStatus() : User.Status.ACTIVE); // statut par défaut ACTIF
        user.setAvatarUrl(userDto.getAvatarUrl());

        // Sauvegarde en BDD
        User savedUser = userRepository.save(user);
        return convertToDto(savedUser); // Retourner le DTO
    }

    // 🔹 Mise à jour d’un utilisateur
    public UserDto updateUser(Long id, UserDto userDto, String password) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Mise à jour des champs
        user.setName(userDto.getName());
        user.setRole(userDto.getRole());
        user.setStatus(userDto.getStatus());
        user.setAvatarUrl(userDto.getAvatarUrl());

        if (password != null && !password.isBlank())
        {
            user.setPassword(password);
        }

        User updatedUser = userRepository.save(user);
        return convertToDto(updatedUser);
    }

    // 🔹 Suppression d’un utilisateur par ID
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }

    // 🔹 Mettre à jour la date du dernier login
    public void updateLastLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    // 🔹 Compter le nombre total d’utilisateurs
    public long getTotalUsers() {
        return userRepository.count();
    }

    // 🔹 Compter uniquement les administrateurs
    public long getAdminCount() {
        return userRepository.countAdmins();
    }

    // 🔹 Compter uniquement les utilisateurs actifs
    public long getActiveUserCount() {
        return userRepository.countActiveUsers();
    }
    public void changePassword(Long id,String avatarUrl, String currentPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
    }

    // 🔹 Conversion d’un objet User en DTO (pour éviter d’exposer directement l’entité)
    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setLastLogin(user.getLastLogin());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
