package tn.iset.investplatformpfe.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Repository.InvestmentServiceRepository;
import tn.iset.investplatformpfe.Repository.PartenaireLocalRepository;
import tn.iset.investplatformpfe.Repository.InvestorRepository;
import tn.iset.investplatformpfe.Repository.RegionRepository;
import tn.iset.investplatformpfe.Repository.EconomicSectorRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Service
public class InvestmentServiceService {

    private final InvestmentServiceRepository investmentRepository;
    private final PartenaireLocalRepository partenaireLocalRepository;
    private final InvestorRepository investorRepository;
    private final RegionRepository regionRepository;
    private final EconomicSectorRepository economicSectorRepository;
    private final NotificationService notificationService;

    public InvestmentServiceService(
            InvestmentServiceRepository investmentRepository,
            PartenaireLocalRepository partenaireLocalRepository,
            InvestorRepository investorRepository,
            RegionRepository regionRepository,
            EconomicSectorRepository economicSectorRepository,
            NotificationService notificationService) {
        this.investmentRepository = investmentRepository;
        this.partenaireLocalRepository = partenaireLocalRepository;
        this.investorRepository = investorRepository;
        this.regionRepository = regionRepository;
        this.economicSectorRepository = economicSectorRepository;
        this.notificationService = notificationService;
    }

    // ========================================
    // CREATE - Par un partenaire local (avec email)
    // ========================================
    @Transactional
    public InvestmentService createInvestmentService(InvestmentService service, String emailPartenaire) {

        // Récupérer le partenaire local par email
        PartenaireLocal partenaire = partenaireLocalRepository.findByEmail(emailPartenaire)
                .orElseThrow(() -> new RuntimeException("Partenaire local non trouvé avec email: " + emailPartenaire));

        return createInvestmentServiceWithProvider(service, partenaire.getId());
    }

    // ========================================
    // CREATE - Avec ID du fournisseur
    // ========================================
    @Transactional
    public InvestmentService createInvestmentServiceWithProvider(InvestmentService service, Long providerId) {

        System.out.println("DÉBUT CRÉATION SERVICE D'INVESTISSEMENT");
        System.out.println("Provider ID reçu: " + providerId);

        // Récupérer le provider
        PartenaireLocal provider = partenaireLocalRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider non trouvé avec id: " + providerId));
        System.out.println("Provider trouvé: " + provider.getEmail());

        // Créer un nouvel objet
        InvestmentService newService = new InvestmentService();

        // Copier les propriétés de base
        newService.setName(service.getName());
        newService.setDescription(service.getDescription());

        // Vérifier et charger la région
        if (service.getRegion() != null && service.getRegion().getId() != null) {
            Region region = regionRepository.findById(service.getRegion().getId())
                    .orElseThrow(() -> new RuntimeException("Région non trouvée avec id: " + service.getRegion().getId()));
            newService.setRegion(region);
        }

        newService.setPrice(service.getPrice());
        newService.setAvailability(service.getAvailability());
        newService.setContactPerson(service.getContactPerson());

        // Copier les propriétés spécifiques
        newService.setTitle(service.getTitle());
        newService.setZone(service.getZone());

        // Vérifier et charger le secteur économique
        if (service.getEconomicSector() != null && service.getEconomicSector().getId() != null) {
            EconomicSector sector = economicSectorRepository.findById(service.getEconomicSector().getId())
                    .orElseThrow(() -> new RuntimeException("Secteur économique non trouvé avec id: " + service.getEconomicSector().getId()));
            newService.setEconomicSector(sector);
        }

        newService.setTotalAmount(service.getTotalAmount());
        newService.setMinimumAmount(service.getMinimumAmount());
        newService.setDeadlineDate(service.getDeadlineDate());
        newService.setProjectDuration(service.getProjectDuration());
        newService.setAttachedDocuments(service.getAttachedDocuments());

        // Assigner le provider
        newService.setProvider(provider);

        // Le type sera forcé à "INVESTMENT" par @PrePersist
        // Le statut est PENDING par défaut

        // Valider les champs obligatoires
        validateRequiredFields(newService);

        // Sauvegarder
        try {
            InvestmentService saved = investmentRepository.save(newService);
            System.out.println("Service d'investissement sauvegardé avec ID: " + saved.getId());

            // Notifier les admins
            notificationService.notifyAdminNewInvestmentService(saved);

            return saved;
        } catch (Exception e) {
            System.out.println("ERREUR lors de la sauvegarde: " + e.getMessage());
            throw new RuntimeException("Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    // ========================================
    // READ - Tous les services
    // ========================================
    public List<InvestmentService> getAllInvestmentServices() {
        return investmentRepository.findAll();
    }

    // ========================================
    // READ - Par ID
    // ========================================
    public InvestmentService getInvestmentServiceById(Long id) {
        return investmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service d'investissement non trouvé avec id: " + id));
    }

    // ========================================
    // READ - Par fournisseur
    // ========================================
    public List<InvestmentService> getInvestmentServicesByProviderId(Long providerId) {
        return investmentRepository.findByProviderId(providerId);
    }

    // ========================================
    // READ - Par région
    // ========================================
    public List<InvestmentService> getInvestmentServicesByRegionId(Long regionId) {
        return investmentRepository.findByRegionId(regionId);
    }

    // ========================================
    // READ - Par statut
    // ========================================
    public List<InvestmentService> getInvestmentServicesByStatus(ServiceStatus status) {
        return investmentRepository.findByStatus(status);
    }

    // ========================================
    // READ - Services en attente
    // ========================================
    public List<InvestmentService> getPendingInvestmentServices() {
        return investmentRepository.findByStatus(ServiceStatus.PENDING);
    }

    // ========================================
    // READ - Services approuvés
    // ========================================
    public List<InvestmentService> getApprovedInvestmentServices() {
        return investmentRepository.findByStatus(ServiceStatus.APPROVED);
    }

    // ========================================
    // READ - Par zone
    // ========================================
    public List<InvestmentService> getInvestmentServicesByZone(String zone) {
        return investmentRepository.findByZone(zone);
    }

    // ========================================
    // READ - Par montant maximum
    // ========================================
    public List<InvestmentService> getInvestmentServicesByMaxAmount(BigDecimal maxAmount) {
        return investmentRepository.findByMinimumAmountLessThanEqual(maxAmount);
    }

    // ========================================
    // READ - Services actifs (date limite non dépassée)
    // ========================================
    public List<InvestmentService> getActiveInvestmentServices() {
        return investmentRepository.findByDeadlineDateAfter(LocalDate.now());
    }

    // ========================================
    // UPDATE - Par le propriétaire
    // ========================================
    @Transactional
    public InvestmentService updateInvestmentService(Long id, InvestmentService serviceDetails, String emailPartenaire) {

        InvestmentService existingService = getInvestmentServiceById(id);

        // Vérifier que le service appartient au partenaire connecté
        PartenaireLocal partenaire = partenaireLocalRepository.findByEmail(emailPartenaire)
                .orElseThrow(() -> new RuntimeException("Partenaire non trouvé"));

        if (!existingService.getProvider().getId().equals(partenaire.getId())) {
            throw new RuntimeException("Vous ne pouvez modifier que vos propres services");
        }

        // Vérifier que le service est en attente
        if (existingService.getStatus() != ServiceStatus.PENDING) {
            throw new RuntimeException("Impossible de modifier un service qui n'est pas en attente");
        }

        // Mise à jour des champs
        if (serviceDetails.getName() != null) existingService.setName(serviceDetails.getName());
        if (serviceDetails.getDescription() != null) existingService.setDescription(serviceDetails.getDescription());
        if (serviceDetails.getRegion() != null) existingService.setRegion(serviceDetails.getRegion());
        if (serviceDetails.getPrice() != null) existingService.setPrice(serviceDetails.getPrice());
        if (serviceDetails.getAvailability() != null) existingService.setAvailability(serviceDetails.getAvailability());
        if (serviceDetails.getContactPerson() != null) existingService.setContactPerson(serviceDetails.getContactPerson());
        if (serviceDetails.getTitle() != null) existingService.setTitle(serviceDetails.getTitle());
        if (serviceDetails.getZone() != null) existingService.setZone(serviceDetails.getZone());
        if (serviceDetails.getEconomicSector() != null) existingService.setEconomicSector(serviceDetails.getEconomicSector());
        if (serviceDetails.getTotalAmount() != null) existingService.setTotalAmount(serviceDetails.getTotalAmount());
        if (serviceDetails.getMinimumAmount() != null) existingService.setMinimumAmount(serviceDetails.getMinimumAmount());
        if (serviceDetails.getDeadlineDate() != null) existingService.setDeadlineDate(serviceDetails.getDeadlineDate());
        if (serviceDetails.getProjectDuration() != null) existingService.setProjectDuration(serviceDetails.getProjectDuration());
        if (serviceDetails.getAttachedDocuments() != null) existingService.setAttachedDocuments(serviceDetails.getAttachedDocuments());

        return investmentRepository.save(existingService);
    }

    // ========================================
    // ADMIN: Approuver un service
    // ========================================
    @Transactional
    public InvestmentService approveInvestmentService(Long id) {
        InvestmentService service = getInvestmentServiceById(id);
        service.setStatus(ServiceStatus.APPROVED);
        InvestmentService approved = investmentRepository.save(service);

        // Notifier le partenaire
        notificationService.notifyLocalPartnerInvestmentApproved(approved);

        return approved;
    }

    // ========================================
    // ADMIN: Rejeter un service
    // ========================================
    @Transactional
    public InvestmentService rejectInvestmentService(Long id) {
        InvestmentService service = getInvestmentServiceById(id);
        service.setStatus(ServiceStatus.REJECTED);
        InvestmentService rejected = investmentRepository.save(service);

        // Notifier le partenaire
        notificationService.notifyLocalPartnerInvestmentRejected(rejected);

        return rejected;
    }

    // ========================================
    // INVESTOR: Marquer son intérêt
    // ========================================
    @Transactional
    public InvestmentService markInterest(Long serviceId, Long investorId) {
        InvestmentService service = getInvestmentServiceById(serviceId);
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new RuntimeException("Investisseur non trouvé"));

        if (service.getInterestedInvestors() == null) {
            service.setInterestedInvestors(new java.util.ArrayList<>());
        }

        boolean alreadyInterested = service.getInterestedInvestors().stream()
                .anyMatch(i -> i.getId().equals(investorId));

        if (!alreadyInterested) {
            service.getInterestedInvestors().add(investor);
        }

        return investmentRepository.save(service);
    }

    // ========================================
    // DELETE - Par le propriétaire ou admin
    // ========================================
    @Transactional
    public void deleteInvestmentService(Long id, String emailPartenaire, boolean isAdmin) {
        InvestmentService service = getInvestmentServiceById(id);

        if (!isAdmin) {
            PartenaireLocal partenaire = partenaireLocalRepository.findByEmail(emailPartenaire)
                    .orElseThrow(() -> new RuntimeException("Partenaire non trouvé"));

            if (!service.getProvider().getId().equals(partenaire.getId())) {
                throw new RuntimeException("Vous ne pouvez supprimer que vos propres services");
            }
        }

        investmentRepository.deleteById(id);
    }

    // ========================================
    // RECHERCHE
    // ========================================
    public List<InvestmentService> searchInvestmentServices(String keyword) {
        return investmentRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrTitleContainingIgnoreCase(
                keyword, keyword, keyword);
    }

    // ========================================
    // RECHERCHE AVANCÉE
    // ========================================
    public List<InvestmentService> advancedSearch(
            Long regionId, Long sectorId, ServiceStatus status, BigDecimal maxAmount) {
        return investmentRepository.advancedSearch(regionId, sectorId, status, maxAmount);
    }

    // ========================================
    // STATISTIQUES
    // ========================================
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", investmentRepository.count());
        stats.put("pending", investmentRepository.findByStatus(ServiceStatus.PENDING).size());
        stats.put("approved", investmentRepository.findByStatus(ServiceStatus.APPROVED).size());
        stats.put("rejected", investmentRepository.findByStatus(ServiceStatus.REJECTED).size());
        return stats;
    }

    // ========================================
    // VALIDATION
    // ========================================
    private void validateRequiredFields(InvestmentService service) {
        if (service.getName() == null || service.getName().trim().isEmpty()) {
            throw new RuntimeException("Le nom est obligatoire");
        }
        if (service.getTitle() == null || service.getTitle().trim().isEmpty()) {
            throw new RuntimeException("Le titre est obligatoire");
        }
        if (service.getRegion() == null) {
            throw new RuntimeException("La région est obligatoire");
        }
        if (service.getPrice() == null) {
            throw new RuntimeException("Le prix est obligatoire");
        }
        if (service.getAvailability() == null) {
            throw new RuntimeException("La disponibilité est obligatoire");
        }
        if (service.getContactPerson() == null || service.getContactPerson().trim().isEmpty()) {
            throw new RuntimeException("Le contact responsable est obligatoire");
        }
    }
}
