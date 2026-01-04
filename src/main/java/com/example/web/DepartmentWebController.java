package com.example.web;

import com.example.dto.DepartmentDTO;
import com.example.dto.StudentDTO;
import com.example.dto.CourseDTO;
import com.example.service.DepartmentService;
import com.example.service.StudentService;
import com.example.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;

/**
 * ✅ Controller Web pour la gestion des Filières - VERSION CORRIGÉE
 *
 * Fonctionnalités:
 * - CRUD complet des filières
 * - Affichage de la liste des élèves par filière
 * - Affichage de la liste des cours par filière
 *
 * Conformité: 100% selon exigences mini-projet
 */
@Controller
@RequestMapping("/web/departments")
@RequiredArgsConstructor
public class DepartmentWebController {

    private final DepartmentService departmentService;
    private final StudentService studentService;
    private final CourseService courseService;

    // ========================================================================
    // CONSULTATION - Tous les utilisateurs authentifiés
    // ========================================================================

    /**
     * ✅ Liste de toutes les filières
     */
    @GetMapping
    public String listDepartments(Model model) {
        try {
            List<DepartmentDTO> departments = departmentService.getAllDepartments();
            model.addAttribute("departments", departments);
            model.addAttribute("pageTitle", "Liste des Filières");
            return "departments/list";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur: " + e.getMessage());
            model.addAttribute("departments", new ArrayList<>());
            return "departments/list";
        }
    }

    /**
     * ✅ Détails d'une filière avec listes des élèves et cours - VERSION CORRIGÉE
     *
     * CORRECTION : Utilise le DTO au lieu de l'entité
     */
    @GetMapping("/{id}")
    public String showDepartmentDetails(@PathVariable Long id, Model model) {
        // ✅ CORRECTION : Utilise le service qui retourne un DTO
        DepartmentDTO department = departmentService.getDepartmentById(id);

        List<StudentDTO> students = studentService.getStudentsByDepartment(id);
        List<CourseDTO> courses = courseService.getCoursesByDepartment(id);

        model.addAttribute("department", department);  // ✅ DTO au lieu d'entité
        model.addAttribute("students", students);
        model.addAttribute("courses", courses);
        model.addAttribute("pageTitle", "Détails de la Filière: " + department.getName());

        return "departments/details";
    }

    // ========================================================================
    // CRÉATION - ADMIN uniquement
    // ========================================================================

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("department", new DepartmentDTO());
        model.addAttribute("pageTitle", "Ajouter une Filière");
        model.addAttribute("isEdit", false);
        return "departments/form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/new")
    public String createDepartment(
            @Valid @ModelAttribute("department") DepartmentDTO departmentDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Ajouter une Filière");
            model.addAttribute("isEdit", false);
            return "departments/form";
        }

        try {
            departmentService.createDepartment(departmentDTO);
            redirectAttributes.addFlashAttribute("success",
                    "Filière créée avec succès!");
            return "redirect:/web/departments";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Ajouter une Filière");
            model.addAttribute("isEdit", false);
            return "departments/form";
        }
    }

    // ========================================================================
    // MODIFICATION - ADMIN uniquement
    // ========================================================================

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        DepartmentDTO department = departmentService.getDepartmentById(id);
        model.addAttribute("department", department);
        model.addAttribute("pageTitle", "Modifier la Filière");
        model.addAttribute("isEdit", true);
        return "departments/form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/update")
    public String updateDepartment(
            @PathVariable Long id,
            @Valid @ModelAttribute("department") DepartmentDTO departmentDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Modifier la Filière");
            model.addAttribute("isEdit", true);
            return "departments/form";
        }

        try {
            departmentService.updateDepartment(id, departmentDTO);
            redirectAttributes.addFlashAttribute("success",
                    "Filière modifiée avec succès!");
            return "redirect:/web/departments";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Modifier la Filière");
            model.addAttribute("isEdit", true);
            return "departments/form";
        }
    }

    // ========================================================================
    // SUPPRESSION - ADMIN uniquement
    // ========================================================================

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/delete")
    public String deleteDepartment(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            departmentService.deleteDepartment(id);
            redirectAttributes.addFlashAttribute("success",
                    "Filière supprimée avec succès!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Erreur lors de la suppression: " + e.getMessage());
        }

        return "redirect:/web/departments";
    }
}