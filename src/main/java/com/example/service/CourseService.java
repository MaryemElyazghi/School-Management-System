package com.example.service;

import com.example.dto.CourseDTO;
import com.example.entity.Course;
import com.example.entity.Department;
import com.example.entity.Teacher;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.CourseRepository;
import com.example.repository.DepartmentRepository;
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
        Course course = convertToEntity(courseDTO);
        Course savedCourse = courseRepository.save(course);
        return convertToDTO(savedCourse);
    }

    public CourseDTO updateCourse(Long id, CourseDTO courseDTO) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));

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