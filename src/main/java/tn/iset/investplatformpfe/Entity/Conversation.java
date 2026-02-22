package tn.iset.investplatformpfe.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String expediteurRole;

    @Column(nullable = false)
    private String expediteurEmail;

    @ManyToOne
    @JoinColumn(name = "partenaire_id", nullable = false)
    private PartenaireLocal partenaire;

    private String dernierMessage;

    private LocalDateTime dateDernierMessage;

    private boolean expediteurVu = true;
    private boolean partenaireVu = true;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Message> messages = new ArrayList<>();

    public Conversation() {}

    public Conversation(String expediteurRole, String expediteurEmail, PartenaireLocal partenaire) {
        this.expediteurRole = expediteurRole;
        this.expediteurEmail = expediteurEmail;
        this.partenaire = partenaire;
        this.dateDernierMessage = LocalDateTime.now();
        this.expediteurVu = true;      // ✅ AJOUTER CETTE LIGNE
        this.partenaireVu = false;     // ✅ AJOUTER CETTE LIGNE
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExpediteurRole() {
        return expediteurRole;
    }

    public void setExpediteurRole(String expediteurRole) {
        this.expediteurRole = expediteurRole;
    }

    public String getExpediteurEmail() {
        return expediteurEmail;
    }

    public void setExpediteurEmail(String expediteurEmail) {
        this.expediteurEmail = expediteurEmail;
    }

    public PartenaireLocal getPartenaire() {
        return partenaire;
    }

    public void setPartenaire(PartenaireLocal partenaire) {
        this.partenaire = partenaire;
    }

    public String getDernierMessage() {
        return dernierMessage;
    }

    public void setDernierMessage(String dernierMessage) {
        this.dernierMessage = dernierMessage;
    }

    public LocalDateTime getDateDernierMessage() {
        return dateDernierMessage;
    }

    public void setDateDernierMessage(LocalDateTime dateDernierMessage) {
        this.dateDernierMessage = dateDernierMessage;
    }

    public boolean isExpediteurVu() {
        return expediteurVu;
    }

    public void setExpediteurVu(boolean expediteurVu) {
        this.expediteurVu = expediteurVu;
    }

    public boolean isPartenaireVu() {
        return partenaireVu;
    }

    public void setPartenaireVu(boolean partenaireVu) {
        this.partenaireVu = partenaireVu;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
