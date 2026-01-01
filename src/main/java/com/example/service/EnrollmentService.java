package com.example.service;

import com.example.dto.EnrollmentDTO;
import com.example.entity.*;
import com.example.exception.BusinessException;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.CourseRepository;
import com.example.repository.EnrollmentRepository;
import com.example.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    /**
     * RÈGLE MÉTIER PRINCIPALE :
     * Un étudiant ne peut s'inscrire QU'AUX COURS DE SA FILIÈRE
     */
    public EnrollmentDTO enrollStudent(Long studentId, Long courseId) {
        // 1. Récupérer l'étudiant
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        // 2. Récupérer le cours
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        // 3. VALIDATION : Le cours doit appartenir à la filière de l'étudiant
        if (!course.getDepartment().getId().equals(student.getDepartment().getId())) {
            throw new BusinessException(
                    String.format("Student from %s department cannot enroll in %s department course",
                            student.getDepartment().getName(),
                            course.getDepartment().getName())
            );
        }

        // 4. VALIDATION : Vérifier si déjà inscrit
        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new BusinessException("Student is already enrolled in this course");
        }

        // 5. VALIDATION : Vérifier si le cours est plein
        if (course.isFull()) {
            throw new BusinessException(
                    String.format("Course %s is full (maximum %d students)",
                            course.getName(), course.getMaxStudents())
            );
        }

        // 6. Créer l'inscription
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setStatus(Enrollment.EnrollmentStatus.ACTIVE);

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        return convertToDTO(savedEnrollment);
    }

    /**
     * Retourner (abandon) un cours
     */
    public EnrollmentDTO dropCourse(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", enrollmentId));

        if (enrollment.getStatus() == Enrollment.EnrollmentStatus.COMPLETED) {
            throw new BusinessException("Cannot drop a completed course");
        }

        enrollment.setStatus(Enrollment.EnrollmentStatus.DROPPED);
        Enrollment updated = enrollmentRepository.save(enrollment);

        return convertToDTO(updated);
    }

    /**
     * Obtenir toutes les inscriptions d'un étudiant
     */
    public List<EnrollmentDTO> getStudentEnrollments(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtenir les inscriptions actives d'un étudiant
     */
    public List<EnrollmentDTO> getActiveStudentEnrollments(Long studentId) {
        return enrollmentRepository.findByStudentIdAndStatus(
                        studentId,
                        Enrollment.EnrollmentStatus.ACTIVE
                )
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtenir tous les étudiants inscrits à un cours
     */
    public List<EnrollmentDTO> getCourseEnrollments(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Mettre à jour le statut d'une inscription
     */
    public EnrollmentDTO updateEnrollmentStatus(Long enrollmentId, Enrollment.EnrollmentStatus status) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", enrollmentId));

        enrollment.setStatus(status);
        Enrollment updated = enrollmentRepository.save(enrollment);

        return convertToDTO(updated);
    }

    /**
     * Attribuer une note
     */
    public EnrollmentDTO assignGrade(Long enrollmentId, Double grade) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", enrollmentId));

        if (grade < 0 || grade > 20) {
            throw new BusinessException("Grade must be between 0 and 20");
        }

        enrollment.setGrade(grade);

        // Si la note est >= 10, marquer comme COMPLETED, sinon FAILED
        if (grade >= 10) {
            enrollment.setStatus(Enrollment.EnrollmentStatus.COMPLETED);
        } else {
            enrollment.setStatus(Enrollment.EnrollmentStatus.FAILED);
        }

        Enrollment updated = enrollmentRepository.save(enrollment);

        return convertToDTO(updated);
    }

    // Conversion methods
    private EnrollmentDTO convertToDTO(Enrollment enrollment) {
        EnrollmentDTO dto = new EnrollmentDTO();
        dto.setId(enrollment.getId());
        dto.setEnrollmentDate(enrollment.getEnrollmentDate());
        dto.setStatus(enrollment.getStatus());
        dto.setGrade(enrollment.getGrade());

        if (enrollment.getStudent() != null) {
            dto.setStudentId(enrollment.getStudent().getId());
            dto.setStudentName(enrollment.getStudent().getFullName());
        }

        if (enrollment.getCourse() != null) {
            dto.setCourseId(enrollment.getCourse().getId());
            dto.setCourseName(enrollment.getCourse().getName());
            dto.setCourseCode(enrollment.getCourse().getCode());
        }

        return dto;
    }
}