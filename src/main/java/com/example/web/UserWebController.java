package com.example.web;

import com.example.dto.UserDTO;
import com.example.entity.Role;
import com.example.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller Web pour la gestion des utilisateurs
 * Accessible uniquement aux ADMIN
 */
@Controller
@RequestMapping("/web/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')") // Toutes les méthodes nécessitent le rôle ADMIN
public class UserWebController {

    private final UserService userService;

    // ========================================================================
    // CONSULTATION - ADMIN uniquement
    // ========================================================================

    /**
     * Liste de tous les utilisateurs
     */
    @GetMapping
    public String listUsers(Model model) {
        try {
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("pageTitle", "Liste des Utilisateurs");
            
            // Statistiques
            model.addAttribute("adminCount", userService.countByRole(Role.ADMIN));
            model.addAttribute("teacherCount", userService.countByRole(Role.TEACHER));
            model.addAttribute("studentCount", userService.countByRole(Role.STUDENT));
            
            log.info("✅ Liste des utilisateurs chargée avec succès");
            return "users/list";
        } catch (Exception e) {
            log.error("❌ Erreur lors du chargement de la liste des utilisateurs", e);
            model.addAttribute("error", "Erreur lors du chargement: " + e.getMessage());
            return "users/list";
        }
    }

    /**
     * Afficher les détails d'un utilisateur (optionnel)
     */
    @GetMapping("/{id}")
    public String showUserDetails(@PathVariable Long id, Model model) {
        try {
            UserDTO user = userService.getUserById(id);
            model.addAttribute("user", user);
            model.addAttribute("pageTitle", "Détails de l'Utilisateur");
            return "users/details";
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'affichage des détails de l'utilisateur {}", id, e);
            return "redirect:/web/users";
        }
    }

    // ========================================================================
    // CRÉATION - ADMIN uniquement
    // ========================================================================

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        UserDTO userDTO = new UserDTO();
        userDTO.setEnabled(true); // Par défaut, l'utilisateur est activé
        
        model.addAttribute("user", userDTO);
        model.addAttribute("roles", Role.values());
        model.addAttribute("pageTitle", "Créer un Utilisateur");
        model.addAttribute("isEdit", false);
        return "users/form";
    }

    @PostMapping("/new")
    public String createUser(
            @Valid @ModelAttribute("user") UserDTO userDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("roles", Role.values());
            model.addAttribute("pageTitle", "Créer un Utilisateur");
            model.addAttribute("isEdit", false);
            return "users/form";
        }

        try {
            UserDTO created = userService.createUser(userDTO);
            log.info("✅ Utilisateur créé avec succès: {}", created.getUsername());
            redirectAttributes.addFlashAttribute("success",
                    "Utilisateur créé avec succès: " + created.getUsername());
            return "redirect:/web/users";
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de l'utilisateur", e);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", Role.values());
            model.addAttribute("pageTitle", "Créer un Utilisateur");
            model.addAttribute("isEdit", false);
            return "users/form";
        }
    }

    // ========================================================================
    // MODIFICATION - ADMIN uniquement
    // ========================================================================

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        try {
            UserDTO user = userService.getUserById(id);
            // Effacer le mot de passe pour ne pas le préremplir
            user.setPassword(null);
            
            model.addAttribute("user", user);
            model.addAttribute("roles", Role.values());
            model.addAttribute("pageTitle", "Modifier l'Utilisateur");
            model.addAttribute("isEdit", true);
            return "users/form";
        } catch (Exception e) {
            log.error("❌ Erreur lors du chargement du formulaire d'édition pour l'utilisateur {}", id, e);
            return "redirect:/web/users";
        }
    }

    @PostMapping("/{id}/update")
    public String updateUser(
            @PathVariable Long id,
            @Valid @ModelAttribute("user") UserDTO userDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("roles", Role.values());
            model.addAttribute("pageTitle", "Modifier l'Utilisateur");
            model.addAttribute("isEdit", true);
            return "users/form";
        }

        try {
            UserDTO updated = userService.updateUser(id, userDTO);
            log.info("✅ Utilisateur modifié avec succès: {}", updated.getUsername());
            redirectAttributes.addFlashAttribute("success",
                    "Utilisateur modifié avec succès: " + updated.getUsername());
            return "redirect:/web/users";
        } catch (Exception e) {
            log.error("❌ Erreur lors de la modification de l'utilisateur {}", id, e);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", Role.values());
            model.addAttribute("pageTitle", "Modifier l'Utilisateur");
            model.addAttribute("isEdit", true);
            return "users/form";
        }
    }

    // ========================================================================
    // SUPPRESSION - ADMIN uniquement
    // ========================================================================

    @PostMapping("/{id}/delete")
    public String deleteUser(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        log.info("🗑️ Tentative de suppression de l'utilisateur ID={}", id);

        try {
            // Récupérer les infos de l'utilisateur AVANT suppression
            UserDTO user = userService.getUserById(id);
            String username = user.getUsername();

            log.info("📋 Utilisateur trouvé: {} (ID={})", username, id);

            // Appeler le service de suppression
            userService.deleteUser(id);

            log.info("✅ Utilisateur supprimé avec succès: {} (ID={})", username, id);

            // Message de succès
            redirectAttributes.addFlashAttribute("success",
                    "Utilisateur supprimé avec succès: " + username);

        } catch (Exception e) {
            log.error("❌ ERREUR lors de la suppression de l'utilisateur ID={}", id, e);

            // Message d'erreur détaillé
            String errorMessage = "Erreur lors de la suppression de l'utilisateur: " + e.getMessage();
            redirectAttributes.addFlashAttribute("error", errorMessage);
        }

        // TOUJOURS rediriger vers la liste
        log.info("↩️ Redirection vers /web/users");
        return "redirect:/web/users";
    }
}
