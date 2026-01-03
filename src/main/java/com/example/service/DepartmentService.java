package com.example.service;

import com.example.dto.DepartmentDTO;
import com.example.entity.Department;
import com.example.exception.BusinessException;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.DepartmentRepository;
import com.example.repository.StudentRepository;
import com.example.repository.CourseRepository;
import com.example.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ✅ Service de gestion des Filières (VERSION CORRIGÉE - LAZY LOADING FIX)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;

    // ========================================================================
    // READ - Consultation
    // ========================================================================

    /**
     * ✅ Récupérer toutes les filières (SANS statistiques pour éviter lazy loading)
     */
    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findAll()
                .stream()
                .map(this::convertToDTOSimple) // ✅ Version simple sans accès aux collections
                .collect(Collectors.toList());
    }

    /**
     * ✅ Récupérer une filière par ID (AVEC statistiques)
     */
    public DepartmentDTO getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department", "id", id));

        return convertToDTO(department); // ✅ Version complète avec statistiques
    }

    /**
     * ✅ Récupérer une filière par code
     */
    public DepartmentDTO getDepartmentByCode(String code) {
        Department department = departmentRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department", "code", code));

        return convertToDTO(department);
    }

    // ========================================================================
    // CREATE - Création
    // ========================================================================

    /**
     * ✅ Créer une nouvelle filière
     */
    public DepartmentDTO createDepartment(DepartmentDTO departmentDTO) {
        // ✅ VALIDATION 1: Format du code (alphanumérique uniquement)
        if (!isValidDepartmentCode(departmentDTO.getCode())) {
            throw new BusinessException(
                    "Le code de la filière doit être alphanumérique uniquement (A-Z, 0-9). " +
                            "Caractères interdits: espaces, tirets, underscores, etc."
            );
        }

        // ✅ VALIDATION 2: Unicité du code
        if (departmentRepository.existsByCode(departmentDTO.getCode())) {
            throw new BusinessException(
                    "Le code de filière '" + departmentDTO.getCode() + "' existe déjà. " +
                            "Veuillez choisir un code différent."
            );
        }

        // ✅ VALIDATION 3: Unicité du nom (optionnel mais recommandé)
        departmentRepository.findByName(departmentDTO.getName()).ifPresent(existing -> {
            throw new BusinessException(
                    "Une filière avec le nom '" + departmentDTO.getName() + "' existe déjà."
            );
        });

        // Conversion DTO → Entity
        Department department = convertToEntity(departmentDTO);

        // Sauvegarde
        Department saved = departmentRepository.save(department);

        // Conversion Entity → DTO
        return convertToDTOSimple(saved);
    }

    // ========================================================================
    // UPDATE - Modification
    // ========================================================================

    /**
     * ✅ Mettre à jour une filière existante
     */
    public DepartmentDTO updateDepartment(Long id, DepartmentDTO departmentDTO) {
        // Vérifier que la filière existe
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department", "id", id));

        // ✅ VALIDATION 1: Format du code
        if (!isValidDepartmentCode(departmentDTO.getCode())) {
            throw new BusinessException(
                    "Le code de la filière doit être alphanumérique uniquement."
            );
        }

        // ✅ VALIDATION 2: Si le code a changé, vérifier l'unicité
        if (!department.getCode().equals(departmentDTO.getCode())) {
            if (departmentRepository.existsByCode(departmentDTO.getCode())) {
                throw new BusinessException(
                        "Le code de filière '" + departmentDTO.getCode() + "' est déjà utilisé."
                );
            }
        }

        // ✅ VALIDATION 3: Si le nom a changé, vérifier l'unicité
        if (!department.getName().equals(departmentDTO.getName())) {
            departmentRepository.findByName(departmentDTO.getName()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new BusinessException(
                            "Une filière avec le nom '" + departmentDTO.getName() + "' existe déjà."
                    );
                }
            });
        }

        // Mise à jour des champs
        department.setCode(departmentDTO.getCode());
        department.setName(departmentDTO.getName());
        department.setDescription(departmentDTO.getDescription());

        // Sauvegarde
        Department updated = departmentRepository.save(department);

        return convertToDTOSimple(updated);
    }

    // ========================================================================
    // DELETE - Suppression
    // ========================================================================

    /**
     * ✅ Supprimer une filière
     */
    public void deleteDepartment(Long id) {
        // Vérifier que la filière existe
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department", "id", id));

        // ✅ PROTECTION 1: Vérifier les élèves
        long studentCount = studentRepository.findByDepartmentId(id).size();
        if (studentCount > 0) {
            throw new BusinessException(
                    String.format(
                            "Impossible de supprimer la filière '%s'. " +
                                    "Elle contient %d élève(s). " +
                                    "Veuillez d'abord transférer ou supprimer les élèves.",
                            department.getName(),
                            studentCount
                    )
            );
        }

        // ✅ PROTECTION 2: Vérifier les cours
        long courseCount = courseRepository.findByDepartmentId(id).size();
        if (courseCount > 0) {
            throw new BusinessException(
                    String.format(
                            "Impossible de supprimer la filière '%s'. " +
                                    "Elle contient %d cours. " +
                                    "Veuillez d'abord supprimer ou transférer les cours.",
                            department.getName(),
                            courseCount
                    )
            );
        }

        // ✅ PROTECTION 3: Vérifier les enseignants (optionnel)
        long teacherCount = teacherRepository.findByDepartmentId(id).size();
        if (teacherCount > 0) {
            throw new BusinessException(
                    String.format(
                            "Impossible de supprimer la filière '%s'. " +
                                    "%d enseignant(s) y sont affectés. " +
                                    "Veuillez d'abord les réaffecter.",
                            department.getName(),
                            teacherCount
                    )
            );
        }

        // Si toutes les vérifications passent, supprimer
        departmentRepository.delete(department);
    }

    // ========================================================================
    // VALIDATIONS MÉTIER
    // ========================================================================

    /**
     * ✅ Valider le format du code de filière
     */
    private boolean isValidDepartmentCode(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        return code.matches("^[A-Za-z0-9]+$");
    }

    // ========================================================================
    // STATISTIQUES
    // ========================================================================

    /**
     * ✅ Obtenir le nombre d'élèves dans une filière
     */
    public long getStudentCount(Long departmentId) {
        return studentRepository.findByDepartmentId(departmentId).size();
    }

    /**
     * ✅ Obtenir le nombre de cours dans une filière
     */
    public long getCourseCount(Long departmentId) {
        return courseRepository.findByDepartmentId(departmentId).size();
    }

    // ========================================================================
    // CONVERSIONS DTO ↔ ENTITY (VERSION CORRIGÉE ANTI-LAZY-LOADING)
    // ========================================================================

    /**
     * ✅ Convertir Entity → DTO (VERSION SIMPLE - pour listes)
     *
     * N'accède PAS aux collections lazy - Met des 0 par défaut
     */
    private DepartmentDTO convertToDTOSimple(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setCode(department.getCode());
        dto.setName(department.getName());
        dto.setDescription(department.getDescription());

        // ✅ Pas d'accès aux collections - valeurs par défaut
        dto.setStudentCount(0);
        dto.setCourseCount(0);

        return dto;
    }

    /**
     * ✅ Convertir Entity → DTO (VERSION COMPLÈTE - pour détails)
     *
     * Utilise les repositories pour compter au lieu d'accéder aux collections
     */
    private DepartmentDTO convertToDTO(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setCode(department.getCode());
        dto.setName(department.getName());
        dto.setDescription(department.getDescription());

        // ✅ Utiliser les repositories pour éviter lazy loading
        dto.setStudentCount((int) studentRepository.findByDepartmentId(department.getId()).size());
        dto.setCourseCount((int) courseRepository.findByDepartmentId(department.getId()).size());

        return dto;
    }

    /**
     * Convertir DTO → Entity
     */
    private Department convertToEntity(DepartmentDTO dto) {
        Department department = new Department();
        department.setCode(dto.getCode());
        department.setName(dto.getName());
        department.setDescription(dto.getDescription());
        return department;
    }
}