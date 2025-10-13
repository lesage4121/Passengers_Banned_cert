package com.PassBanned.API.exception;


import com.PassBanned.API.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Cette classe est un gestionnaire global d'exceptions.
 * Elle permet d'intercepter les erreurs levées dans toute l'application
 * et de retourner une réponse structurée au format JSON (basée sur ApiError).
 */
@ControllerAdvice // Permet d'appliquer les règles de gestion d'exceptions à tous les contrôleurs
public class GlobalExceptionHandler {

    /**
     * Méthode qui gère toutes les exceptions de type "Exception"
     * (c'est-à-dire la classe mère de toutes les exceptions Java).
     *
     * @param ex      l'exception interceptée
     * @param request la requête HTTP où l'erreur est survenue
     * @return un objet ApiError encapsulé dans une ResponseEntity avec le code 500 (Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllExceptions(Exception ex, HttpServletRequest request) {
        ApiError apiError = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),   // Code HTTP (500)
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), // Libellé standard du code HTTP ("Internal Server Error")
                ex.getMessage(),    // Message détaillé de l'erreur (venant de l'exception)
                request.getRequestURI() // Le chemin de l’API où l’erreur est survenue
        );

        // Retourne une réponse HTTP avec le body (apiError) et le status 500
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Méthode qui gère toutes les exceptions de type RuntimeException
     * (ex. NullPointerException, IllegalArgumentException, etc.).
     *
     * @param ex      l'exception interceptée
     * @param request la requête HTTP où l'erreur est survenue
     * @return un objet ApiError encapsulé dans une ResponseEntity avec le code 400 (Bad Request)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntimeExceptions(RuntimeException ex, HttpServletRequest request) {
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),   // Code HTTP (400)
                HttpStatus.BAD_REQUEST.getReasonPhrase(), // Libellé standard du code HTTP ("Bad Request")
                ex.getMessage(),    // Message détaillé de l'erreur
                request.getRequestURI() // Le chemin de l’API où l’erreur est survenue
        );

        // Retourne une réponse HTTP avec le body (apiError) et le status 400
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }
}
