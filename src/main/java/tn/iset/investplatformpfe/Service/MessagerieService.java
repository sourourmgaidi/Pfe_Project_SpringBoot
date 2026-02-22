package tn.iset.investplatformpfe.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessagerieService {

    private final MessageRepository messageRepo;
    private final ConversationRepository conversationRepo;
    private final InvestorRepository investorRepo;
    private final PartenaireEconomiqueRepository partenaireEcoRepo;
    private final PartenaireLocalRepository partenaireLocalRepo;

    public MessagerieService(MessageRepository messageRepo,
                             ConversationRepository conversationRepo,
                             InvestorRepository investorRepo,
                             PartenaireEconomiqueRepository partenaireEcoRepo,
                             PartenaireLocalRepository partenaireLocalRepo) {
        this.messageRepo = messageRepo;
        this.conversationRepo = conversationRepo;
        this.investorRepo = investorRepo;
        this.partenaireEcoRepo = partenaireEcoRepo;
        this.partenaireLocalRepo = partenaireLocalRepo;
    }

    /**
     * Rechercher des partenaires locaux (pour investisseur ou partenaire économique)
     */
    public List<Map<String, Object>> rechercherPartenairesLocaux(String recherche) {
        return partenaireLocalRepo.rechercherPartenaires(recherche).stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", p.getId());
                    map.put("nomComplet", p.getPrenom() + " " + p.getNom());
                    map.put("email", p.getEmail());
                    map.put("domaine", p.getDomaineActivite());
                    return map;
                })
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * Rechercher dans les conversations d'un expéditeur (investisseur ou partenaire éco)
     */
    public List<Conversation> rechercherConversationsExpediteur(String email, String recherche) {
        return conversationRepo.rechercherConversationsExpediteur(email, recherche);
    }

    /**
     * Envoyer un message (crée la conversation si elle n'existe pas)
     */
    @Transactional
    public Message envoyerMessage(String expediteurEmail, String partenaireEmail, String contenu, String role) {

        // Récupérer le partenaire local
        PartenaireLocal partenaire = partenaireLocalRepo.findByEmail(partenaireEmail)
                .orElseThrow(() -> new RuntimeException("Partenaire local non trouvé"));

        // Chercher la conversation existante OU en créer une nouvelle
        Conversation conversation = conversationRepo
                .findByExpediteurEmailAndPartenaire(expediteurEmail, partenaire)
                .orElseGet(() -> {
                    Conversation nouvelle = new Conversation(role, expediteurEmail, partenaire);
                    return conversationRepo.save(nouvelle);
                });

        // Créer et sauvegarder le message
        Message message = new Message(contenu, expediteurEmail, partenaireEmail, conversation);
        message = messageRepo.save(message);

        // Mettre à jour la conversation
        conversation.setDernierMessage(contenu);
        conversation.setDateDernierMessage(LocalDateTime.now());
        conversation.setExpediteurVu(true);
        conversation.setPartenaireVu(false);
        conversationRepo.save(conversation);

        return message;
    }

    /**
     * Partenaire local répond à un expéditeur
     */
    @Transactional
    public Message repondreMessage(String partenaireEmail, String expediteurEmail, String contenu) {

        PartenaireLocal partenaire = partenaireLocalRepo.findByEmail(partenaireEmail)
                .orElseThrow(() -> new RuntimeException("Partenaire local non trouvé"));

        Conversation conversation = conversationRepo
                .findByExpediteurEmailAndPartenaire(expediteurEmail, partenaire)
                .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));

        Message message = new Message(contenu, partenaireEmail, expediteurEmail, conversation);
        message = messageRepo.save(message);

        conversation.setDernierMessage(contenu);
        conversation.setDateDernierMessage(LocalDateTime.now());
        conversation.setExpediteurVu(false);
        conversation.setPartenaireVu(true);
        conversationRepo.save(conversation);

        return message;
    }

    /**
     * Récupérer une conversation complète
     */
    @Transactional
    public List<Message> getConversation(String monEmail, String autreEmail) {

        PartenaireLocal partenaire = partenaireLocalRepo.findByEmail(autreEmail)
                .orElseThrow(() -> new RuntimeException("Partenaire non trouvé"));

        Conversation conversation = conversationRepo
                .findByExpediteurEmailAndPartenaire(monEmail, partenaire)
                .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));

        // Marquer comme lu selon qui consulte
        if (conversation.getExpediteurEmail().equals(monEmail)) {
            messageRepo.marquerCommeLu(monEmail, conversation);
            conversation.setExpediteurVu(true);
        } else {
            messageRepo.marquerCommeLu(monEmail, conversation);
            conversation.setPartenaireVu(true);
        }

        conversationRepo.save(conversation);
        return messageRepo.findByConversationOrderByDateEnvoiAsc(conversation);
    }

    /**
     * Récupérer toutes les conversations d'un expéditeur (investisseur ou partenaire éco)
     */
    public List<Conversation> getConversationsExpediteur(String email) {
        return conversationRepo.findByExpediteurEmailOrderByDateDernierMessageDesc(email);
    }

    /**
     * Récupérer toutes les conversations d'un partenaire local
     */
    public List<Conversation> getConversationsPartenaireLocal(String email) {
        PartenaireLocal partenaire = partenaireLocalRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Partenaire local non trouvé"));
        return conversationRepo.findByPartenaireOrderByDateDernierMessageDesc(partenaire);
    }

    /**
     * Compter les messages non lus
     */
    public long countMessagesNonLus(String email) {
        return messageRepo.countNonLusParDestinataire(email);
    }

    /**
     * Récupérer la liste des messages non lus
     */
    public List<Message> getMessagesNonLus(String email) {
        return messageRepo.findByDestinataireEmailAndLuFalse(email);
    }

    /**
     * Vérifier si une conversation existe
     */
    public boolean conversationExiste(String expediteurEmail, String partenaireEmail) {
        PartenaireLocal partenaire = partenaireLocalRepo.findByEmail(partenaireEmail).orElse(null);
        if (partenaire == null) return false;
        return conversationRepo.findByExpediteurEmailAndPartenaire(expediteurEmail, partenaire).isPresent();
    }
}