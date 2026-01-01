package com.example.web;

import com.example.dto.StudentDTO;
import com.example.entity.Student;
import com.example.repository.DepartmentRepository;
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

    /**
     * Liste de tous les étudiants
     */
    @GetMapping
    public String listStudents(Model model) {
        model.addAttribute("students", studentService.getAllStudents());
        model.addAttribute("pageTitle", "Liste des Élèves");
        return "students/list";
    }

    /**
     * Afficher le formulaire de création
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("student", new StudentDTO());
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("pageTitle", "Ajouter un Élève");
        model.addAttribute("isEdit", false);
        return "students/form";
    }

    /**
     * Créer un nouvel étudiant
     */
    @PostMapping
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

    /**
     * Afficher le formulaire de modification
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        StudentDTO student = studentService.getStudentById(id);
        model.addAttribute("student", student);
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("pageTitle", "Modifier l'Élève");
        model.addAttribute("isEdit", true);
        return "students/form";
    }

    /**
     * Mettre à jour un étudiant
     */
    @PostMapping("/{id}")
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

    /**
     * Détails d'un étudiant (avec filière, cours, dossier administratif)
     */
    @GetMapping("/{id}")
    public String showStudentDetails(@PathVariable Long id, Model model) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        model.addAttribute("student", student);
        model.addAttribute("enrollments", enrollmentService.getStudentEnrollments(id));
        model.addAttribute("pageTitle", "Détails de l'Élève");
        return "students/details";
    }

    /**
     * Supprimer un étudiant
     */
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

    /**
     * Inscrire un étudiant à un cours
     */
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
}