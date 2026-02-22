package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.iset.investplatformpfe.Entity.PartenaireLocal;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartenaireLocalRepository extends JpaRepository<PartenaireLocal, Long> {
    boolean existsByEmail(String email);
    Optional<PartenaireLocal> findByEmail(String email);
    @Query("SELECT p FROM PartenaireLocal p WHERE " +
            "LOWER(p.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.prenom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<PartenaireLocal> rechercherPartenaires(@Param("search") String search);
}
