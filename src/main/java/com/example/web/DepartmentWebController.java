package com.example.web;

import com.example.dto.DepartmentDTO;
import com.example.entity.Department;
import com.example.repository.DepartmentRepository;
import com.example.service.CourseService;
import com.example.service.DepartmentService;
import com.example.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/web/departments")
@RequiredArgsConstructor
public class DepartmentWebController {

    private final DepartmentService departmentService;
    private final StudentService studentService;
    private final CourseService courseService;
    private final DepartmentRepository departmentRepository;

    // ========== CONSULTATION - Tous authentifiés ==========

    @GetMapping
    public String listDepartments(Model model) {
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("pageTitle", "Liste des Filières");
        return "departments/list";
    }

    @GetMapping("/{id}")
    public String showDepartmentDetails(@PathVariable Long id, Model model) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        model.addAttribute("department", department);
        model.addAttribute("students", studentService.getStudentsByDepartment(id));
        model.addAttribute("courses", courseService.getCoursesByDepartment(id));
        model.addAttribute("pageTitle", "Détails de la Filière");
        return "departments/details";
    }

    // ========== CRÉATION - ADMIN uniquement ==========

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
            redirectAttributes.addFlashAttribute("success", "Filière créée avec succès!");
            return "redirect:/web/departments";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Ajouter une Filière");
            model.addAttribute("isEdit", false);
            return "departments/form";
        }
    }

    // ========== MODIFICATION - ADMIN uniquement ==========

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
            redirectAttributes.addFlashAttribute("success", "Filière modifiée avec succès!");
            return "redirect:/web/departments";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Modifier la Filière");
            model.addAttribute("isEdit", true);
            return "departments/form";
        }
    }

    // ========== SUPPRESSION - ADMIN uniquement ==========

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/delete")
    public String deleteDepartment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            departmentService.deleteDepartment(id);
            redirectAttributes.addFlashAttribute("success", "Filière supprimée avec succès!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/web/departments";
    }
}