package com.example.service;

import com.example.dto.StudentDTO;
import com.example.entity.Department;
import com.example.entity.DossierAdministratif;
import com.example.entity.Enrollment;
import com.example.entity.Student;
import com.example.exception.BusinessException;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.DepartmentRepository;
import com.example.repository.DossierAdministratifRepository;
import com.example.repository.EnrollmentRepository;
import com.example.repository.StudentRepository;
import com.example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * SERVICE ÉTUDIANT - VERSION CORRIGÉE (SUPPRESSION FONCTIONNELLE)
 * ═══════════════════════════════════════════════════════════════════════════════
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final DossierAdministratifRepository dossierRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    // ════════════════════════════════════════════════════════════════════════════
    // LECTURE (READ)
    // ════════════════════════════════════════════════════════════════════════════

    public List<StudentDTO> getAllStudents() {
        return studentRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public StudentDTO getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
        return convertToDTO(student);
    }

    public StudentDTO getStudentByUserId(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "userId", userId));
        return convertToDTO(student);
    }

    public List<StudentDTO> getStudentsByDepartment(Long departmentId) {
        return studentRepository.findByDepartmentId(departmentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<StudentDTO> searchStudents(String keyword) {
        return studentRepository.searchStudents(keyword)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CRÉATION (CREATE)
    // ════════════════════════════════════════════════════════════════════════════

    public StudentDTO createStudent(StudentDTO studentDTO) {
        log.info("📝 Création d'un nouvel étudiant: {} {}",
                studentDTO.getFirstName(), studentDTO.getLastName());

        validateStudentData(studentDTO, null);

        if (studentDTO.getEnrollmentDate() == null) {
            studentDTO.setEnrollmentDate(LocalDate.now());
        }

        Student student = convertToEntity(studentDTO);
        Student savedStudent = studentRepository.save(student);
        studentRepository.flush();
        log.info("✅ Étudiant créé avec ID: {}", savedStudent.getId());

        DossierAdministratif dossier = createDossierAdministratif(savedStudent);
        savedStudent.setDossierAdministratif(dossier);
        savedStudent = studentRepository.save(savedStudent);
        studentRepository.flush();

        log.info("✅ Dossier administratif créé: {}", dossier.getNumeroInscription());

        return convertToDTO(savedStudent);
    }

    private DossierAdministratif createDossierAdministratif(Student student) {
        DossierAdministratif dossier = new DossierAdministratif();
        dossier.setDateCreation(LocalDate.now());
        dossier.setStudent(student);

        String departmentCode = student.getDepartment() != null
                ? student.getDepartment().getCode()
                : "UNKNOWN";
        dossier.generateNumeroInscription(departmentCode, student.getId());

        DossierAdministratif saved = dossierRepository.save(dossier);
        dossierRepository.flush();
        return saved;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // MODIFICATION (UPDATE)
    // ════════════════════════════════════════════════════════════════════════════

    public StudentDTO updateStudent(Long id, StudentDTO studentDTO) {
        log.info("📝 Modification de l'étudiant ID: {}", id);

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));

        validateStudentData(studentDTO, id);

        student.setFirstName(studentDTO.getFirstName());
        student.setLastName(studentDTO.getLastName());
        student.setEmail(studentDTO.getEmail());
        student.setPhone(studentDTO.getPhone());
        student.setDateOfBirth(studentDTO.getDateOfBirth());

        if (studentDTO.getDepartmentId() != null &&
                !studentDTO.getDepartmentId().equals(student.getDepartment().getId())) {

            Department newDept = departmentRepository.findById(studentDTO.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department", "id", studentDTO.getDepartmentId()));
            student.setDepartment(newDept);
        }

        Student updatedStudent = studentRepository.save(student);
        studentRepository.flush();
        log.info("✅ Étudiant modifié avec succès");

        return convertToDTO(updatedStudent);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SUPPRESSION (DELETE) - VERSION CORRIGÉE
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║  SUPPRESSION ÉTUDIANT - ORDRE CRITIQUE                                   ║
     * ╠═══════════════════════════════════════════════════════════════════════════╣
     * ║  1. Supprimer tous les Enrollments                                        ║
     * ║  2. Supprimer le DossierAdministratif                                    ║
     * ║  3. Casser la relation User ↔ Student (si User existe)                   ║
     * ║  4. Supprimer le Student                                                  ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     */
    @Transactional
    public void deleteStudent(Long id) {
        log.info("🗑️ ══════════════════════════════════════════════════════════");
        log.info("🗑️ SUPPRESSION ÉTUDIANT - ID: {}", id);
        log.info("🗑️ ══════════════════════════════════════════════════════════");

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));

        String studentName = student.getFullName();
        log.info("📋 Étudiant trouvé: {} (ID={})", studentName, id);

        // ═══ ÉTAPE 1: Supprimer tous les ENROLLMENTS ═══
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(id);
        log.info("📊 Enrollments trouvés: {}", enrollments.size());

        if (!enrollments.isEmpty()) {
            log.info("🔄 Suppression des enrollments...");
            enrollmentRepository.deleteAll(enrollments);
            enrollmentRepository.flush();
            log.info("✅ {} enrollment(s) supprimé(s)", enrollments.size());
        }

        // ═══ ÉTAPE 2: Supprimer le DOSSIER ADMINISTRATIF ═══
        DossierAdministratif dossier = student.getDossierAdministratif();
        if (dossier != null) {
            Long dossierId = dossier.getId();
            String numInscription = dossier.getNumeroInscription();
            log.info("🔄 Suppression du dossier: {} (ID={})", numInscription, dossierId);

            // Casser la relation bidirectionnelle AVANT suppression
            student.setDossierAdministratif(null);
            dossier.setStudent(null);

            studentRepository.save(student);
            studentRepository.flush();

            dossierRepository.delete(dossier);
            dossierRepository.flush();
            log.info("✅ Dossier administratif supprimé");
        }

        // ═══ ÉTAPE 3: Gérer la relation USER ═══
        if (student.getUser() != null) {
            log.info("🔄 Relation User détectée - Cassage de la relation...");
            var user = student.getUser();

            // Casser la relation bidirectionnelle
            student.setUser(null);
            user.setStudent(null);

            studentRepository.save(student);
            studentRepository.flush();

            userRepository.save(user);
            userRepository.flush();
            log.info("✅ Relation User cassée");
        }

        // ═══ ÉTAPE 4: Supprimer l'ÉTUDIANT ═══
        log.info("🔄 Suppression finale de l'étudiant...");

        // Recharger pour avoir l'état propre
        student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));

        studentRepository.delete(student);
        studentRepository.flush();

        // Vérification
        boolean stillExists = studentRepository.existsById(id);
        if (stillExists) {
            log.error("❌ ERREUR: L'étudiant existe toujours après delete!");
            throw new BusinessException("Échec de la suppression de l'étudiant");
        }

        log.info("✅ ══════════════════════════════════════════════════════════");
        log.info("✅ ÉTUDIANT SUPPRIMÉ AVEC SUCCÈS: {} (ID={})", studentName, id);
        log.info("✅ ══════════════════════════════════════════════════════════");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // VALIDATIONS
    // ════════════════════════════════════════════════════════════════════════════

    private void validateStudentData(StudentDTO dto, Long excludeId) {
        if (dto.getFirstName() == null || dto.getFirstName().trim().isEmpty()) {
            throw new BusinessException("Le prénom est obligatoire");
        }

        if (dto.getLastName() == null || dto.getLastName().trim().isEmpty()) {
            throw new BusinessException("Le nom est obligatoire");
        }

        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new BusinessException("L'email est obligatoire");
        }

        studentRepository.findByEmail(dto.getEmail()).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new BusinessException(
                        "L'email '" + dto.getEmail() + "' est déjà utilisé");
            }
        });

        if (dto.getDepartmentId() == null) {
            throw new BusinessException("La filière est obligatoire");
        }

        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            if (!isValidPhoneNumber(dto.getPhone())) {
                throw new BusinessException("Format du numéro de téléphone invalide");
            }
        }
    }

    private boolean isValidPhoneNumber(String phone) {
        String phoneRegex = "^[+]?[(]?[0-9]{1,4}[)]?[-\\s./0-9]*$";
        return phone.matches(phoneRegex) &&
                phone.replaceAll("[^0-9]", "").length() >= 8;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CONVERSIONS
    // ════════════════════════════════════════════════════════════════════════════

    private StudentDTO convertToDTO(Student student) {
        StudentDTO dto = new StudentDTO();
        dto.setId(student.getId());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setEmail(student.getEmail());
        dto.setPhone(student.getPhone());
        dto.setDateOfBirth(student.getDateOfBirth());
        dto.setEnrollmentDate(student.getEnrollmentDate());

        if (student.getDepartment() != null) {
            dto.setDepartmentId(student.getDepartment().getId());
            dto.setDepartmentName(student.getDepartment().getName());
            dto.setDepartmentCode(student.getDepartment().getCode());
        }

        dto.setFullName(student.getFullName());

        if (student.getDossierAdministratif() != null) {
            dto.setNumeroInscription(
                    student.getDossierAdministratif().getNumeroInscription());
            dto.setDossierDateCreation(
                    student.getDossierAdministratif().getDateCreation());
        }

        return dto;
    }

    private Student convertToEntity(StudentDTO dto) {
        Student student = new Student();
        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        student.setEmail(dto.getEmail());
        student.setPhone(dto.getPhone());
        student.setDateOfBirth(dto.getDateOfBirth());
        student.setEnrollmentDate(dto.getEnrollmentDate());

        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department", "id", dto.getDepartmentId()));
            student.setDepartment(department);
        }

        return student;
    }
}