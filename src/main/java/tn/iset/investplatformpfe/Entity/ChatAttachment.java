package tn.iset.investplatformpfe.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_attachments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType; // MIME type (image/jpeg, application/pdf, etc.)

    @Column(name = "file_size", nullable = false)
    private Long fileSize; // in bytes

    @Column(name = "file_path", nullable = false)
    private String filePath; // storage path on server

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;
}
