package com.example.repository;

import com.example.entity.DossierAdministratif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DossierAdministratifRepository extends JpaRepository<DossierAdministratif, Long> {
    Optional<DossierAdministratif> findByNumeroInscription(String numeroInscription);
    Optional<DossierAdministratif> findByStudentId(Long studentId);
    boolean existsByNumeroInscription(String numeroInscription);

    @Query("SELECT d FROM DossierAdministratif d LEFT JOIN FETCH d.student")
    List<DossierAdministratif> findAllWithRelations();
}