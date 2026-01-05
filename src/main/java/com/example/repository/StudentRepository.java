package com.example.repository;

import com.example.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ✅ VERSION CORRIGÉE - StudentRepository sans studentNumber
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByEmail(String email);
    Optional<Student> findByUserId(Long userId);
    List<Student> findByDepartmentId(Long departmentId);

    /**
     * ✅ CORRECTION : Recherche mise à jour sans studentNumber
     * Recherche par : prénom, nom, email, ou ID
     */
    @Query("SELECT s FROM Student s WHERE " +
            "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "CAST(s.id AS string) LIKE CONCAT('%', :keyword, '%')")
    List<Student> searchStudents(@Param("keyword") String keyword);
}