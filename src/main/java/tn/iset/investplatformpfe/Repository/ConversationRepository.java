package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.iset.investplatformpfe.Entity.Conversation;
import tn.iset.investplatformpfe.Entity.PartenaireLocal;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // Pour INVESTISSEUR ou PARTENAIRE ECONOMIQUE : toutes ses conversations
    List<Conversation> findByExpediteurEmailOrderByDateDernierMessageDesc(String expediteurEmail);

    // Pour PARTENAIRE LOCAL : toutes ses conversations
    List<Conversation> findByPartenaireOrderByDateDernierMessageDesc(PartenaireLocal partenaire);

    // Trouver une conversation spécifique entre un expéditeur et un partenaire
    Optional<Conversation> findByExpediteurEmailAndPartenaire(String expediteurEmail, PartenaireLocal partenaire);

    // Rechercher des conversations pour un expéditeur (par nom du partenaire)
    @Query("SELECT c FROM Conversation c WHERE c.expediteurEmail = :email AND " +
            "(LOWER(c.partenaire.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.partenaire.prenom) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Conversation> rechercherConversationsExpediteur(@Param("email") String email, @Param("search") String search);
}