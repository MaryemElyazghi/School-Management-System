package com.example.dto;

import com.example.entity.Enrollment;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * ✅ VERSION CORRIGÉE - EnrollmentDTO sans studentNumber
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDTO {
    private Long id;

    @NotNull(message = "Student ID is required")
    private Long studentId;

    private String studentName;
    // ✅ SUPPRIMÉ : studentNumber

    @NotNull(message = "Course ID is required")
    private Long courseId;

    private String courseName;
    private String courseCode;

    private LocalDate enrollmentDate;
    private Enrollment.EnrollmentStatus status;
    private Double grade;
}