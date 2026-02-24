package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.iset.investplatformpfe.DTO.AttachmentDTO;
import tn.iset.investplatformpfe.DTO.MessageDTO;
import tn.iset.investplatformpfe.Entity.Chat;
import tn.iset.investplatformpfe.Entity.ChatAttachment;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Repository.ChatRepository;
import tn.iset.investplatformpfe.Repository.ChatAttachmentRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatAttachmentRepository chatAttachmentRepository;

    @Value("${file.upload-dir:uploads/chat}")
    private String uploadDir;

    public ChatService(ChatRepository chatRepository, ChatAttachmentRepository chatAttachmentRepository) {
        this.chatRepository = chatRepository;
        this.chatAttachmentRepository = chatAttachmentRepository;
    }

    // ========================================
    // ENVOYER MESSAGE SIMPLE (SANS FICHIER)
    // ========================================
    @Transactional
    public Chat sendMessage(Role senderType, Long senderId,
                            Role receiverType, Long receiverId,
                            String content) {
        Chat chat = Chat.builder()
                .senderType(senderType)
                .senderId(senderId)
                .receiverType(receiverType)
                .receiverId(receiverId)
                .content(content)
                .isRead(false)
                .deletedBySender(false)
                .deletedByReceiver(false)
                .build();
        return chatRepository.save(chat);
    }

    // ========================================
    // ENVOYER MESSAGE AVEC FICHIERS
    // ========================================
    @Transactional
    public Chat sendMessageWithAttachments(Role senderType, Long senderId,
                                           Role receiverType, Long receiverId,
                                           String content,
                                           MultipartFile[] attachments) throws IOException {

        Chat chat = Chat.builder()
                .senderType(senderType)
                .senderId(senderId)
                .receiverType(receiverType)
                .receiverId(receiverId)
                .content(content)
                .isRead(false)
                .deletedBySender(false)
                .deletedByReceiver(false)
                .build();

        Chat savedChat = chatRepository.save(chat);
        System.out.println("✅ Message sauvegardé avec ID: " + savedChat.getId());

        if (attachments != null && attachments.length > 0) {
            for (MultipartFile file : attachments) {
                if (!file.isEmpty()) {
                    saveAttachment(savedChat, file);
                }
            }
            System.out.println("✅ " + attachments.length + " fichier(s) attaché(s)");
        }

        return savedChat;
    }

    // ========================================
    // SAUVEGARDER UN FICHIER
    // ========================================
    private void saveAttachment(Chat chat, MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            System.out.println("📁 Dossier créé: " + uploadPath);
        }

        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(uniqueFileName);

        Files.copy(file.getInputStream(), filePath);
        System.out.println("💾 Fichier sauvegardé: " + filePath);

        ChatAttachment attachment = ChatAttachment.builder()
                .fileName(originalFileName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .filePath(filePath.toString())
                .uploadedAt(LocalDateTime.now())
                .chat(chat)
                .build();

        chatAttachmentRepository.save(attachment);
        System.out.println("✅ Attachment enregistré en BDD");
    }

    // ========================================
    // RÉCUPÉRER UNE CONVERSATION
    // ========================================
    public Page<Chat> getConversation(Role user1Type, Long user1Id,
                                      Role user2Type, Long user2Id,
                                      int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        return chatRepository.findConversation(user1Type, user1Id, user2Type, user2Id, pageable);
    }

    // ========================================
    // MARQUER COMME LU
    // ========================================
    @Transactional
    public void markAsRead(Long messageId, Role userType, Long userId) {
        Chat chat = chatRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message non trouvé"));
        if (chat.getReceiverType() == userType && chat.getReceiverId().equals(userId)) {
            chat.setIsRead(true);
            chatRepository.save(chat);
        }
    }

    // ========================================
    // SUPPRIMER POUR SOI
    // ========================================
    @Transactional
    public void deleteForMe(Long messageId, Role userType, Long userId) {
        Chat chat = chatRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message non trouvé"));
        if (chat.getSenderType() == userType && chat.getSenderId().equals(userId)) {
            chat.setDeletedBySender(true);
        } else if (chat.getReceiverType() == userType && chat.getReceiverId().equals(userId)) {
            chat.setDeletedByReceiver(true);
        }
        chatRepository.save(chat);
    }

    // ========================================
    // COMPTER MESSAGES NON LUS
    // ========================================
    public long countUnreadMessages(Role userType, Long userId) {
        return chatRepository.countUnreadMessages(userType, userId);
    }

    // ========================================
    // CONVERSATION STYLE MESSENGER (AVEC FICHIERS)
    // ========================================
    public Page<MessageDTO> getConversationAsMessenger(Role user1Type, Long user1Id,
                                                       Role user2Type, Long user2Id,
                                                       int page, int size) {

        System.out.println("📱 Récupération conversation style Messenger");

        Page<Chat> chats = getConversation(user1Type, user1Id, user2Type, user2Id, page, size);

        return chats.map(chat -> {
            MessageDTO dto = new MessageDTO();

            dto.setId(chat.getId());
            dto.setContent(chat.getContent());
            dto.setRead(chat.getIsRead());  // ✅ Utilisation du getter Lombok
            dto.setSentAt(chat.getSentAt());
            dto.setSenderType(chat.getSenderType());
            dto.setSenderId(chat.getSenderId());
            dto.setReceiverType(chat.getReceiverType());
            dto.setReceiverId(chat.getReceiverId());

            List<AttachmentDTO> attachmentDTOs = chat.getAttachments().stream()
                    .map(att -> {
                        AttachmentDTO attDto = new AttachmentDTO();
                        attDto.setId(att.getId());
                        attDto.setFileName(att.getFileName());
                        attDto.setFileType(att.getFileType());
                        attDto.setFileSize(att.getFileSize());
                        attDto.setDownloadUrl("/api/chat/attachment/" + att.getId());
                        return attDto;
                    })
                    .collect(Collectors.toList());

            dto.setAttachments(attachmentDTOs);

            return dto;
        });
    }
}