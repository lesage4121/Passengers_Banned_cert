package com.PassBanned.API.security;


import com.PassBanned.API.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component // Indique que cette classe est un composant Spring géré par le conteneur (injectable).
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // Cette classe est un filtre qui s’exécute à chaque requête HTTP (une seule fois par requête).

    @Autowired
    private JwtTokenProvider tokenProvider; 
    // Classe utilitaire (non fournie ici) qui contient la logique de génération et de validation des JWT.

    @Autowired
    private UserService userService;
    // Service qui permet de charger les informations utilisateur (implémente UserDetailsService).

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Cette méthode est appelée automatiquement à chaque requête.
        // Elle vérifie la présence d'un JWT et configure l'authentification si valide.

        try {
            // 1. Récupérer le token JWT dans l’en-tête "Authorization"
            String jwt = getJwtFromRequest(request);

            // 2. Vérifier que le token existe et est valide
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // 3. Extraire le nom d’utilisateur depuis le JWT
                String username = tokenProvider.getUsernameFromToken(jwt);

                // 4. Charger les informations de l’utilisateur depuis la base de données
                UserDetails userDetails = userService.loadUserByUsername(username);

                // 5. Créer un objet d’authentification avec les rôles de l’utilisateur
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails,       // L’utilisateur authentifié
                        null,              // Pas de mot de passe requis ici (déjà validé via le JWT)
                        userDetails.getAuthorities() // Les rôles/permissions de l’utilisateur
                    );

                // 6. Associer les détails de la requête (adresse IP, session, etc.)
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 7. Enregistrer l’authentification dans le contexte de sécurité de Spring
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // En cas d’erreur (ex: token invalide), on log mais on n’empêche pas la requête de continuer
            logger.error("Could not set user authentication in security context", ex);
        }

        // 8. Continuer la chaîne des filtres (indispensable pour que la requête atteigne le contrôleur)
        filterChain.doFilter(request, response);
    }

    // Méthode utilitaire qui récupère le JWT depuis l’en-tête Authorization
    private String getJwtFromRequest(HttpServletRequest request) {
        // Exemple attendu : "Authorization: Bearer <jwt_token>"
        String bearerToken = request.getHeader("Authorization");

        // Vérifie que l’en-tête existe, n’est pas vide et commence bien par "Bearer "
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Retourne le token sans le préfixe "Bearer "
            return bearerToken.substring(7);
        }
        return null; // Pas de token trouvé
    }
}
