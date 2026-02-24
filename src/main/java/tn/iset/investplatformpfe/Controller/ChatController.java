package tn.iset.investplatformpfe.Controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.iset.investplatformpfe.DTO.MessageDTO;
import tn.iset.investplatformpfe.Entity.Chat;
import tn.iset.investplatformpfe.Entity.ChatAttachment;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Service.ChatService;
import tn.iset.investplatformpfe.Service.UserService;
import tn.iset.investplatformpfe.Repository.ChatAttachmentRepository;
import tn.iset.investplatformpfe.Repository.ChatRepository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;
    private final ChatAttachmentRepository chatAttachmentRepository;
    private final ChatRepository chatRepository;

    public ChatController(ChatService chatService,
                          UserService userService,
                          ChatAttachmentRepository chatAttachmentRepository,
                          ChatRepository chatRepository) {
        this.chatService = chatService;
        this.userService = userService;
        this.chatAttachmentRepository = chatAttachmentRepository;
        this.chatRepository = chatRepository;
    }

    // ========================================
    // ENVOYER MESSAGE SIMPLE (SANS FICHIER)
    // ========================================
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Object> payload) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            Role senderRole = extractRole(jwt);
            Long senderId = userService.getUserIdByEmailAndRole(
                    jwt.getClaimAsString("email"), senderRole);

            Role receiverRole = Role.valueOf((String) payload.get("receiverRole"));
            Long receiverId = Long.valueOf(payload.get("receiverId").toString());
            String content = (String) payload.get("content");

            Chat chat = chatService.sendMessage(
                    senderRole, senderId, receiverRole, receiverId, content);

            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // ENVOYER MESSAGE AVEC FICHIERS
    // ========================================
    @PostMapping(value = "/send-with-attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sendMessageWithAttachments(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("receiverRole") String receiverRoleStr,
            @RequestParam("receiverId") Long receiverId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "attachments", required = false) MultipartFile[] attachments) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            Role senderRole = extractRole(jwt);
            Long senderId = userService.getUserIdByEmailAndRole(
                    jwt.getClaimAsString("email"), senderRole);

            Role receiverRole = Role.valueOf(receiverRoleStr);

            Chat chat = chatService.sendMessageWithAttachments(
                    senderRole, senderId, receiverRole, receiverId, content, attachments);

            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // VOIR CONVERSATION STYLE MESSENGER (avec fichiers)
    // ========================================
    @GetMapping("/conversation/messenger/{targetRole}/{targetId}")
    public ResponseEntity<?> getConversationAsMessenger(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Role targetRole,
            @PathVariable Long targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            Role userRole = extractRole(jwt);
            Long userId = userService.getUserIdByEmailAndRole(
                    jwt.getClaimAsString("email"), userRole);

            Page<MessageDTO> conversation = chatService.getConversationAsMessenger(
                    userRole, userId, targetRole, targetId, page, size);

            return ResponseEntity.ok(conversation);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // VOIR CONVERSATION SIMPLE
    // ========================================
    @GetMapping("/conversation/{targetRole}/{targetId}")
    public ResponseEntity<?> getConversation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Role targetRole,
            @PathVariable Long targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            Role userRole = extractRole(jwt);
            Long userId = userService.getUserIdByEmailAndRole(
                    jwt.getClaimAsString("email"), userRole);

            Page<Chat> conversation = chatService.getConversation(
                    userRole, userId, targetRole, targetId, page, size);

            return ResponseEntity.ok(conversation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // VOIR LES FICHIERS D'UN MESSAGE
    // ========================================
    @GetMapping("/{chatId}/attachments")
    public ResponseEntity<?> getChatAttachments(@PathVariable Long chatId) {
        try {
            List<ChatAttachment> attachments = chatAttachmentRepository.findByChatId(chatId);
            return ResponseEntity.ok(attachments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // TÉLÉCHARGER UN FICHIER
    // ========================================
    @GetMapping("/attachment/{attachmentId}")
    public ResponseEntity<?> downloadAttachment(@PathVariable Long attachmentId) {
        try {
            ChatAttachment attachment = chatAttachmentRepository.findById(attachmentId)
                    .orElseThrow(() -> new RuntimeException("Attachment non trouvé"));

            Path filePath = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + attachment.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(attachment.getFileType()))
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // MARQUER UN MESSAGE COMME LU
    // ========================================
    @PutMapping("/{messageId}/read")
    public ResponseEntity<?> markAsRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long messageId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            Role userRole = extractRole(jwt);
            Long userId = userService.getUserIdByEmailAndRole(
                    jwt.getClaimAsString("email"), userRole);

            chatService.markAsRead(messageId, userRole, userId);
            return ResponseEntity.ok(Map.of("message", "Message marqué comme lu"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // SUPPRIMER UN MESSAGE POUR SOI
    // ========================================
    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> deleteForMe(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long messageId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            Role userRole = extractRole(jwt);
            Long userId = userService.getUserIdByEmailAndRole(
                    jwt.getClaimAsString("email"), userRole);

            chatService.deleteForMe(messageId, userRole, userId);
            return ResponseEntity.ok(Map.of("message", "Message supprimé"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // COMPTER LES MESSAGES NON LUS
    // ========================================
    @GetMapping("/unread/count")
    public ResponseEntity<?> countUnread(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            Role userRole = extractRole(jwt);
            Long userId = userService.getUserIdByEmailAndRole(
                    jwt.getClaimAsString("email"), userRole);

            long count = chatService.countUnreadMessages(userRole, userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // LISTER TOUTES LES CONVERSATIONS D'UN UTILISATEUR
    // ========================================
    @GetMapping("/conversations")
    public ResponseEntity<?> getUserConversations(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            Role userRole = extractRole(jwt);
            Long userId = userService.getUserIdByEmailAndRole(
                    jwt.getClaimAsString("email"), userRole);

            List<Object[]> conversations = chatRepository.findUserConversations(userRole, userId);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // EXTRAIRE LE RÔLE DU JWT
    // ========================================
    private Role extractRole(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles != null) {
                if (roles.contains("ADMIN")) return Role.ADMIN;
                if (roles.contains("LOCAL_PARTNER")) return Role.LOCAL_PARTNER;
                if (roles.contains("PARTNER")) return Role.PARTNER;
                if (roles.contains("INTERNATIONAL_COMPANY")) return Role.INTERNATIONAL_COMPANY;
                if (roles.contains("INVESTOR")) return Role.INVESTOR;
                if (roles.contains("TOURIST")) return Role.TOURIST;
            }
        }
        throw new RuntimeException("Rôle non trouvé dans le token");
    }
}