package com.example.service;

import com.example.dto.DepartmentDTO;
import com.example.entity.Department;
import com.example.exception.BusinessException;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.CourseRepository;
import com.example.repository.DepartmentRepository;
import com.example.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * SERVICE FILIÈRE - SANS TEACHER
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * RÈGLE DE SUPPRESSION STRICTE :
 * - ⛔ BLOQUÉ si des ÉTUDIANTS sont affectés
 * - ⛔ BLOQUÉ si des COURS sont rattachés
 * - ✅ AUTORISÉ uniquement si vide
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    // ════════════════════════════════════════════════════════════════════════════
    // LECTURE (READ)
    // ════════════════════════════════════════════════════════════════════════════

    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findAll()
                .stream()
                .map(this::convertToDTOWithStats)
                .collect(Collectors.toList());
    }

    public DepartmentDTO getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        return convertToDTOWithStats(department);
    }

    public DepartmentDTO getDepartmentByCode(String code) {
        Department department = departmentRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "code", code));
        return convertToDTOWithStats(department);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CRÉATION (CREATE)
    // ════════════════════════════════════════════════════════════════════════════

    public DepartmentDTO createDepartment(DepartmentDTO departmentDTO) {
        log.info("📝 Création d'une nouvelle filière: {}", departmentDTO.getCode());

        validateDepartmentData(departmentDTO, null);

        Department department = convertToEntity(departmentDTO);
        Department saved = departmentRepository.save(department);
        departmentRepository.flush();

        log.info("✅ Filière créée avec succès: {} (ID={})", saved.getCode(), saved.getId());

        return convertToDTOSimple(saved);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // MODIFICATION (UPDATE)
    // ════════════════════════════════════════════════════════════════════════════

    public DepartmentDTO updateDepartment(Long id, DepartmentDTO departmentDTO) {
        log.info("📝 Modification de la filière ID: {}", id);

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        validateDepartmentData(departmentDTO, id);

        department.setCode(departmentDTO.getCode());
        department.setName(departmentDTO.getName());
        department.setDescription(departmentDTO.getDescription());

        Department updated = departmentRepository.save(department);
        departmentRepository.flush();

        log.info("✅ Filière modifiée avec succès");

        return convertToDTOSimple(updated);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SUPPRESSION (DELETE) - STRATÉGIE STRICTE
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║  SUPPRESSION FILIÈRE - STRATÉGIE STRICTE (SANS TEACHER)                  ║
     * ╠═══════════════════════════════════════════════════════════════════════════╣
     * ║  ⛔ BLOQUÉ si des ÉTUDIANTS sont affectés                                ║
     * ║  ⛔ BLOQUÉ si des COURS sont rattachés                                   ║
     * ║  ✅ AUTORISÉ uniquement si la filière est vide                           ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     */
    @Transactional
    public void deleteDepartment(Long id) {
        log.info("🗑️ ══════════════════════════════════════════════════════════");
        log.info("🗑️ SUPPRESSION FILIÈRE - ID: {}", id);
        log.info("🗑️ ══════════════════════════════════════════════════════════");

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        String deptName = department.getName();
        String deptCode = department.getCode();
        log.info("📋 Filière trouvée: {} ({}) - ID={}", deptName, deptCode, id);

        // ═══ ÉTAPE 1: Vérifier les ÉTUDIANTS ═══
        long studentCount = studentRepository.findByDepartmentId(id).size();
        log.info("📊 Nombre d'étudiants: {}", studentCount);

        if (studentCount > 0) {
            log.warn("⛔ SUPPRESSION BLOQUÉE - Étudiants présents");
            throw new BusinessException(String.format(
                    "Impossible de supprimer la filière '%s' (%s).\n" +
                            "• %d étudiant(s) affecté(s) à cette filière.\n\n" +
                            "Actions possibles :\n" +
                            "• Transférer les étudiants vers une autre filière\n" +
                            "• Supprimer les étudiants individuellement",
                    deptName, deptCode, studentCount));
        }

        // ═══ ÉTAPE 2: Vérifier les COURS ═══
        long courseCount = courseRepository.findByDepartmentId(id).size();
        log.info("📊 Nombre de cours: {}", courseCount);

        if (courseCount > 0) {
            log.warn("⛔ SUPPRESSION BLOQUÉE - Cours présents");
            throw new BusinessException(String.format(
                    "Impossible de supprimer la filière '%s' (%s).\n" +
                            "• %d cours rattaché(s) à cette filière.\n\n" +
                            "Actions possibles :\n" +
                            "• Transférer les cours vers une autre filière\n" +
                            "• Supprimer les cours individuellement",
                    deptName, deptCode, courseCount));
        }

        // ═══ ÉTAPE 3: Suppression autorisée ═══
        log.info("✅ Aucune dépendance détectée - Suppression autorisée");
        log.info("🔄 Suppression de la filière...");

        departmentRepository.delete(department);
        departmentRepository.flush();

        log.info("✅ ══════════════════════════════════════════════════════════");
        log.info("✅ FILIÈRE SUPPRIMÉE AVEC SUCCÈS: {} ({}) - ID={}", deptName, deptCode, id);
        log.info("✅ ══════════════════════════════════════════════════════════");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // STATISTIQUES
    // ════════════════════════════════════════════════════════════════════════════

    public long getStudentCount(Long departmentId) {
        return studentRepository.findByDepartmentId(departmentId).size();
    }

    public long getCourseCount(Long departmentId) {
        return courseRepository.findByDepartmentId(departmentId).size();
    }

    public boolean canDelete(Long departmentId) {
        long studentCount = studentRepository.findByDepartmentId(departmentId).size();
        long courseCount = courseRepository.findByDepartmentId(departmentId).size();
        return studentCount == 0 && courseCount == 0;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // VALIDATIONS
    // ════════════════════════════════════════════════════════════════════════════

    private void validateDepartmentData(DepartmentDTO dto, Long excludeId) {
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            throw new BusinessException("Le code de la filière est obligatoire");
        }

        if (!dto.getCode().matches("^[A-Za-z0-9]+$")) {
            throw new BusinessException(
                    "Le code de la filière doit être alphanumérique uniquement (A-Z, 0-9)");
        }

        departmentRepository.findByCode(dto.getCode()).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new BusinessException(
                        "Le code de filière '" + dto.getCode() + "' existe déjà");
            }
        });

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BusinessException("Le nom de la filière est obligatoire");
        }

        departmentRepository.findByName(dto.getName()).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new BusinessException(
                        "Une filière avec le nom '" + dto.getName() + "' existe déjà");
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CONVERSIONS
    // ════════════════════════════════════════════════════════════════════════════

    private DepartmentDTO convertToDTOWithStats(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setCode(department.getCode());
        dto.setName(department.getName());
        dto.setDescription(department.getDescription());

        dto.setStudentCount((int) studentRepository.findByDepartmentId(department.getId()).size());
        dto.setCourseCount((int) courseRepository.findByDepartmentId(department.getId()).size());

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
        dto.setStudentCount(0);
        dto.setCourseCount(0);
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