package com.example.web;

import com.example.dto.DepartmentDTO;
import com.example.dto.StudentDTO;
import com.example.dto.CourseDTO;
import com.example.entity.Department;
import com.example.repository.DepartmentRepository;
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
 * ✅ Controller Web pour la gestion des Filières
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
    private final DepartmentRepository departmentRepository;

    // ========================================================================
    // CONSULTATION - Tous les utilisateurs authentifiés
    // ========================================================================

    /**
     * ✅ Liste de toutes les filières
     * URL: GET /web/departments
     *
     * Affiche:
     * - Toutes les filières en cards
     * - Statistiques (nombre d'élèves, nombre de cours)
     * - Boutons d'action selon rôle
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
     * ✅ Détails d'une filière avec listes des élèves et cours
     * URL: GET /web/departments/{id}
     *
     * Affiche:
     * - Informations de la filière
     * - ✅ LISTE COMPLÈTE DES ÉLÈVES de cette filière
     * - ✅ LISTE COMPLÈTE DES COURS de cette filière
     *
     * EXIGENCE MINI-PROJET: "liste des élèves + liste des cours"
     */
    @GetMapping("/{id}")
    public String showDepartmentDetails(@PathVariable Long id, Model model) {
        // Récupérer la filière
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Filière non trouvée"));

        // ✅ Récupérer la liste des élèves de cette filière
        List<StudentDTO> students = studentService.getStudentsByDepartment(id);

        // ✅ Récupérer la liste des cours de cette filière
        List<CourseDTO> courses = courseService.getCoursesByDepartment(id);

        // Ajouter au modèle
        model.addAttribute("department", department);
        model.addAttribute("students", students);  // ✅ LISTE ÉLÈVES
        model.addAttribute("courses", courses);    // ✅ LISTE COURS
        model.addAttribute("pageTitle", "Détails de la Filière: " + department.getName());

        return "departments/details";
    }

    // ========================================================================
    // CRÉATION - ADMIN uniquement
    // ========================================================================

    /**
     * ✅ Formulaire de création d'une nouvelle filière
     * URL: GET /web/departments/new
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("department", new DepartmentDTO());
        model.addAttribute("pageTitle", "Ajouter une Filière");
        model.addAttribute("isEdit", false);

        return "departments/form";
    }

    /**
     * ✅ Traitement de la création
     * URL: POST /web/departments/new
     *
     * CORRECTION: Utilise /new au lieu de / pour éviter ambiguïté
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/new")
    public String createDepartment(
            @Valid @ModelAttribute("department") DepartmentDTO departmentDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Validation des erreurs
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Ajouter une Filière");
            model.addAttribute("isEdit", false);
            return "departments/form";
        }

        try {
            // Créer la filière
            departmentService.createDepartment(departmentDTO);

            // Message de succès
            redirectAttributes.addFlashAttribute("success",
                    "Filière créée avec succès!");

            return "redirect:/web/departments";

        } catch (Exception e) {
            // Erreur métier (ex: code déjà existant)
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Ajouter une Filière");
            model.addAttribute("isEdit", false);

            return "departments/form";
        }
    }

    // ========================================================================
    // MODIFICATION - ADMIN uniquement
    // ========================================================================

    /**
     * ✅ Formulaire de modification
     * URL: GET /web/departments/{id}/edit
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        DepartmentDTO department = departmentService.getDepartmentById(id);

        model.addAttribute("department", department);
        model.addAttribute("pageTitle", "Modifier la Filière");
        model.addAttribute("isEdit", true);

        return "departments/form";
    }

    /**
     * ✅ Traitement de la modification
     * URL: POST /web/departments/{id}/update
     *
     * CORRECTION: Utilise /{id}/update au lieu de /{id} pour éviter conflit
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/update")
    public String updateDepartment(
            @PathVariable Long id,
            @Valid @ModelAttribute("department") DepartmentDTO departmentDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Validation des erreurs
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Modifier la Filière");
            model.addAttribute("isEdit", true);
            return "departments/form";
        }

        try {
            // Mettre à jour la filière
            departmentService.updateDepartment(id, departmentDTO);

            // Message de succès
            redirectAttributes.addFlashAttribute("success",
                    "Filière modifiée avec succès!");

            return "redirect:/web/departments";

        } catch (Exception e) {
            // Erreur métier
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Modifier la Filière");
            model.addAttribute("isEdit", true);

            return "departments/form";
        }
    }

    // ========================================================================
    // SUPPRESSION - ADMIN uniquement
    // ========================================================================

    /**
     * ✅ Suppression d'une filière
     * URL: POST /web/departments/{id}/delete
     *
     * Protection: Impossible si des élèves ou cours existent
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/delete")
    public String deleteDepartment(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            // Tenter la suppression
            // ✅ Le service vérifie automatiquement:
            //    - Pas d'élèves dans la filière
            //    - Pas de cours dans la filière
            departmentService.deleteDepartment(id);

            // Message de succès
            redirectAttributes.addFlashAttribute("success",
                    "Filière supprimée avec succès!");

        } catch (Exception e) {
            // Erreur (ex: "Cannot delete, 5 students exist")
            redirectAttributes.addFlashAttribute("error",
                    "Erreur lors de la suppression: " + e.getMessage());
        }

        return "redirect:/web/departments";
    }
}