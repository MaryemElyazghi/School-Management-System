package com.example.service;

import com.example.dto.CourseDTO;
import com.example.entity.Course;
import com.example.entity.Department;
import com.example.entity.Enrollment;
import com.example.exception.BusinessException;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.CourseRepository;
import com.example.repository.DepartmentRepository;
import com.example.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * SERVICE COURS - STRATÉGIE DE SUPPRESSION STRICTE
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * RÈGLE DE SUPPRESSION :
 * - ⛔ BLOQUÉ si enrollments ACTIF ou COMPLETED existent
 * - ✅ AUTORISÉ si seulement DROPPED ou FAILED (ces enrollments seront supprimés)
 *
 * Logique métier :
 * - Un cours avec des étudiants actifs ou ayant validé ne peut pas être supprimé
 * - Un cours où tous les étudiants ont abandonné ou échoué peut être supprimé
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final EnrollmentRepository enrollmentRepository;

    // ════════════════════════════════════════════════════════════════════════════
    // LECTURE (READ)
    // ════════════════════════════════════════════════════════════════════════════

    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CourseDTO getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));
        return convertToDTO(course);
    }

    public List<CourseDTO> getCoursesByDepartment(Long departmentId) {
        return courseRepository.findByDepartmentId(departmentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    // ════════════════════════════════════════════════════════════════════════════
    // MÉTHODES POUR ÉTUDIANTS (accès restreint par filière)
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Cours auxquels un étudiant est inscrit
     */
    public List<CourseDTO> getCoursesForStudent(Long studentId, Long departmentId) {
        return courseRepository.findCoursesByStudentId(studentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Cours disponibles pour inscription (filière de l'étudiant, non encore inscrit, non complet)
     */
    public List<CourseDTO> getAvailableCoursesForStudent(Long studentId, Long departmentId) {
        List<Course> departmentCourses = courseRepository.findAvailableCoursesForDepartment(departmentId);

        List<Long> enrolledCourseIds = courseRepository.findCoursesByStudentId(studentId)
                .stream()
                .map(Course::getId)
                .collect(Collectors.toList());

        return departmentCourses.stream()
                .filter(course -> !enrolledCourseIds.contains(course.getId()))
                .filter(course -> !course.isFull())
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CourseDTO> getAvailableCoursesForDepartment(Long departmentId) {
        return courseRepository.findAvailableCoursesForDepartment(departmentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public boolean canStudentAccessCourse(Long studentId, Long courseId) {
        return courseRepository.findCoursesByStudentId(studentId)
                .stream()
                .anyMatch(course -> course.getId().equals(courseId));
    }

    public CourseDTO getCourseForStudent(Long studentId, Long courseId) {
        if (!canStudentAccessCourse(studentId, courseId)) {
            throw new AccessDeniedException("Vous n'avez pas accès à ce cours");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        return convertToDTO(course);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CRÉATION (CREATE)
    // ════════════════════════════════════════════════════════════════════════════

    public CourseDTO createCourse(CourseDTO courseDTO) {
        log.info("📝 Création d'un nouveau cours: {}", courseDTO.getCode());

        // Validations
        validateCourseData(courseDTO, null);

        // Valeurs par défaut
        if (courseDTO.getMaxStudents() == null) {
            courseDTO.setMaxStudents(30);
        }
        if (courseDTO.getCredits() == null) {
            courseDTO.setCredits(3);
        }

        Course course = convertToEntity(courseDTO);
        Course savedCourse = courseRepository.save(course);

        log.info("✅ Cours créé avec succès: {} (ID={})",
                savedCourse.getCode(), savedCourse.getId());

        return convertToDTO(savedCourse);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // MODIFICATION (UPDATE)
    // ════════════════════════════════════════════════════════════════════════════

    public CourseDTO updateCourse(Long id, CourseDTO courseDTO) {
        log.info("📝 Modification du cours ID: {}", id);

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));

        // Validations
        validateCourseData(courseDTO, id);

        // Validation: ne pas réduire maxStudents en dessous du nombre actuel d'inscrits
        if (courseDTO.getMaxStudents() != null) {
            long currentEnrollments = enrollmentRepository.countByCourseId(id);
            if (courseDTO.getMaxStudents() < currentEnrollments) {
                throw new BusinessException(String.format(
                        "Impossible de réduire à %d places. Il y a déjà %d étudiant(s) inscrit(s).",
                        courseDTO.getMaxStudents(), currentEnrollments));
            }
        }

        // Mise à jour des champs
        course.setName(courseDTO.getName());
        course.setCode(courseDTO.getCode());
        course.setDescription(courseDTO.getDescription());
        course.setCredits(courseDTO.getCredits());
        course.setMaxStudents(courseDTO.getMaxStudents());

        // Changement de filière
        if (courseDTO.getDepartmentId() != null) {
            Department department = departmentRepository.findById(courseDTO.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department", "id", courseDTO.getDepartmentId()));
            course.setDepartment(department);
        }

        Course updatedCourse = courseRepository.save(course);
        log.info("✅ Cours modifié avec succès");

        return convertToDTO(updatedCourse);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SUPPRESSION (DELETE) - STRATÉGIE STRICTE
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║  SUPPRESSION COURS - STRATÉGIE STRICTE (Option B)                        ║
     * ╠═══════════════════════════════════════════════════════════════════════════╣
     * ║  RÈGLES :                                                                 ║
     * ║  ⛔ BLOQUÉ si enrollments ACTIVE ou COMPLETED existent                   ║
     * ║  ✅ AUTORISÉ si seulement DROPPED ou FAILED                              ║
     * ║                                                                           ║
     * ║  Ordre de suppression :                                                   ║
     * ║  1. Vérifier qu'aucun enrollment ACTIVE/COMPLETED n'existe               ║
     * ║  2. Supprimer les enrollments DROPPED/FAILED                             ║
     * ║  3. Supprimer le cours                                                    ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     */
    @Transactional
    public void deleteCourse(Long id) {
        log.info("🗑️ ══════════════════════════════════════════════════════════");
        log.info("🗑️ SUPPRESSION COURS - ID: {}", id);
        log.info("🗑️ ══════════════════════════════════════════════════════════");

        // Vérifier que le cours existe
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));

        String courseName = course.getName();
        String courseCode = course.getCode();
        log.info("📋 Cours trouvé: {} ({}) - ID={}", courseName, courseCode, id);

        // ═══ ÉTAPE 1: Récupérer et analyser les enrollments ═══
        List<Enrollment> allEnrollments = enrollmentRepository.findByCourseId(id);
        log.info("📊 Nombre total d'enrollments: {}", allEnrollments.size());

        // Séparer par statut
        List<Enrollment> activeOrCompleted = allEnrollments.stream()
                .filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.ACTIVE ||
                        e.getStatus() == Enrollment.EnrollmentStatus.COMPLETED)
                .collect(Collectors.toList());

        List<Enrollment> droppedOrFailed = allEnrollments.stream()
                .filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.DROPPED ||
                        e.getStatus() == Enrollment.EnrollmentStatus.FAILED)
                .collect(Collectors.toList());

        log.info("   - ACTIVE/COMPLETED: {}", activeOrCompleted.size());
        log.info("   - DROPPED/FAILED: {}", droppedOrFailed.size());

        // ═══ ÉTAPE 2: Vérifier la règle stricte ═══
        if (!activeOrCompleted.isEmpty()) {
            log.warn("⛔ SUPPRESSION BLOQUÉE - Enrollments actifs/complétés détectés");

            // Construire un message d'erreur détaillé
            long activeCount = activeOrCompleted.stream()
                    .filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.ACTIVE)
                    .count();
            long completedCount = activeOrCompleted.stream()
                    .filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.COMPLETED)
                    .count();

            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append(String.format(
                    "Impossible de supprimer le cours '%s' (%s).\n", courseName, courseCode));

            if (activeCount > 0) {
                errorMsg.append(String.format("• %d étudiant(s) actuellement inscrit(s) (ACTIVE)\n", activeCount));
            }
            if (completedCount > 0) {
                errorMsg.append(String.format("• %d étudiant(s) ayant validé le cours (COMPLETED)\n", completedCount));
            }
            errorMsg.append("\nVeuillez d'abord désinscrire les étudiants actifs.");

            throw new BusinessException(errorMsg.toString());
        }

        // ═══ ÉTAPE 3: Supprimer les enrollments DROPPED/FAILED ═══
        if (!droppedOrFailed.isEmpty()) {
            log.info("🔄 Suppression de {} enrollment(s) DROPPED/FAILED...", droppedOrFailed.size());
            enrollmentRepository.deleteAll(droppedOrFailed);
            enrollmentRepository.flush();
            log.info("✅ Enrollments DROPPED/FAILED supprimés");
        }

        // ═══ ÉTAPE 4: Supprimer le cours ═══
        log.info("🔄 Suppression du cours...");
        courseRepository.delete(course);
        courseRepository.flush();

        log.info("✅ ══════════════════════════════════════════════════════════");
        log.info("✅ COURS SUPPRIMÉ AVEC SUCCÈS: {} ({}) - ID={}", courseName, courseCode, id);
        log.info("✅ ══════════════════════════════════════════════════════════");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // VALIDATIONS
    // ════════════════════════════════════════════════════════════════════════════

    private void validateCourseData(CourseDTO dto, Long excludeId) {
        // Code obligatoire et unique
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            throw new BusinessException("Le code du cours est obligatoire");
        }

        courseRepository.findByCode(dto.getCode()).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new BusinessException(
                        "Le code du cours '" + dto.getCode() + "' existe déjà");
            }
        });

        // Nom obligatoire
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BusinessException("Le nom du cours est obligatoire");
        }

        // Filière obligatoire (rattachement)
        if (dto.getDepartmentId() == null) {
            throw new BusinessException("Le cours doit être rattaché à une filière");
        }

        // Crédits positifs
        if (dto.getCredits() != null && dto.getCredits() <= 0) {
            throw new BusinessException("Le nombre de crédits doit être positif");
        }

        // MaxStudents positif
        if (dto.getMaxStudents() != null && dto.getMaxStudents() <= 0) {
            throw new BusinessException("Le nombre maximum d'étudiants doit être positif");
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CONVERSIONS DTO ↔ ENTITY
    // ════════════════════════════════════════════════════════════════════════════

    private CourseDTO convertToDTO(Course course) {
        CourseDTO dto = new CourseDTO();
        dto.setId(course.getId());
        dto.setName(course.getName());
        dto.setCode(course.getCode());
        dto.setDescription(course.getDescription());
        dto.setCredits(course.getCredits());
        dto.setMaxStudents(course.getMaxStudents());

        if (course.getDepartment() != null) {
            dto.setDepartmentId(course.getDepartment().getId());
            dto.setDepartmentName(course.getDepartment().getName());
        }

        // Utiliser le repository pour éviter le lazy loading
        int enrollmentCount = (int) enrollmentRepository.countByCourseId(course.getId());
        dto.setCurrentEnrollmentCount(enrollmentCount);
        dto.setIsFull(course.getMaxStudents() != null &&
                enrollmentCount >= course.getMaxStudents());

        return dto;
    }

    private Course convertToEntity(CourseDTO dto) {
        Course course = new Course();
        course.setName(dto.getName());
        course.setCode(dto.getCode());
        course.setDescription(dto.getDescription());
        course.setCredits(dto.getCredits());
        course.setMaxStudents(dto.getMaxStudents());

        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department", "id", dto.getDepartmentId()));
            course.setDepartment(department);
        }

        return course;
    }
}