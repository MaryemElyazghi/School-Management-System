package com.example.web.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FieldConfig {
    private String name;           // Nom du champ (propriété de l'entité)
    private String label;          // Label affiché
    private String type;           // text, email, number, date, select, textarea
    private boolean required;      // Champ obligatoire
    private boolean showInList;    // Afficher dans la liste
    private boolean showInForm;    // Afficher dans le formulaire
    private boolean showInDetails; // Afficher dans les détails
    private boolean editable;      // Modifiable (false = readonly)
    private String selectOptions;  // Pour les selects: nom de l'attribut model contenant les options
    private String optionValue;    // Propriété pour la valeur de l'option
    private String optionLabel;    // Propriété pour le label de l'option
    private String icon;           // Icône Bootstrap Icons
    private String format;         // Format d'affichage (ex: date -> dd/MM/yyyy)

    // Méthodes utilitaires
    public boolean isSelect() {
        return "select".equals(type);
    }

    public boolean isTextarea() {
        return "textarea".equals(type);
    }

    public boolean isDate() {
        return "date".equals(type);
    }
}
