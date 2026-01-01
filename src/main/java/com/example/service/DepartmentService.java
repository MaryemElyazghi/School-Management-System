package com.example.service;

import com.example.dto.DepartmentDTO;
import com.example.entity.Department;
import com.example.exception.BusinessException;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.DepartmentRepository;
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

        department.setName(departmentDTO.getName());
        department.setCode(departmentDTO.getCode());
        department.setDescription(departmentDTO.getDescription());

        Department updated = departmentRepository.save(department);
        return convertToDTO(updated);
    }

    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        departmentRepository.delete(department);
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