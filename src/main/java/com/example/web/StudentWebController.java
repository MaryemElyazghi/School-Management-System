package com.example.web;

import com.example.dto.StudentDTO;
import com.example.service.CourseService;
import com.example.service.DepartmentService;
import com.example.service.EnrollmentService;
import com.example.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;

/**
 * ✅ Controller Web pour la gestion des Élèves - VERSION CORRIGÉE SUPPRESSION
 */
@Controller
@RequestMapping("/web/students")
@RequiredArgsConstructor
@Slf4j
public class StudentWebController {

    private final StudentService studentService;
    private final DepartmentService departmentService;
    private final EnrollmentService enrollmentService;
    private final CourseService courseService;

    // ========== CONSULTATION - Tous les utilisateurs authentifiés ==========

    @GetMapping
    public String listStudents(Model model) {
        try {
            model.addAttribute("students", studentService.getAllStudents());
            model.addAttribute("pageTitle", "Liste des Élèves");
            log.info("✅ Liste des étudiants chargée avec succès");
            return "students/list";
        } catch (Exception e) {
            log.error("❌ Erreur lors du chargement de la liste des étudiants", e);
            model.addAttribute("error", "Erreur lors du chargement: " + e.getMessage());
            return "students/list";
        }
    }

    /**
     * ✅ Afficher les détails d'un élève - VERSION CORRIGÉE
     */
    @GetMapping("/{id}")
    public String showStudentDetails(@PathVariable Long id, Model model) {
        try {
            StudentDTO student = studentService.getStudentById(id);
            model.addAttribute("student", student);
            model.addAttribute("enrollments", enrollmentService.getStudentEnrollments(id));

            // Ajouter les cours disponibles pour inscription
            if (student.getDepartmentId() != null) {
                model.addAttribute("availableCourses",
                        courseService.getAvailableCoursesForStudent(id, student.getDepartmentId()));
            }

            model.addAttribute("pageTitle", "Détails de l'Élève");
            return "students/details";
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'affichage des détails de l'étudiant {}", id, e);
            model.addAttribute("error", "Étudiant introuvable");
            return "redirect:/web/students";
        }
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
            StudentDTO created = studentService.createStudent(studentDTO);
            log.info("✅ Étudiant créé avec succès: ID={}, Nom={} {}",
                    created.getId(), created.getFirstName(), created.getLastName());
            redirectAttributes.addFlashAttribute("success",
                    "Élève créé avec succès: " + created.getFullName());
            return "redirect:/web/students";
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de l'étudiant", e);
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
        try {
            StudentDTO student = studentService.getStudentById(id);
            model.addAttribute("student", student);
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("pageTitle", "Modifier l'Élève");
            model.addAttribute("isEdit", true);
            return "students/form";
        } catch (Exception e) {
            log.error("❌ Erreur lors du chargement du formulaire d'édition pour l'étudiant {}", id, e);
            return "redirect:/web/students";
        }
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
            StudentDTO updated = studentService.updateStudent(id, studentDTO);
            log.info("✅ Étudiant modifié avec succès: ID={}, Nom={} {}",
                    id, updated.getFirstName(), updated.getLastName());
            redirectAttributes.addFlashAttribute("success",
                    "Élève modifié avec succès: " + updated.getFullName());
            return "redirect:/web/students";
        } catch (Exception e) {
            log.error("❌ Erreur lors de la modification de l'étudiant {}", id, e);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("pageTitle", "Modifier l'Élève");
            model.addAttribute("isEdit", true);
            return "students/form";
        }
    }

    // ========== SUPPRESSION - ADMIN uniquement ==========

    /**
     * ✅ SUPPRESSION D'UN ÉTUDIANT - VERSION CORRIGÉE AVEC LOGS
     *
     * IMPORTANT: Cette méthode doit:
     * 1. Supprimer l'étudiant (avec ses enrollments et dossier)
     * 2. Rediriger vers la liste
     * 3. Afficher un message de succès
     * 4. Logger les opérations pour debug
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/delete")
    public String deleteStudent(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        log.info("🗑️ Tentative de suppression de l'étudiant ID={}", id);

        try {
            // Récupérer les infos de l'étudiant AVANT suppression pour le message
            StudentDTO student = studentService.getStudentById(id);
            String studentName = student.getFullName();

            log.info("📋 Étudiant trouvé: {} (ID={})", studentName, id);

            // Appeler le service de suppression
            studentService.deleteStudent(id);

            log.info("✅ Étudiant supprimé avec succès: {} (ID={})", studentName, id);

            // Message de succès
            redirectAttributes.addFlashAttribute("success",
                    "Élève supprimé avec succès: " + studentName);

        } catch (Exception e) {
            log.error("❌ ERREUR lors de la suppression de l'étudiant ID={}", id, e);

            // Message d'erreur détaillé
            String errorMessage = "Erreur lors de la suppression de l'élève: " + e.getMessage();
            redirectAttributes.addFlashAttribute("error", errorMessage);
        }

        // ✅ TOUJOURS rediriger vers la liste (que ce soit un succès ou une erreur)
        log.info("↩️ Redirection vers /web/students");
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
            log.info("✅ Étudiant {} inscrit au cours {}", studentId, courseId);
            redirectAttributes.addFlashAttribute("success", "Inscription réussie!");
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'inscription de l'étudiant {} au cours {}",
                    studentId, courseId, e);
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
            log.info("✅ Étudiant {} désinscrit de l'enrollment {}", studentId, enrollmentId);
            redirectAttributes.addFlashAttribute("success", "Désinscription réussie!");
        } catch (Exception e) {
            log.error("❌ Erreur lors de la désinscription de l'étudiant {} de l'enrollment {}",
                    studentId, enrollmentId, e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/students/" + studentId;
    }
}