package com.PassBanned.API.service;

import com.PassBanned.API.Model.BannedPassenger;
import com.PassBanned.API.Model.User;
import com.PassBanned.API.dto.BannedPassengerDto;
import com.PassBanned.API.repository.BannedPassengerRepository;
import com.PassBanned.API.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service   // Indique que cette classe est un "Service" Spring (logique métier)
@Transactional // Assure que toutes les méthodes de cette classe seront exécutées dans une transaction
public class BannedPassengerService {

    // Injection du repository pour accéder à la base de données des passagers bannis
    @Autowired
    private BannedPassengerRepository passengerRepository;

    // Injection du repository des utilisateurs (ex: pour savoir qui a créé le bannissement)
    @Autowired
    private UserRepository userRepository;

    // Récupérer tous les passagers bannis (sans pagination)
    public List<BannedPassengerDto> getAllPassengers() {
        return passengerRepository.findAll().stream()   // Récupère la liste complète depuis la BDD
                .map(this::convertToDto)                // Convertit chaque entité en DTO
                .collect(Collectors.toList());          // Transforme en liste
    }

    // Récupérer tous les passagers bannis avec pagination
    public Page<BannedPassengerDto> getAllPassengers(Pageable pageable) {
        return passengerRepository.findAll(pageable)    // Requête paginée en BDD
                .map(this::convertToDto);               // Conversion automatique en DTO
    }

    // Récupérer un passager par son ID
    public Optional<BannedPassengerDto> getPassengerById(Long id) {
        return passengerRepository.findById(id)         // Recherche par ID
                .map(this::convertToDto);               // Conversion en DTO si trouvé
    }

    // Récupérer un passager par son numéro de passeport
    public Optional<BannedPassengerDto> getPassengerByPassport(String passportNumber) {
        return passengerRepository.findByPassportNumber(passportNumber)
                .map(this::convertToDto);
    }

    // Créer un nouveau passager banni
    public BannedPassengerDto createPassenger(BannedPassengerDto passengerDto, String createdByEmail) {
        // Vérifie si un passager avec ce numéro de passeport existe déjà
        if (passengerRepository.existsByPassportNumber(passengerDto.getPassportNumber())) {
            throw new RuntimeException("Passenger with this passport number already exists");
        }

        // Récupère l'utilisateur qui a créé le bannissement
        User createdBy = userRepository.findByEmail(createdByEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Conversion du DTO en entité
        BannedPassenger passenger = convertToEntity(passengerDto);
        passenger.setCreatedBy(createdBy); // Associe le créateur

        // Sauvegarde en base
        BannedPassenger savedPassenger = passengerRepository.save(passenger);

        // Retourne le DTO correspondant
        return convertToDto(savedPassenger);
    }

    // Mettre à jour un passager existant
    public BannedPassengerDto updatePassenger(Long id, BannedPassengerDto passengerDto) {
        // Vérifie si le passager existe
        BannedPassenger passenger = passengerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Passenger not found"));

        // Vérifie que le nouveau numéro de passeport n'existe pas déjà
        if (!passenger.getPassportNumber().equals(passengerDto.getPassportNumber()) &&
                passengerRepository.existsByPassportNumber(passengerDto.getPassportNumber())) {
            throw new RuntimeException("Passenger with this passport number already exists");
        }

        // Met à jour les champs de l'entité avec les données du DTO
        updateEntityFromDto(passenger, passengerDto);

        // Sauvegarde en BDD
        BannedPassenger updatedPassenger = passengerRepository.save(passenger);

        return convertToDto(updatedPassenger);
    }

    // Supprimer un passager
    public void deletePassenger(Long id) {
        if (!passengerRepository.existsById(id)) {
            throw new RuntimeException("Passenger not found");
        }
        passengerRepository.deleteById(id);
    }

    // Recherche de passagers par mot-clé avec pagination
    public Page<BannedPassengerDto> searchPassengers(String query, Pageable pageable) {
        return passengerRepository.searchPassengers(query, pageable)
                .map(this::convertToDto);
    }

    // Filtrer les passagers par plusieurs critères (gravité, statut, durée du ban, nationalité...)
    public Page<BannedPassengerDto> findWithFilters(
            BannedPassenger.Severity severity,
            BannedPassenger.Status status,
            BannedPassenger.BanDuration banDuration,
            String nationality,
            Pageable pageable) {
        return passengerRepository.findWithFilters(severity, status, banDuration, nationality, pageable)
                .map(this::convertToDto);
    }

    // Récupérer les bannissements récents (ex: derniers 7 jours)
    public List<BannedPassengerDto> getRecentBans(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        return passengerRepository.findRecentBans(startDate).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Compter le nombre total de passagers bannis
    public long getTotalPassengers() {
        return passengerRepository.count();
    }

    // Compter le nombre de bannissements actifs
    public long getActiveBans() {
        return passengerRepository.countActiveBans();
    }

    // Compter le nombre de cas critiques
    public long getCriticalCases() {
        return passengerRepository.countCriticalCases();
    }

    // -------------------- Méthodes utilitaires de conversion --------------------

    // Convertit une entité BannedPassenger en DTO
    private BannedPassengerDto convertToDto(BannedPassenger passenger) {
        BannedPassengerDto dto = new BannedPassengerDto();
        dto.setId(passenger.getId());
        dto.setFirstName(passenger.getFirstName());
        dto.setLastName(passenger.getLastName());
        dto.setEmail(passenger.getEmail());
        dto.setPhone(passenger.getPhone());
        dto.setPassportNumber(passenger.getPassportNumber());
        dto.setNationality(passenger.getNationality());
        dto.setDateOfBirth(passenger.getDateOfBirth());
        dto.setBanReason(passenger.getBanReason());
        dto.setBanDate(passenger.getBanDate());
        dto.setBanDuration(passenger.getBanDuration());
        dto.setBanEndDate(passenger.getBanEndDate());
        dto.setSeverity(passenger.getSeverity());
        dto.setNotes(passenger.getNotes());
        dto.setStatus(passenger.getStatus());
        dto.setCreatedByName(passenger.getCreatedBy().getName());   // Infos du créateur
        dto.setCreatedByEmail(passenger.getCreatedBy().getEmail());
        dto.setCreatedAt(passenger.getCreatedAt());
        dto.setUpdatedAt(passenger.getUpdatedAt());
        return dto;
    }

    // Convertit un DTO en entité
    private BannedPassenger convertToEntity(BannedPassengerDto dto) {
        BannedPassenger passenger = new BannedPassenger();
        updateEntityFromDto(passenger, dto); // Réutilise la méthode de mise à jour
        return passenger;
    }

    // Met à jour une entité avec les données d’un DTO
    private void updateEntityFromDto(BannedPassenger passenger, BannedPassengerDto dto) {
        passenger.setFirstName(dto.getFirstName());
        passenger.setLastName(dto.getLastName());
        passenger.setEmail(dto.getEmail());
        passenger.setPhone(dto.getPhone());
        passenger.setPassportNumber(dto.getPassportNumber());
        passenger.setNationality(dto.getNationality());
        passenger.setDateOfBirth(dto.getDateOfBirth());
        passenger.setBanReason(dto.getBanReason());
        passenger.setBanDate(dto.getBanDate());
        passenger.setBanDuration(dto.getBanDuration());
        passenger.setBanEndDate(dto.getBanEndDate());
        passenger.setSeverity(dto.getSeverity());
        passenger.setNotes(dto.getNotes());
        passenger.setStatus(dto.getStatus());
    }
}
