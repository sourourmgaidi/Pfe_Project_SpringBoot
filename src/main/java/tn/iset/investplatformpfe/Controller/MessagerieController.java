package tn.iset.investplatformpfe.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.Message;
import tn.iset.investplatformpfe.Entity.Conversation;
import tn.iset.investplatformpfe.Service.MessagerieService;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/messagerie")
public class MessagerieController {

    private final MessagerieService messagerieService;

    public MessagerieController(MessagerieService messagerieService) {
        this.messagerieService = messagerieService;
    }

    // ==================== POUR TOUS LES EXPÉDITEURS (INVESTOR, PARTNER) ====================

    /**
     * Rechercher des partenaires locaux
     */
    @GetMapping("/rechercher-partenaires-locaux")
    public ResponseEntity<?> rechercherPartenairesLocaux(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String q) {

        if (!hasAnyRole(jwt, "INVESTOR", "PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("erreur", "Accès réservé"));
        }

        if (q.length() < 2) {
            return ResponseEntity.badRequest().body(Map.of("erreur", "Minimum 2 caractères"));
        }

        return ResponseEntity.ok(messagerieService.rechercherPartenairesLocaux(q));
    }

    /**
     * Rechercher dans ses conversations
     */
    @GetMapping("/rechercher-conversations")
    public ResponseEntity<?> rechercherConversations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String q) {

        if (!hasAnyRole(jwt, "INVESTOR", "PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("erreur", "Accès réservé"));
        }

        String email = jwt.getClaimAsString("email");
        return ResponseEntity.ok(messagerieService.rechercherConversationsExpediteur(email, q));
    }

    /**
     * Envoyer un message à un partenaire local (crée la conversation automatiquement)
     */
    @PostMapping("/envoyer")
    public ResponseEntity<?> envoyerMessage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> request) {

        String role = getRole(jwt);
        if (!hasAnyRole(jwt, "INVESTOR", "PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("erreur", "Accès réservé"));
        }

        String expediteurEmail = jwt.getClaimAsString("email");
        String partenaireEmail = request.get("partenaireEmail");
        String contenu = request.get("contenu");

        if (contenu == null || contenu.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("erreur", "Message vide"));
        }

        try {
            Message message = messagerieService.envoyerMessage(expediteurEmail, partenaireEmail, contenu, role);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        }
    }

    /**
     * Voir ses conversations
     */
    @GetMapping("/mes-conversations")
    public ResponseEntity<?> getMesConversations(@AuthenticationPrincipal Jwt jwt) {
        if (!hasAnyRole(jwt, "INVESTOR", "PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("erreur", "Accès réservé"));
        }

        String email = jwt.getClaimAsString("email");
        return ResponseEntity.ok(messagerieService.getConversationsExpediteur(email));
    }

    // ==================== POUR LES PARTENAIRES LOCAUX ====================

    /**
     * Partenaire local : voir ses conversations
     */
    @GetMapping("/partenaire-local/mes-conversations")
    public ResponseEntity<?> getConversationsPartenaireLocal(@AuthenticationPrincipal Jwt jwt) {
        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("erreur", "Accès réservé aux partenaires locaux"));
        }

        String email = jwt.getClaimAsString("email");
        return ResponseEntity.ok(messagerieService.getConversationsPartenaireLocal(email));
    }

    /**
     * Partenaire local : répondre à un message
     */
    @PostMapping("/partenaire-local/repondre")
    public ResponseEntity<?> repondreMessage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> request) {

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("erreur", "Accès réservé aux partenaires locaux"));
        }

        String partenaireEmail = jwt.getClaimAsString("email");
        String expediteurEmail = request.get("expediteurEmail");
        String contenu = request.get("contenu");

        if (contenu == null || contenu.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("erreur", "Message vide"));
        }

        try {
            Message message = messagerieService.repondreMessage(partenaireEmail, expediteurEmail, contenu);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        }
    }

    // ==================== POUR TOUS ====================

    /**
     * Voir une conversation spécifique
     */
    @GetMapping("/conversation/{autreEmail}")
    public ResponseEntity<?> getConversation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String autreEmail) {

        String monEmail = jwt.getClaimAsString("email");
        List<Message> messages = messagerieService.getConversation(monEmail, autreEmail);
        return ResponseEntity.ok(messages);
    }

    /**
     * Voir les messages non lus
     */
    @GetMapping("/non-lus")
    public ResponseEntity<?> getMessagesNonLus(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("erreur", "Non authentifié"));
        }

        String email = jwt.getClaimAsString("email");

        long count = messagerieService.countMessagesNonLus(email);
        List<Message> messagesNonLus = messagerieService.getMessagesNonLus(email);

        Map<String, Object> response = new HashMap<>();
        response.put("nonLus", count);
        response.put("messages", messagesNonLus);

        return ResponseEntity.ok(response);
    }

    // ==================== UTILITAIRES ====================

    private boolean hasRole(Jwt jwt, String role) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles != null && roles.contains(role);
        }
        return false;
    }

    private boolean hasAnyRole(Jwt jwt, String... roles) {
        for (String role : roles) {
            if (hasRole(jwt, role)) return true;
        }
        return false;
    }

    private String getRole(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles != null && !roles.isEmpty()) {
                return roles.get(0); // Retourne le premier rôle
            }
        }
        return null;
    }
}