package com.example.service;

import com.example.dto.DepartmentDTO;
import com.example.entity.Department;
import com.example.exception.BusinessException;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.CourseRepository;
import com.example.repository.DepartmentRepository;
import com.example.repository.StudentRepository;
import com.example.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;

    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public DepartmentDTO getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        return convertToDTO(department);
    }

    public DepartmentDTO createDepartment(DepartmentDTO departmentDTO) {
        // Validation: le code doit être alphanumérique
        if (!isValidDepartmentCode(departmentDTO.getCode())) {
            throw new BusinessException("Department code must be alphanumeric (letters and numbers only)");
        }

        if (departmentRepository.existsByCode(departmentDTO.getCode())) {
            throw new BusinessException("Department code already exists: " + departmentDTO.getCode());
        }

        Department department = convertToEntity(departmentDTO);
        Department saved = departmentRepository.save(department);
        return convertToDTO(saved);
    }

    public DepartmentDTO updateDepartment(Long id, DepartmentDTO departmentDTO) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        // Vérifier si le code existe déjà pour un autre département
        if (!department.getCode().equals(departmentDTO.getCode())) {
            if (departmentRepository.existsByCode(departmentDTO.getCode())) {
                throw new BusinessException("Department code already exists: " + departmentDTO.getCode());
            }
        }

        // Validation: le code doit être alphanumérique
        if (!isValidDepartmentCode(departmentDTO.getCode())) {
            throw new BusinessException("Department code must be alphanumeric (letters and numbers only)");
        }

        department.setName(departmentDTO.getName());
        department.setCode(departmentDTO.getCode());
        department.setDescription(departmentDTO.getDescription());

        Department updated = departmentRepository.save(department);
        return convertToDTO(updated);
    }

    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        // Protection contre la suppression en cascade: vérifier les étudiants
        long studentCount = studentRepository.findByDepartmentId(id).size();
        if (studentCount > 0) {
            throw new BusinessException(
                    String.format("Cannot delete department '%s'. There are %d student(s) in this department.",
                            department.getName(), studentCount));
        }

        // Protection contre la suppression en cascade: vérifier les cours
        long courseCount = courseRepository.findByDepartmentId(id).size();
        if (courseCount > 0) {
            throw new BusinessException(
                    String.format("Cannot delete department '%s'. There are %d course(s) in this department.",
                            department.getName(), courseCount));
        }

        // Protection contre la suppression en cascade: vérifier les enseignants
        long teacherCount = teacherRepository.findByDepartmentId(id).size();
        if (teacherCount > 0) {
            throw new BusinessException(
                    String.format("Cannot delete department '%s'. There are %d teacher(s) in this department.",
                            department.getName(), teacherCount));
        }

        departmentRepository.delete(department);
    }

    /**
     * Valider le format du code de département (alphanumérique uniquement)
     */
    private boolean isValidDepartmentCode(String code) {
        return code != null && code.matches("^[A-Za-z0-9]+$");
    }

    private DepartmentDTO convertToDTO(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setName(department.getName());
        dto.setCode(department.getCode());
        dto.setDescription(department.getDescription());
        return dto;
    }

    private Department convertToEntity(DepartmentDTO dto) {
        Department department = new Department();
        department.setName(dto.getName());
        department.setCode(dto.getCode());
        department.setDescription(dto.getDescription());
        return department;
    }
}