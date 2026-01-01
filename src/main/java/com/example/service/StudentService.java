package com.example.service;

import com.example.dto.StudentDTO;
import com.example.entity.Department;
import com.example.entity.Student;
import com.example.exception.BusinessException;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.DepartmentRepository;
import com.example.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;

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

    public StudentDTO createStudent(StudentDTO studentDTO) {
        // Vérifier si le numéro étudiant existe déjà
        if (studentRepository.findByStudentNumber(studentDTO.getStudentNumber()).isPresent()) {
            throw new BusinessException("Student number already exists: " + studentDTO.getStudentNumber());
        }

        // Vérifier si l'email existe déjà
        if (studentRepository.findByEmail(studentDTO.getEmail()).isPresent()) {
            throw new BusinessException("Email already exists: " + studentDTO.getEmail());
        }

        Student student = convertToEntity(studentDTO);
        Student savedStudent = studentRepository.save(student);
        return convertToDTO(savedStudent);
    }

    public StudentDTO updateStudent(Long id, StudentDTO studentDTO) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));

        student.setFirstName(studentDTO.getFirstName());
        student.setLastName(studentDTO.getLastName());
        student.setEmail(studentDTO.getEmail());
        student.setPhone(studentDTO.getPhone());
        student.setDateOfBirth(studentDTO.getDateOfBirth());

        if (studentDTO.getDepartmentId() != null) {
            Department department = departmentRepository.findById(studentDTO.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", studentDTO.getDepartmentId()));
            student.setDepartment(department);
        }

        Student updatedStudent = studentRepository.save(student);
        return convertToDTO(updatedStudent);
    }

    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
        studentRepository.delete(student);
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

    // Conversion methods
    private StudentDTO convertToDTO(Student student) {
        StudentDTO dto = new StudentDTO();
        dto.setId(student.getId());
        dto.setStudentNumber(student.getStudentNumber());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setEmail(student.getEmail());
        dto.setPhone(student.getPhone());
        dto.setDateOfBirth(student.getDateOfBirth());
        dto.setEnrollmentDate(student.getEnrollmentDate());

        if (student.getDepartment() != null) {
            dto.setDepartmentId(student.getDepartment().getId());
            dto.setDepartmentName(student.getDepartment().getName());
        }

        dto.setFullName(student.getFullName());

        return dto;
    }

    private Student convertToEntity(StudentDTO dto) {
        Student student = new Student();
        student.setStudentNumber(dto.getStudentNumber());
        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        student.setEmail(dto.getEmail());
        student.setPhone(dto.getPhone());
        student.setDateOfBirth(dto.getDateOfBirth());
        student.setEnrollmentDate(dto.getEnrollmentDate());

        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", dto.getDepartmentId()));
            student.setDepartment(department);
        }

        return student;
    }
}