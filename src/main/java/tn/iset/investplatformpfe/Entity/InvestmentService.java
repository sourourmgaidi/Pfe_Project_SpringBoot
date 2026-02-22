package tn.iset.investplatformpfe.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "investment_services")
@Data
@NoArgsConstructor
@AllArgsConstructor  // ← Ajouter ceci
@Builder
public class InvestmentService {

    // ===============================
    // ID
    // ===============================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===============================
    // Informations principales (comme CollaborationService)
    // ===============================
    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private PartenaireLocal provider;  // Le partenaire local qui crée le service

    @Column(nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Availability availability;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @Column(name = "contact_person", nullable = false)
    private String contactPerson;

    // ===============================
    // Détails spécifiques à l'investissement
    // ===============================
    @Column(nullable = false)
    private String title;  // Titre de l'investissement

    private String type;  // "INVESTMENT" (sera forcé)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.PENDING;

    private String zone;  // Zone spécifique

    @ManyToOne
    @JoinColumn(name = "economic_sector_id")
    private EconomicSector economicSector;  // Secteur économique

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;  // Montant total

    @Column(name = "minimum_amount", precision = 15, scale = 2)
    private BigDecimal minimumAmount;  // Montant minimum

    @Column(name = "deadline_date")
    private LocalDate deadlineDate;  // Date limite

    @Column(name = "project_duration")
    private String projectDuration;  // Durée du projet

    @ElementCollection
    @CollectionTable(
            name = "investment_service_documents",
            joinColumns = @JoinColumn(name = "investment_service_id")
    )
    @Column(name = "document_url")
    private List<String> attachedDocuments;  // Documents joints

    @ManyToMany
    @JoinTable(
            name = "investment_service_interested_investors",
            joinColumns = @JoinColumn(name = "investment_service_id"),
            inverseJoinColumns = @JoinColumn(name = "investor_id")
    )
    private List<Investor> interestedInvestors;  // Investisseurs intéressés

    // ===============================
    // Champs système (comme CollaborationService)
    // ===============================
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.publicationDate == null) {
            this.publicationDate = LocalDate.now();
        }
        // Forcer le type à "INVESTMENT"
        this.type = "INVESTMENT";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public PartenaireLocal getProvider() {
        return provider;
    }

    public void setProvider(PartenaireLocal provider) {
        this.provider = provider;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Availability getAvailability() {
        return availability;
    }

    public void setAvailability(Availability availability) {
        this.availability = availability;
    }

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceStatus status) {
        this.status = status;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public EconomicSector getEconomicSector() {
        return economicSector;
    }

    public void setEconomicSector(EconomicSector economicSector) {
        this.economicSector = economicSector;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getMinimumAmount() {
        return minimumAmount;
    }

    public void setMinimumAmount(BigDecimal minimumAmount) {
        this.minimumAmount = minimumAmount;
    }

    public LocalDate getDeadlineDate() {
        return deadlineDate;
    }

    public void setDeadlineDate(LocalDate deadlineDate) {
        this.deadlineDate = deadlineDate;
    }

    public String getProjectDuration() {
        return projectDuration;
    }

    public void setProjectDuration(String projectDuration) {
        this.projectDuration = projectDuration;
    }

    public List<String> getAttachedDocuments() {
        return attachedDocuments;
    }

    public void setAttachedDocuments(List<String> attachedDocuments) {
        this.attachedDocuments = attachedDocuments;
    }

    public List<Investor> getInterestedInvestors() {
        return interestedInvestors;
    }

    public void setInterestedInvestors(List<Investor> interestedInvestors) {
        this.interestedInvestors = interestedInvestors;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}