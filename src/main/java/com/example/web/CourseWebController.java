package com.example.web;

import com.example.dto.CourseDTO;
import com.example.service.CourseService;
import com.example.service.DepartmentService;
import com.example.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/web/courses")
@RequiredArgsConstructor
public class CourseWebController {

    private final CourseService courseService;
    private final DepartmentService departmentService;
    private final EnrollmentService enrollmentService;

    // ========== CONSULTATION - Tous authentifiés ==========

    @GetMapping
    public String listCourses(Model model) {
        model.addAttribute("courses", courseService.getAllCourses());
        model.addAttribute("pageTitle", "Liste des Cours");
        return "courses/list";
    }

    @GetMapping("/{id}")
    public String showCourseDetails(@PathVariable Long id, Model model) {
        model.addAttribute("course", courseService.getCourseById(id));
        model.addAttribute("enrollments", enrollmentService.getCourseEnrollments(id));
        model.addAttribute("pageTitle", "Détails du Cours");
        return "courses/details";
    }

    // ========== CRÉATION - ADMIN et TEACHER ==========

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("course", new CourseDTO());
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("pageTitle", "Ajouter un Cours");
        model.addAttribute("isEdit", false);
        return "courses/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/new")
    public String createCourse(
            @Valid @ModelAttribute("course") CourseDTO courseDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("pageTitle", "Ajouter un Cours");
            model.addAttribute("isEdit", false);
            return "courses/form";
        }

        try {
            courseService.createCourse(courseDTO);
            redirectAttributes.addFlashAttribute("success", "Cours créé avec succès!");
            return "redirect:/web/courses";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("pageTitle", "Ajouter un Cours");
            model.addAttribute("isEdit", false);
            return "courses/form";
        }
    }

    // ========== MODIFICATION - ADMIN et TEACHER ==========

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        CourseDTO course = courseService.getCourseById(id);
        model.addAttribute("course", course);
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("pageTitle", "Modifier le Cours");
        model.addAttribute("isEdit", true);
        return "courses/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{id}/update")
    public String updateCourse(
            @PathVariable Long id,
            @Valid @ModelAttribute("course") CourseDTO courseDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("pageTitle", "Modifier le Cours");
            model.addAttribute("isEdit", true);
            return "courses/form";
        }

        try {
            courseService.updateCourse(id, courseDTO);
            redirectAttributes.addFlashAttribute("success", "Cours modifié avec succès!");
            return "redirect:/web/courses";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("pageTitle", "Modifier le Cours");
            model.addAttribute("isEdit", true);
            return "courses/form";
        }
    }

    // ========== SUPPRESSION - ADMIN et TEACHER ==========

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{id}/delete")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            courseService.deleteCourse(id);
            redirectAttributes.addFlashAttribute("success", "Cours supprimé avec succès!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/web/courses";
    }

    // ========== GESTION DES INSCRIPTIONS - ADMIN et TEACHER ==========

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{courseId}/enrollments/{enrollmentId}/grade")
    public String assignGrade(
            @PathVariable Long courseId,
            @PathVariable Long enrollmentId,
            @RequestParam Double grade,
            RedirectAttributes redirectAttributes) {
        try {
            // Try to assign grade first, if it fails (grade already exists), try to update
            try {
                enrollmentService.assignGrade(enrollmentId, grade);
            } catch (Exception e) {
                // If grade already exists, update it instead
                enrollmentService.updateGrade(enrollmentId, grade, "Mise à jour de la note");
            }
            redirectAttributes.addFlashAttribute("success", "Note attribuée avec succès!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/web/courses/" + courseId;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{courseId}/enrollments/{enrollmentId}/status")
    public String updateEnrollmentStatus(
            @PathVariable Long courseId,
            @PathVariable Long enrollmentId,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            enrollmentService.updateEnrollmentStatus(enrollmentId,
                    com.example.entity.Enrollment.EnrollmentStatus.valueOf(status));
            redirectAttributes.addFlashAttribute("success", "Statut mis à jour avec succès!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/web/courses/" + courseId;
    }
}