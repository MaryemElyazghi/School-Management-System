package com.example.service;

import com.example.dto.DepartmentDTO;
import com.example.entity.Course;
import com.example.entity.Department;
import com.example.entity.Enrollment;
import com.example.exception.BusinessException;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.DepartmentRepository;
import com.example.repository.StudentRepository;
import com.example.repository.CourseRepository;
import com.example.repository.TeacherRepository;
import com.example.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ✅ Service de gestion des Filières - VERSION CORRIGÉE SUPPRESSION CASCADE
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;

    // ========================================================================
    // READ - Consultation
    // ========================================================================

    /**
     * ✅ Récupérer toutes les filières AVEC statistiques
     * Utilise les repositories pour éviter le lazy loading
     */
    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findAll()
                .stream()
                .map(this::convertToDTOWithStats) // ✅ Version avec statistiques
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
    // DELETE - Suppression - VERSION CORRIGÉE CASCADE
    // ========================================================================

    /**
     * ✅ Supprimer une filière - VERSION CORRIGÉE
     *
     * PROBLÈME RÉSOLU:
     * - Supprime en CASCADE dans le bon ordre
     * - Gère les enrollments orphelins
     *
     * ORDRE DE SUPPRESSION CRITIQUE:
     * 1. Enrollments (références courses + students)
     * 2. Courses (références department)
     * 3. Students (références department) - si vide
     * 4. Teachers (références department) - si vide
     * 5. Department
     */
    public void deleteDepartment(Long id) {
        // Vérifier que la filière existe
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department", "id", id));

        // ✅ ÉTAPE 1: Supprimer tous les enrollments liés aux cours de cette filière
        List<Course> courses = courseRepository.findByDepartmentId(id);
        for (Course course : courses) {
            List<Enrollment> enrollments = enrollmentRepository.findByCourseId(course.getId());
            if (!enrollments.isEmpty()) {
                enrollmentRepository.deleteAll(enrollments);
            }
        }

        // ✅ ÉTAPE 2: Supprimer tous les cours de cette filière
        if (!courses.isEmpty()) {
            courseRepository.deleteAll(courses);
        }

        // ✅ ÉTAPE 3: Vérifier les élèves
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

        // ✅ ÉTAPE 4: Vérifier les enseignants (optionnel - peut être supprimé)
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

        // ✅ ÉTAPE 5: Supprimer la filière
        departmentRepository.delete(department);
    }

    /**
     * ✅ OPTION ALTERNATIVE: Suppression forcée (supprime aussi les étudiants)
     * Décommenter si vous voulez autoriser la suppression même avec des étudiants
     */
    /*
    @Transactional
    public void deleteDepartmentForce(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        // 1. Supprimer tous les enrollments des cours de cette filière
        List<Course> courses = courseRepository.findByDepartmentId(id);
        for (Course course : courses) {
            enrollmentRepository.deleteAll(enrollmentRepository.findByCourseId(course.getId()));
        }

        // 2. Supprimer tous les cours
        courseRepository.deleteAll(courses);

        // 3. Supprimer tous les dossiers administratifs des étudiants
        List<Student> students = studentRepository.findByDepartmentId(id);
        for (Student student : students) {
            if (student.getDossierAdministratif() != null) {
                dossierRepository.delete(student.getDossierAdministratif());
            }
        }

        // 4. Supprimer tous les enrollments des étudiants
        for (Student student : students) {
            enrollmentRepository.deleteAll(enrollmentRepository.findByStudentId(student.getId()));
        }

        // 5. Supprimer tous les étudiants
        studentRepository.deleteAll(students);

        // 6. Réaffecter ou supprimer les enseignants (selon votre choix)
        // Option A: Erreur si enseignants présents
        long teacherCount = teacherRepository.findByDepartmentId(id).size();
        if (teacherCount > 0) {
            throw new BusinessException("Veuillez d'abord réaffecter les enseignants");
        }

        // 7. Supprimer la filière
        departmentRepository.delete(department);
    }
    */

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
    // CONVERSIONS DTO ↔ ENTITY
    // ========================================================================

    private DepartmentDTO convertToDTO(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setCode(department.getCode());
        dto.setName(department.getName());
        dto.setDescription(department.getDescription());

        // ✅ Utiliser les repositories pour éviter lazy loading
        dto.setStudentCount((int) studentRepository.findByDepartmentId(department.getId()).size());
        dto.setCourseCount((int) courseRepository.findByDepartmentId(department.getId()).size());

        // ✅ Champs d'audit
        dto.setCreatedAt(department.getCreatedAt());
        dto.setUpdatedAt(department.getUpdatedAt());

        return dto;
    }

    private DepartmentDTO convertToDTOWithStats(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setCode(department.getCode());
        dto.setName(department.getName());
        dto.setDescription(department.getDescription());

        // ✅ Utiliser les repositories pour compter (pas de lazy loading)
        dto.setStudentCount((int) studentRepository.findByDepartmentId(department.getId()).size());
        dto.setCourseCount((int) courseRepository.findByDepartmentId(department.getId()).size());

        // ✅ Champs d'audit
        dto.setCreatedAt(department.getCreatedAt());
        dto.setUpdatedAt(department.getUpdatedAt());

        return dto;
    }

    private DepartmentDTO convertToDTOSimple(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setCode(department.getCode());
        dto.setName(department.getName());
        dto.setDescription(department.getDescription());

        // ✅ Pas d'accès aux collections - valeurs par défaut
        dto.setStudentCount(0);
        dto.setCourseCount(0);

        // ✅ Champs d'audit
        dto.setCreatedAt(department.getCreatedAt());
        dto.setUpdatedAt(department.getUpdatedAt());

        return dto;
    }

    private Department convertToEntity(DepartmentDTO dto) {
        Department department = new Department();
        department.setCode(dto.getCode());
        department.setName(dto.getName());
        department.setDescription(dto.getDescription());
        return department;
    }
}