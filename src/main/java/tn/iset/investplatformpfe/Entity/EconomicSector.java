package tn.iset.investplatformpfe.Entity;


import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "economic_sectors")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EconomicSector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;  // Agriculture, Tourism, Technology, etc.

    @Column(length = 500)
    private String description;

    // Pour les services d'investissement dans ce secteur
    @OneToMany(mappedBy = "economicSector")
    private List<InvestmentService> investmentServices;

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
}