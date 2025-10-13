package com.PassBanned.API.config;


import com.PassBanned.API.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/*
 * SecurityConfig
 *
 * Configuration centralisée de la sécurité Spring Security pour l'application PassBanned.
 *
 * Points clés :
 *  - Définit les beans de sécurité (PasswordEncoder, AuthenticationManager, SecurityFilterChain).
 *  - Active la sécurité web et la sécurité de méthodes (annotations @PreAuthorize / @PostAuthorize).
 *  - Configure les règles d'autorisation (qui peut accéder à quoi).
 *  - Désactive la session côté serveur (stateless) car on utilise des JWT.
 *  - Branche un filtre JWT personnalisé (JwtAuthenticationFilter) avant le filtre standard de login.
 *  - Configure la politique CORS pour autoriser le frontend (dev : localhost).
 */

@Configuration
@EnableWebSecurity                // Active la configuration web de Spring Security
@EnableMethodSecurity(prePostEnabled = true) // Permet d'utiliser @PreAuthorize / @PostAuthorize sur les méthodes
public class SecurityConfig {


    /**
     * PasswordEncoder bean
     *
     * BCryptPasswordEncoder : encodage sécurisé des mots de passe.
     * - Utilisé pour hacher les mots de passe à la création d'un utilisateur.
     * - Utilisé par le provider d'authentification pour comparer mot de passe brut -> hash.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager bean
     *
     * Retourne l'AuthenticationManager configuré par Spring (providers, UserDetailsService, passwordEncoder).
     * - Utile dans les services d'authentification (ex: AuthController) pour appeler authenticate(...).
     * - Injection via AuthenticationConfiguration pour obtenir l'instance prête à l'emploi.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * SecurityFilterChain
     *
     * Définit la chaîne de filtres de sécurité et les règles HTTP :
     *  - CORS configuré via corsConfigurationSource()
     *  - CSRF désactivé (typique pour API stateless JWT)
     *  - Session stateless (PAS de session serveur — JWT stocké côté client)
     *  - Autorisations par pattern d'URL
     *  - Désactivation des frameOptions (nécessaire pour la console H2 en dev)
     *  - Ajout du filtre JWT avant UsernamePasswordAuthenticationFilter
     *
     * Remarques :
     *  - requestMatchers sont évalués dans l'ordre fourni ; soyez précis pour éviter des ouvertures accidentelles.
     *  - hasRole("ADMIN") vérifie la présence de l'autorité "ROLE_ADMIN" (Spring ajoute le préfixe ROLE_).
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            // CORS — permet au navigateur d'envoyer des requêtes depuis le frontend (ex : localhost:3000)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // CSRF désactivé : pour les APIs REST stateless (si vous utilisez des cookies d'auth, revoir)
            .csrf(csrf -> csrf.disable())
            // Utilisation du mode stateless (pas de session côté serveur)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Règles d'autorisation par URL
            .authorizeHttpRequests(authz -> authz
                // Endpoints publics (authentification, console H2 pour dev, actuator si nécessaire)
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                // Endpoints utilisateurs — seulement ADMIN peut accéder aux /users/**
                .requestMatchers("/users/**").hasRole("ADMIN")
                // Endpoints passagers — doit être authentifié (tout utilisateur connecté)
                .requestMatchers("/passengers/**").authenticated()
                // Toute autre requête nécessite une authentification
                .anyRequest().authenticated()
            )
            // Autoriser l'affichage de la console H2 dans un <frame> (utile en dev)
            .headers(headers -> headers.frameOptions().disable())
            // Placer le filtre JWT avant le filtre standard UsernamePasswordAuthenticationFilter
            // (pour que le contexte de sécurité soit initialisé à partir du token JWT si présent)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Construire et retourner la chaîne de filtres
        return http.build();
    }

    /**
     * CORS configuration source
     *
     * Définit les origines, méthodes et headers autorisés.
     * Points importants :
     *  - setAllowedOriginPatterns accepte des patterns (ex: http://localhost:*), utile en dev quand le port change.
     *  - setAllowedOrigins nécessite des origines exactes (ex: http://localhost:5173).
     *  - setAllowCredentials(true) : autorise l'envoi des cookies/credentials ; si activé, éviter "*" pour les origines.
     *  - setMaxAge : durée pendant laquelle la réponse préflight peut être mise en cache par le navigateur (en secondes).
     *
     * Attention :
     *  - En production, restreindre STRICTEMENT allowedOrigins aux domaines de ton frontend.
     *  - Si vous envoyez des cookies cross-site (avec allowCredentials=true), configurez les origines explicitement.
     */

    @Value("${frontend.urlOne}")
    private String frontendUrlOne;

    @Value("${frontend.urlTwo}")
    private String frontendUrlTwo;

    @Bean
    @Value("${frontend.url}")
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Autoriser les patterns (utile principalement en développement)

        configuration.setAllowedOrigins(List.of(frontendUrlOne, frontendUrlTwo));

        // Autoriser explicitement les origines de développement (exemples : Vite / Create React App)
        //configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:3000"));

        // Méthodes HTTP autorisées pour l'API
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Autoriser tous les headers (ou préciser la liste si besoin)
        configuration.setAllowedHeaders(List.of("*"));

        // Autoriser l'envoi des cookies / Authorization header depuis le navigateur
        configuration.setAllowCredentials(true);

        // Durée du cache des requêtes préflight (en secondes)
        configuration.setMaxAge(3600L);

        // Enregistrer cette configuration pour toutes les routes de l'application
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
