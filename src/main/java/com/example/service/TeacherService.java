package com.example.service;

import com.example.dto.TeacherDTO;
import com.example.entity.Department;
import com.example.entity.Teacher;
import com.example.exception.BusinessException;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.CourseRepository;
import com.example.repository.DepartmentRepository;
import com.example.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;

    /**
     * Récupérer tous les enseignants
     */
    public List<TeacherDTO> getAllTeachers() {
        return teacherRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer un enseignant par ID
     */
    public TeacherDTO getTeacherById(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", id));
        return convertToDTO(teacher);
    }

    /**
     * Récupérer un enseignant par son ID utilisateur
     */
    public TeacherDTO getTeacherByUserId(Long userId) {
        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", userId));
        return convertToDTO(teacher);
    }

    /**
     * Créer un nouvel enseignant
     * RÈGLES MÉTIER:
     * - Le numéro d'employé doit être unique
     * - L'email doit être unique
     * - Le département doit exister
     */
    public TeacherDTO createTeacher(TeacherDTO teacherDTO) {
        // Vérifier si le numéro d'employé existe déjà
        if (teacherRepository.findByEmployeeNumber(teacherDTO.getEmployeeNumber()).isPresent()) {
            throw new BusinessException("Employee number already exists: " + teacherDTO.getEmployeeNumber());
        }

        // Vérifier si l'email existe déjà
        if (teacherRepository.findByEmail(teacherDTO.getEmail()).isPresent()) {
            throw new BusinessException("Email already exists: " + teacherDTO.getEmail());
        }

        // Validation du format du numéro de téléphone
        if (teacherDTO.getPhone() != null && !teacherDTO.getPhone().isEmpty()) {
            if (!isValidPhoneNumber(teacherDTO.getPhone())) {
                throw new BusinessException("Invalid phone number format: " + teacherDTO.getPhone());
            }
        }

        Teacher teacher = convertToEntity(teacherDTO);
        Teacher savedTeacher = teacherRepository.save(teacher);
        return convertToDTO(savedTeacher);
    }

    /**
     * Mettre à jour un enseignant
     * RÈGLES MÉTIER:
     * - Le numéro d'employé ne peut pas être modifié
     * - L'email doit rester unique
     */
    public TeacherDTO updateTeacher(Long id, TeacherDTO teacherDTO) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", id));

        // Vérifier si l'email existe déjà pour un autre enseignant
        if (!teacher.getEmail().equals(teacherDTO.getEmail())) {
            teacherRepository.findByEmail(teacherDTO.getEmail())
                    .ifPresent(existingTeacher -> {
                        if (!existingTeacher.getId().equals(id)) {
                            throw new BusinessException("Email already exists: " + teacherDTO.getEmail());
                        }
                    });
        }

        // Validation du format du numéro de téléphone
        if (teacherDTO.getPhone() != null && !teacherDTO.getPhone().isEmpty()) {
            if (!isValidPhoneNumber(teacherDTO.getPhone())) {
                throw new BusinessException("Invalid phone number format: " + teacherDTO.getPhone());
            }
        }

        // Mise à jour des champs (le numéro d'employé ne change pas)
        teacher.setFirstName(teacherDTO.getFirstName());
        teacher.setLastName(teacherDTO.getLastName());
        teacher.setEmail(teacherDTO.getEmail());
        teacher.setPhone(teacherDTO.getPhone());
        teacher.setSpecialization(teacherDTO.getSpecialization());
        teacher.setHireDate(teacherDTO.getHireDate());

        if (teacherDTO.getDepartmentId() != null) {
            Department department = departmentRepository.findById(teacherDTO.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", teacherDTO.getDepartmentId()));
            teacher.setDepartment(department);
        }

        Teacher updatedTeacher = teacherRepository.save(teacher);
        return convertToDTO(updatedTeacher);
    }

    /**
     * Supprimer un enseignant
     * RÈGLE MÉTIER: Ne peut pas supprimer un enseignant qui a des cours assignés
     */
    public void deleteTeacher(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", id));

        // Vérifier s'il y a des cours assignés à cet enseignant
        long courseCount = courseRepository.findByTeacherId(id).size();
        if (courseCount > 0) {
            throw new BusinessException(
                    String.format("Cannot delete teacher '%s'. There are %d course(s) assigned to this teacher.",
                            teacher.getFullName(), courseCount));
        }

        teacherRepository.delete(teacher);
    }

    /**
     * Récupérer les enseignants par département
     */
    public List<TeacherDTO> getTeachersByDepartment(Long departmentId) {
        return teacherRepository.findByDepartmentId(departmentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Rechercher des enseignants par mot-clé
     */
    public List<TeacherDTO> searchTeachers(String keyword) {
        return teacherRepository.findAll()
                .stream()
                .filter(teacher ->
                        teacher.getFirstName().toLowerCase().contains(keyword.toLowerCase()) ||
                        teacher.getLastName().toLowerCase().contains(keyword.toLowerCase()) ||
                        teacher.getEmail().toLowerCase().contains(keyword.toLowerCase()) ||
                        teacher.getEmployeeNumber().toLowerCase().contains(keyword.toLowerCase()) ||
                        (teacher.getSpecialization() != null &&
                         teacher.getSpecialization().toLowerCase().contains(keyword.toLowerCase())))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Valider le format du numéro de téléphone
     */
    private boolean isValidPhoneNumber(String phone) {
        // Format accepté: chiffres, espaces, tirets, parenthèses, + (minimum 8 chiffres)
        String phoneRegex = "^[+]?[(]?[0-9]{1,4}[)]?[-\\s./0-9]*$";
        return phone.matches(phoneRegex) && phone.replaceAll("[^0-9]", "").length() >= 8;
    }

    /**
     * Convertir une entité Teacher en DTO
     */
    private TeacherDTO convertToDTO(Teacher teacher) {
        TeacherDTO dto = new TeacherDTO();
        dto.setId(teacher.getId());
        dto.setEmployeeNumber(teacher.getEmployeeNumber());
        dto.setFirstName(teacher.getFirstName());
        dto.setLastName(teacher.getLastName());
        dto.setEmail(teacher.getEmail());
        dto.setPhone(teacher.getPhone());
        dto.setSpecialization(teacher.getSpecialization());
        dto.setHireDate(teacher.getHireDate());

        if (teacher.getDepartment() != null) {
            dto.setDepartmentId(teacher.getDepartment().getId());
            dto.setDepartmentName(teacher.getDepartment().getName());
        }

        dto.setFullName(teacher.getFullName());

        // Compter le nombre de cours enseignés
        int courseCount = courseRepository.findByTeacherId(teacher.getId()).size();
        dto.setCourseCount(courseCount);

        return dto;
    }

    /**
     * Convertir un DTO en entité Teacher
     */
    private Teacher convertToEntity(TeacherDTO dto) {
        Teacher teacher = new Teacher();
        teacher.setEmployeeNumber(dto.getEmployeeNumber());
        teacher.setFirstName(dto.getFirstName());
        teacher.setLastName(dto.getLastName());
        teacher.setEmail(dto.getEmail());
        teacher.setPhone(dto.getPhone());
        teacher.setSpecialization(dto.getSpecialization());
        teacher.setHireDate(dto.getHireDate());

        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", dto.getDepartmentId()));
            teacher.setDepartment(department);
        }

        return teacher;
    }
}
