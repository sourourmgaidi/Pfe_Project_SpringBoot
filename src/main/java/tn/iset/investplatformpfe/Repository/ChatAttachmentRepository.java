package tn.iset.investplatformpfe.Repository;
import tn.iset.investplatformpfe.Entity.ChatAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, Long> {
    List<ChatAttachment> findByChatId(Long chatId);
}
