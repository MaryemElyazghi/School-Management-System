package com.example.repository;

import com.example.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ✅ EnrollmentRepository - VERSION CORRIGÉE
 *
 * Hérite de JpaRepository qui fournit automatiquement:
 * - save(), delete(), flush(), etc.
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentId(Long studentId);
    List<Enrollment> findByCourseId(Long courseId);

    // Fetch enrollments with student data eagerly loaded for course details page
    @Query("SELECT e FROM Enrollment e JOIN FETCH e.student WHERE e.course.id = :courseId")
    List<Enrollment> findByCourseIdWithStudent(@Param("courseId") Long courseId);

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

    // ✅ SUPPRIMÉ: deleteByStudentId - on utilisera deleteAll() à la place
    // car deleteByStudentId peut causer des problèmes avec Spring Data JPA
    // La méthode deleteAll() de JpaRepository est plus fiable
}