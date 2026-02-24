package tn.iset.investplatformpfe.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.iset.investplatformpfe.Entity.Chat;
import tn.iset.investplatformpfe.Entity.Role;
import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    // Get conversation between two users
    @Query("SELECT c FROM Chat c WHERE " +
            "(c.senderType = :senderType AND c.senderId = :senderId AND c.receiverType = :receiverType AND c.receiverId = :receiverId AND c.deletedBySender = false) " +
            "OR (c.senderType = :receiverType AND c.senderId = :receiverId AND c.receiverType = :senderType AND c.receiverId = :senderId AND c.deletedByReceiver = false) " +
            "ORDER BY c.sentAt DESC")
    Page<Chat> findConversation(
            @Param("senderType") Role senderType,
            @Param("senderId") Long senderId,
            @Param("receiverType") Role receiverType,
            @Param("receiverId") Long receiverId,
            Pageable pageable);

    // Get all conversations for a user
    @Query("SELECT DISTINCT " +
            "CASE WHEN c.senderType = :userRole AND c.senderId = :userId THEN c.receiverType ELSE c.senderType END as otherRole, " +
            "CASE WHEN c.senderType = :userRole AND c.senderId = :userId THEN c.receiverId ELSE c.senderId END as otherId, " +
            "MAX(c.sentAt) as lastMessageDate " +
            "FROM Chat c " +
            "WHERE (c.senderType = :userRole AND c.senderId = :userId AND c.deletedBySender = false) " +
            "OR (c.receiverType = :userRole AND c.receiverId = :userId AND c.deletedByReceiver = false) " +
            "GROUP BY otherRole, otherId " +
            "ORDER BY lastMessageDate DESC")
    List<Object[]> findUserConversations(
            @Param("userRole") Role userRole,
            @Param("userId") Long userId);

    // Count unread messages
    @Query("SELECT COUNT(c) FROM Chat c WHERE " +
            "c.receiverType = :userRole AND c.receiverId = :userId " +
            "AND c.isRead = false AND c.deletedByReceiver = false")
    long countUnreadMessages(
            @Param("userRole") Role userRole,
            @Param("userId") Long userId);
}
