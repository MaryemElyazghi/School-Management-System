package com.example.service;

import com.example.dto.CourseDTO;
import com.example.entity.Course;
import com.example.entity.Department;
import com.example.entity.Teacher;
import com.example.exception.BusinessException;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.CourseRepository;
import com.example.repository.DepartmentRepository;
import com.example.repository.EnrollmentRepository;
import com.example.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;

    /**
     * RÈGLE MÉTIER : Un étudiant ne peut voir QUE les cours de SA filière
     * auxquels il est inscrit
     */
    public List<CourseDTO> getCoursesForStudent(Long studentId, Long departmentId) {
        // Récupérer les cours auxquels l'étudiant est inscrit
        return courseRepository.findCoursesByStudentId(studentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les cours disponibles pour inscription (de la filière de l'étudiant)
     */
    public List<CourseDTO> getAvailableCoursesForDepartment(Long departmentId) {
        return courseRepository.findAvailableCoursesForDepartment(departmentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Vérifier si un étudiant peut accéder à un cours
     */
    public boolean canStudentAccessCourse(Long studentId, Long courseId) {
        return courseRepository.findCoursesByStudentId(studentId)
                .stream()
                .anyMatch(course -> course.getId().equals(courseId));
    }

    /**
     * Récupérer un cours SEULEMENT si l'étudiant y a accès
     */
    public CourseDTO getCourseForStudent(Long studentId, Long courseId) {
        if (!canStudentAccessCourse(studentId, courseId)) {
            throw new AccessDeniedException("You don't have access to this course");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        return convertToDTO(course);
    }

    // Methods pour ADMIN/TEACHER

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

    public CourseDTO createCourse(CourseDTO courseDTO) {
        // Vérifier si le code du cours existe déjà
        if (courseRepository.findByCode(courseDTO.getCode()).isPresent()) {
            throw new BusinessException("Course code already exists: " + courseDTO.getCode());
        }

        // Validation: maxStudents doit être positif
        if (courseDTO.getMaxStudents() != null && courseDTO.getMaxStudents() <= 0) {
            throw new BusinessException("Maximum students must be a positive number");
        }

        // Validation: credits doit être positif
        if (courseDTO.getCredits() != null && courseDTO.getCredits() <= 0) {
            throw new BusinessException("Credits must be a positive number");
        }

        Course course = convertToEntity(courseDTO);
        Course savedCourse = courseRepository.save(course);
        return convertToDTO(savedCourse);
    }

    public CourseDTO updateCourse(Long id, CourseDTO courseDTO) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));

        // Vérifier si le code du cours existe déjà pour un autre cours
        if (!course.getCode().equals(courseDTO.getCode())) {
            courseRepository.findByCode(courseDTO.getCode())
                    .ifPresent(existingCourse -> {
                        if (!existingCourse.getId().equals(id)) {
                            throw new BusinessException("Course code already exists: " + courseDTO.getCode());
                        }
                    });
        }

        // Validation: maxStudents doit être positif
        if (courseDTO.getMaxStudents() != null && courseDTO.getMaxStudents() <= 0) {
            throw new BusinessException("Maximum students must be a positive number");
        }

        // Validation: credits doit être positif
        if (courseDTO.getCredits() != null && courseDTO.getCredits() <= 0) {
            throw new BusinessException("Credits must be a positive number");
        }

        // Validation: ne pas réduire maxStudents en dessous du nombre d'inscrits actuels
        if (courseDTO.getMaxStudents() != null) {
            long currentEnrollments = enrollmentRepository.countByCourseId(id);
            if (courseDTO.getMaxStudents() < currentEnrollments) {
                throw new BusinessException(
                        String.format("Cannot set max students to %d. There are already %d students enrolled.",
                                courseDTO.getMaxStudents(), currentEnrollments));
            }
        }

        course.setName(courseDTO.getName());
        course.setCode(courseDTO.getCode());
        course.setDescription(courseDTO.getDescription());
        course.setCredits(courseDTO.getCredits());
        course.setMaxStudents(courseDTO.getMaxStudents());

        if (courseDTO.getDepartmentId() != null) {
            Department department = departmentRepository.findById(courseDTO.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", courseDTO.getDepartmentId()));
            course.setDepartment(department);
        }

        if (courseDTO.getTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(courseDTO.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", courseDTO.getTeacherId()));
            course.setTeacher(teacher);
        }

        Course updatedCourse = courseRepository.save(course);
        return convertToDTO(updatedCourse);
    }

    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));

        // Vérifier s'il y a des inscriptions pour ce cours
        long enrollmentCount = enrollmentRepository.countByCourseId(id);
        if (enrollmentCount > 0) {
            throw new BusinessException(
                    String.format("Cannot delete course '%s'. There are %d enrollment(s) associated with it.",
                            course.getName(), enrollmentCount));
        }

        courseRepository.delete(course);
    }

    public List<CourseDTO> getCoursesByDepartment(Long departmentId) {
        return courseRepository.findByDepartmentId(departmentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CourseDTO> searchCourses(String keyword) {
        return courseRepository.searchCourses(keyword)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Conversion methods
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

        if (course.getTeacher() != null) {
            dto.setTeacherId(course.getTeacher().getId());
            dto.setTeacherName(course.getTeacher().getFullName());
        }

        dto.setCurrentEnrollmentCount(course.getCurrentEnrollmentCount());
        dto.setIsFull(course.isFull());

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
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", dto.getDepartmentId()));
            course.setDepartment(department);
        }

        if (dto.getTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", dto.getTeacherId()));
            course.setTeacher(teacher);
        }

        return course;
    }
}