package com.example.controller;

import com.example.dto.CourseDTO;
import com.example.dto.DepartmentDTO;
import com.example.dto.EnrollmentDTO;
import com.example.dto.StudentDTO;
import com.example.entity.Enrollment;
import com.example.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DepartmentService departmentService;
    private final StudentService studentService;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    // ========== DEPARTMENTS ==========

    @GetMapping("/departments")
    public ResponseEntity<List<DepartmentDTO>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/departments/{id}")
    public ResponseEntity<DepartmentDTO> getDepartment(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @PostMapping("/departments")
    public ResponseEntity<DepartmentDTO> createDepartment(@Valid @RequestBody DepartmentDTO dto) {
        DepartmentDTO created = departmentService.createDepartment(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/departments/{id}")
    public ResponseEntity<DepartmentDTO> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentDTO dto) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, dto));
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    // ========== STUDENTS ==========

    @GetMapping("/students")
    public ResponseEntity<List<StudentDTO>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<StudentDTO> getStudent(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @PostMapping("/students")
    public ResponseEntity<StudentDTO> createStudent(@Valid @RequestBody StudentDTO dto) {
        StudentDTO created = studentService.createStudent(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<StudentDTO> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentDTO dto) {
        return ResponseEntity.ok(studentService.updateStudent(id, dto));
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/students/search")
    public ResponseEntity<List<StudentDTO>> searchStudents(@RequestParam String keyword) {
        return ResponseEntity.ok(studentService.searchStudents(keyword));
    }

    // ========== COURSES ==========

    @GetMapping("/courses")
    public ResponseEntity<List<CourseDTO>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/courses/{id}")
    public ResponseEntity<CourseDTO> getCourse(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @PostMapping("/courses")
    public ResponseEntity<CourseDTO> createCourse(@Valid @RequestBody CourseDTO dto) {
        CourseDTO created = courseService.createCourse(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/courses/{id}")
    public ResponseEntity<CourseDTO> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseDTO dto) {
        return ResponseEntity.ok(courseService.updateCourse(id, dto));
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/courses/search")
    public ResponseEntity<List<CourseDTO>> searchCourses(@RequestParam String keyword) {
        return ResponseEntity.ok(courseService.searchCourses(keyword));
    }

    // ========== ENROLLMENTS ==========

    @GetMapping("/enrollments/course/{courseId}")
    public ResponseEntity<List<EnrollmentDTO>> getCourseEnrollments(@PathVariable Long courseId) {
        return ResponseEntity.ok(enrollmentService.getCourseEnrollments(courseId));
    }

    @GetMapping("/enrollments/student/{studentId}")
    public ResponseEntity<List<EnrollmentDTO>> getStudentEnrollments(@PathVariable Long studentId) {
        return ResponseEntity.ok(enrollmentService.getStudentEnrollments(studentId));
    }

    @PostMapping("/enrollments")
    public ResponseEntity<EnrollmentDTO> enrollStudent(
            @RequestParam Long studentId,
            @RequestParam Long courseId) {
        EnrollmentDTO enrollment = enrollmentService.enrollStudent(studentId, courseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    @PutMapping("/enrollments/{enrollmentId}/grade")
    public ResponseEntity<EnrollmentDTO> assignGrade(
            @PathVariable Long enrollmentId,
            @RequestParam Double grade) {
        return ResponseEntity.ok(enrollmentService.assignGrade(enrollmentId, grade));
    }

    @PutMapping("/enrollments/{enrollmentId}/status")
    public ResponseEntity<EnrollmentDTO> updateStatus(
            @PathVariable Long enrollmentId,
            @RequestParam Enrollment.EnrollmentStatus status) {
        return ResponseEntity.ok(enrollmentService.updateEnrollmentStatus(enrollmentId, status));
    }
}