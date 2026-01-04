package com.example.web;

import com.example.dto.StudentDTO;
import com.example.entity.Student;
import com.example.repository.StudentRepository;
import com.example.service.CourseService;
import com.example.service.DepartmentService;
import com.example.service.EnrollmentService;
import com.example.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/web/students")
@RequiredArgsConstructor
public class StudentWebController {

    private final StudentService studentService;
    private final DepartmentService departmentService;
    private final EnrollmentService enrollmentService;
    private final CourseService courseService;
    private final StudentRepository studentRepository;

    // ========== CONSULTATION - Tous les utilisateurs authentifiés ==========

    @GetMapping
    public String listStudents(Model model) {
        model.addAttribute("students", studentService.getAllStudents());
        model.addAttribute("pageTitle", "Liste des Élèves");
        return "students/list";
    }

    @GetMapping("/{id}")
    public String showStudentDetails(@PathVariable Long id, Model model) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        model.addAttribute("student", student);
        model.addAttribute("enrollments", enrollmentService.getStudentEnrollments(id));

        // ✅ Ajouter les cours disponibles pour inscription
        // Utilise getAvailableCoursesForStudent qui EXCLUT les cours déjà suivis et les cours complets
        if (student.getDepartment() != null) {
            model.addAttribute("availableCourses",
                    courseService.getAvailableCoursesForStudent(id, student.getDepartment().getId()));
        }

        model.addAttribute("pageTitle", "Détails de l'Élève");
        return "students/details";
    }

    // ========== CRÉATION - ADMIN et TEACHER uniquement ==========

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("student", new StudentDTO());
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("pageTitle", "Ajouter un Élève");
        model.addAttribute("isEdit", false);
        return "students/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/new")
    public String createStudent(
            @Valid @ModelAttribute("student") StudentDTO studentDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("pageTitle", "Ajouter un Élève");
            model.addAttribute("isEdit", false);
            return "students/form";
        }

        try {
            studentService.createStudent(studentDTO);
            redirectAttributes.addFlashAttribute("success", "Élève créé avec succès!");
            return "redirect:/web/students";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("pageTitle", "Ajouter un Élève");
            model.addAttribute("isEdit", false);
            return "students/form";
        }
    }

    // ========== MODIFICATION - ADMIN et TEACHER uniquement ==========

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        StudentDTO student = studentService.getStudentById(id);
        model.addAttribute("student", student);
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("pageTitle", "Modifier l'Élève");
        model.addAttribute("isEdit", true);
        return "students/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{id}/update")
    public String updateStudent(
            @PathVariable Long id,
            @Valid @ModelAttribute("student") StudentDTO studentDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("pageTitle", "Modifier l'Élève");
            model.addAttribute("isEdit", true);
            return "students/form";
        }

        try {
            studentService.updateStudent(id, studentDTO);
            redirectAttributes.addFlashAttribute("success", "Élève modifié avec succès!");
            return "redirect:/web/students";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("pageTitle", "Modifier l'Élève");
            model.addAttribute("isEdit", true);
            return "students/form";
        }
    }

    // ========== SUPPRESSION - ADMIN uniquement ==========

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/delete")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            studentService.deleteStudent(id);
            redirectAttributes.addFlashAttribute("success", "Élève supprimé avec succès!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/web/students";
    }

    // ========== INSCRIPTION AUX COURS - ADMIN et TEACHER ==========

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{studentId}/enroll/{courseId}")
    public String enrollInCourse(
            @PathVariable Long studentId,
            @PathVariable Long courseId,
            RedirectAttributes redirectAttributes) {
        try {
            enrollmentService.enrollStudent(studentId, courseId);
            redirectAttributes.addFlashAttribute("success", "Inscription réussie!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/students/" + studentId;
    }

    // ========== DÉSINSCRIPTION D'UN COURS - ADMIN et TEACHER ==========

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{studentId}/drop/{enrollmentId}")
    public String dropCourse(
            @PathVariable Long studentId,
            @PathVariable Long enrollmentId,
            RedirectAttributes redirectAttributes) {
        try {
            enrollmentService.dropCourse(enrollmentId);
            redirectAttributes.addFlashAttribute("success", "Désinscription réussie!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/students/" + studentId;
    }
}