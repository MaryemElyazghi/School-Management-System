package com.example.repository;

import com.example.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudentId(Long studentId);
    List<Enrollment> findByCourseId(Long courseId);

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.status = :status")
    List<Enrollment> findByStudentIdAndStatus(
            @Param("studentId") Long studentId,
            @Param("status") Enrollment.EnrollmentStatus status
    );

    long countByCourseId(Long courseId);

    // ✅ Compter les inscriptions actives d'un élève
    long countByStudentIdAndStatus(Long studentId, Enrollment.EnrollmentStatus status);

    // ✅ Supprimer toutes les inscriptions d'un élève
    void deleteByStudentId(Long studentId);
}