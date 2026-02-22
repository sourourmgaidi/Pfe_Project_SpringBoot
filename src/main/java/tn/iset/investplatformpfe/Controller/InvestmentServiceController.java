package tn.iset.investplatformpfe.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.InvestmentService;
import tn.iset.investplatformpfe.Entity.ServiceStatus;
import tn.iset.investplatformpfe.Service.InvestmentServiceService;
import tn.iset.investplatformpfe.Service.PartenaireLocalAuthService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/investment-services")
public class InvestmentServiceController {

    private final InvestmentServiceService investmentService;
    private final PartenaireLocalAuthService partenaireLocalAuthService;

    public InvestmentServiceController(
            InvestmentServiceService investmentService,
            PartenaireLocalAuthService partenaireLocalAuthService) {
        this.investmentService = investmentService;
        this.partenaireLocalAuthService = partenaireLocalAuthService;
    }




    // ========================================
    // CREATE - Réservé aux LOCAL_PARTNER
    // ========================================
    @PostMapping
    public ResponseEntity<?> createInvestmentService(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody InvestmentService service) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les partenaires locaux peuvent créer des services d'investissement"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            InvestmentService created = investmentService.createInvestmentService(service, email);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // CREATE - Avec ID du fournisseur
    // ========================================
    @PostMapping("/with-provider/{providerId}")
    public ResponseEntity<?> createInvestmentServiceWithProvider(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long providerId,
            @RequestBody InvestmentService service) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les partenaires locaux peuvent créer des services d'investissement"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Map<String, Object> partenaire = partenaireLocalAuthService.getProfile(email);
            Long partenaireId = (Long) partenaire.get("id");

            if (!partenaireId.equals(providerId)) {
                return ResponseEntity.status(403).body(Map.of("error",
                        "Vous ne pouvez créer des services que pour votre propre compte"));
            }

            InvestmentService created = investmentService.createInvestmentServiceWithProvider(service, providerId);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // READ (PUBLIC)
    // ========================================
    @GetMapping
    public ResponseEntity<List<InvestmentService>> getAllInvestmentServices() {
        return ResponseEntity.ok(investmentService.getAllInvestmentServices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getInvestmentServiceById(@PathVariable Long id) {
        try {
            InvestmentService service = investmentService.getInvestmentServiceById(id);
            return ResponseEntity.ok(service);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<InvestmentService>> getByProvider(@PathVariable Long providerId) {
        return ResponseEntity.ok(investmentService.getInvestmentServicesByProviderId(providerId));
    }

    @GetMapping("/region/{regionId}")
    public ResponseEntity<List<InvestmentService>> getByRegion(@PathVariable Long regionId) {
        return ResponseEntity.ok(investmentService.getInvestmentServicesByRegionId(regionId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<InvestmentService>> getByStatus(@PathVariable ServiceStatus status) {
        return ResponseEntity.ok(investmentService.getInvestmentServicesByStatus(status));
    }

    @GetMapping("/rejected")
    public ResponseEntity<List<InvestmentService>> getRejected() {
        return ResponseEntity.ok(investmentService.getInvestmentServicesByStatus(ServiceStatus.REJECTED));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<InvestmentService>> getPending() {
        return ResponseEntity.ok(investmentService.getPendingInvestmentServices());
    }

    @GetMapping("/approved")
    public ResponseEntity<List<InvestmentService>> getApproved() {
        return ResponseEntity.ok(investmentService.getApprovedInvestmentServices());
    }

    @GetMapping("/active")
    public ResponseEntity<List<InvestmentService>> getActive() {
        return ResponseEntity.ok(investmentService.getActiveInvestmentServices());
    }

    @GetMapping("/zone/{zone}")
    public ResponseEntity<List<InvestmentService>> getByZone(@PathVariable String zone) {
        return ResponseEntity.ok(investmentService.getInvestmentServicesByZone(zone));
    }

    // ========================================
    // UPDATE - Propriétaire uniquement
    // ========================================
    @PutMapping("/{id}")
    public ResponseEntity<?> updateInvestmentService(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody InvestmentService serviceDetails) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les partenaires locaux peuvent modifier des services"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            InvestmentService updated = investmentService.updateInvestmentService(id, serviceDetails, email);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // ADMIN: Approbation
    // ========================================
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveService(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux administrateurs"));
        }

        try {
            InvestmentService approved = investmentService.approveInvestmentService(id);
            return ResponseEntity.ok(approved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectService(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux administrateurs"));
        }

        try {
            InvestmentService rejected = investmentService.rejectInvestmentService(id);
            return ResponseEntity.ok(rejected);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // INVESTOR: Marquer son intérêt
    // ========================================
    @PostMapping("/{serviceId}/interest/{investorId}")
    public ResponseEntity<?> markInterest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serviceId,
            @PathVariable Long investorId) {

        if (jwt == null || !hasRole(jwt, "INVESTOR")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux investisseurs"));
        }

        try {
            InvestmentService service = investmentService.markInterest(serviceId, investorId);
            return ResponseEntity.ok(service);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // DELETE
    // ========================================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteInvestmentService(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            boolean isAdmin = hasRole(jwt, "ADMIN");

            investmentService.deleteInvestmentService(id, email, isAdmin);
            return ResponseEntity.ok(Map.of("message", "Service supprimé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // RECHERCHE
    // ========================================
    @GetMapping("/search")
    public ResponseEntity<List<InvestmentService>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(investmentService.searchInvestmentServices(keyword));
    }

    @GetMapping("/advanced-search")
    public ResponseEntity<List<InvestmentService>> advancedSearch(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long sectorId,
            @RequestParam(required = false) ServiceStatus status,
            @RequestParam(required = false) BigDecimal maxAmount) {

        return ResponseEntity.ok(investmentService.advancedSearch(regionId, sectorId, status, maxAmount));
    }

    // ========================================
    // STATISTIQUES
    // ========================================
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(investmentService.getStatistics());
    }

    // ========================================
    // Méthode utilitaire pour vérifier les rôles
    // ========================================
    private boolean hasRole(Jwt jwt, String role) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles != null && roles.contains(role);
        }
        return false;
    }


}
