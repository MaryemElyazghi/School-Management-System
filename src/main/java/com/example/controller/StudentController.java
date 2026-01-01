package com.example.controller;

import com.example.dto.CourseDTO;
import com.example.dto.EnrollmentDTO;
import com.example.dto.StudentDTO;
import com.example.entity.User;
import com.example.service.CourseService;
import com.example.service.EnrollmentService;
import com.example.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final StudentService studentService;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    /**
     * Obtenir le profil de l'étudiant connecté
     */
    @GetMapping("/profile")
    public ResponseEntity<StudentDTO> getMyProfile(@AuthenticationPrincipal User user) {
        StudentDTO student = studentService.getStudentByUserId(user.getId());
        return ResponseEntity.ok(student);
    }

    /**
     * RÈGLE MÉTIER : Voir UNIQUEMENT MES cours (auxquels je suis inscrit)
     */
    @GetMapping("/my-courses")
    public ResponseEntity<List<CourseDTO>> getMyCourses(@AuthenticationPrincipal User user) {
        StudentDTO student = studentService.getStudentByUserId(user.getId());
        List<CourseDTO> courses = courseService.getCoursesForStudent(
                student.getId(),
                student.getDepartmentId()
        );
        return ResponseEntity.ok(courses);
    }

    /**
     * Voir les cours disponibles pour inscription (de ma filière)
     */
    @GetMapping("/available-courses")
    public ResponseEntity<List<CourseDTO>> getAvailableCourses(@AuthenticationPrincipal User user) {
        StudentDTO student = studentService.getStudentByUserId(user.getId());
        List<CourseDTO> courses = courseService.getAvailableCoursesForDepartment(
                student.getDepartmentId()
        );
        return ResponseEntity.ok(courses);
    }

    /**
     * Obtenir un cours spécifique (avec vérification d'accès)
     */
    @GetMapping("/courses/{courseId}")
    public ResponseEntity<CourseDTO> getCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User user) {
        StudentDTO student = studentService.getStudentByUserId(user.getId());
        CourseDTO course = courseService.getCourseForStudent(student.getId(), courseId);
        return ResponseEntity.ok(course);
    }

    /**
     * S'inscrire à un cours (validation automatique de la filière)
     */
    @PostMapping("/enroll/{courseId}")
    public ResponseEntity<EnrollmentDTO> enrollInCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User user) {
        StudentDTO student = studentService.getStudentByUserId(user.getId());
        EnrollmentDTO enrollment = enrollmentService.enrollStudent(student.getId(), courseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    /**
     * Abandonner un cours
     */
    @PutMapping("/enrollments/{enrollmentId}/drop")
    public ResponseEntity<EnrollmentDTO> dropCourse(
            @PathVariable Long enrollmentId) {
        EnrollmentDTO enrollment = enrollmentService.dropCourse(enrollmentId);
        return ResponseEntity.ok(enrollment);
    }

    /**
     * Voir mes inscriptions
     */
    @GetMapping("/enrollments")
    public ResponseEntity<List<EnrollmentDTO>> getMyEnrollments(@AuthenticationPrincipal User user) {
        StudentDTO student = studentService.getStudentByUserId(user.getId());
        List<EnrollmentDTO> enrollments = enrollmentService.getStudentEnrollments(student.getId());
        return ResponseEntity.ok(enrollments);
    }

    /**
     * Voir mes inscriptions actives
     */
    @GetMapping("/enrollments/active")
    public ResponseEntity<List<EnrollmentDTO>> getMyActiveEnrollments(@AuthenticationPrincipal User user) {
        StudentDTO student = studentService.getStudentByUserId(user.getId());
        List<EnrollmentDTO> enrollments = enrollmentService.getActiveStudentEnrollments(student.getId());
        return ResponseEntity.ok(enrollments);
    }
}