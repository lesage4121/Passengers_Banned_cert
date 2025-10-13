package com.PassBanned.API.security;

import io.jsonwebtoken.*; // Librairie JJWT pour créer et parser les JWT
import io.jsonwebtoken.security.Keys; // Utilitaire pour générer des clés sécurisées
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication; // Représente l’utilisateur authentifié
import org.springframework.security.core.userdetails.UserDetails; // Interface utilisateur de Spring Security
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey; // Clé secrète utilisée pour signer le token
import java.util.Date;

@Component // Indique à Spring que cette classe est un composant géré par le conteneur (Bean)
public class JwtTokenProvider {

    // La clé secrète utilisée pour signer et vérifier les JWT
    // Elle est injectée depuis application.properties (jwt.secret=...)
    @Value("${jwt.secret}")
    private String jwtSecret;

    // Durée de validité d’un token en millisecondes (ex: 3600000 = 1h)
    @Value("${jwt.expiration}")
    private int jwtExpirationInMs;

    // Méthode privée qui génère une clé HMAC à partir du secret configuré
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // Génération d’un token JWT à partir de l’authentification courante
    public String generateToken(Authentication authentication) {
        // Récupère l’utilisateur authentifié (principal = utilisateur courant)
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        // Calcule la date d’expiration du token
        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationInMs);

        // Construit et signe le token JWT
        return Jwts.builder()
                .setSubject(userPrincipal.getUsername()) // On met l’username dans le payload (subject)
                .setIssuedAt(new Date()) // Date de création du token
                .setExpiration(expiryDate) // Date d’expiration
                .signWith(getSigningKey()) // Signature avec la clé secrète
                .compact(); // Finalise et renvoie le token sous forme de String
    }

    // Récupère le username contenu dans un token JWT
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // Vérifie la signature avec la clé
                .build()
                .parseClaimsJws(token) // Parse le token JWT
                .getBody(); // Récupère le payload (Claims)

        return claims.getSubject(); // Retourne le "subject", c’est-à-dire l’username
    }

    // Valide un token JWT (signature, format, expiration, etc.)
    public boolean validateToken(String authToken) {
        try {
            // Essaie de parser le token et vérifier la signature
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(authToken);
            return true; // Si tout est ok, le token est valide
        } catch (SecurityException ex) {
            System.err.println("Invalid JWT signature"); // Signature invalide
        } catch (MalformedJwtException ex) {
            System.err.println("Invalid JWT token"); // Token mal formé
        } catch (ExpiredJwtException ex) {
            System.err.println("Expired JWT token"); // Token expiré
        } catch (UnsupportedJwtException ex) {
            System.err.println("Unsupported JWT token"); // Format non supporté
        } catch (IllegalArgumentException ex) {
            System.err.println("JWT claims string is empty"); // Payload vide
        }
        return false; // Si une exception est levée, le token n’est pas valide
    }
}
