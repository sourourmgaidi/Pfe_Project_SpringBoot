package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.iset.investplatformpfe.Entity.Conversation;
import tn.iset.investplatformpfe.Entity.Message;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationOrderByDateEnvoiAsc(Conversation conversation);

    List<Message> findByDestinataireEmailAndLuFalse(String destinataireEmail);

    @Modifying
    @Query("UPDATE Message m SET m.lu = true WHERE m.destinataireEmail = :destinataire AND m.conversation = :conversation")
    void marquerCommeLu(@Param("destinataire") String destinataire, @Param("conversation") Conversation conversation);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.destinataireEmail = :email AND m.lu = false")
    long countNonLusParDestinataire(@Param("email") String email);
}