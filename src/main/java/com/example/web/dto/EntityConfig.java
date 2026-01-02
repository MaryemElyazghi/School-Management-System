package com.example.web.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class EntityConfig {
    private String entityName;          // Nom de l'entité (ex: "student")
    private String entityNamePlural;    // Pluriel (ex: "students")
    private String displayName;         // Nom affiché (ex: "Élève")
    private String displayNamePlural;   // Pluriel affiché (ex: "Élèves")
    private String icon;                // Icône principale
    private String baseUrl;             // URL de base (ex: "/web/students")
    private List<FieldConfig> fields;   // Configuration des champs
    private String identifierField;     // Champ identifiant (ex: "id")
    private String titleField;          // Champ utilisé comme titre (ex: "fullName")

    // Couleurs et styles
    @Builder.Default
    private String primaryColor = "primary";
    @Builder.Default
    private String cardClass = "shadow";

    // Actions disponibles
    @Builder.Default
    private boolean canCreate = true;
    @Builder.Default
    private boolean canEdit = true;
    @Builder.Default
    private boolean canDelete = true;
    @Builder.Default
    private boolean canView = true;

    // Rôles requis pour les actions (optionnel)
    private String createRole;
    private String editRole;
    private String deleteRole;

    // Méthodes utilitaires pour filtrer les champs
    public List<FieldConfig> getListFields() {
        return fields.stream().filter(FieldConfig::isShowInList).toList();
    }

    public List<FieldConfig> getFormFields() {
        return fields.stream().filter(FieldConfig::isShowInForm).toList();
    }

    public List<FieldConfig> getDetailsFields() {
        return fields.stream().filter(FieldConfig::isShowInDetails).toList();
    }
}
