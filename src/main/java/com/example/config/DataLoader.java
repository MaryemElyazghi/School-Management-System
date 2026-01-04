package com.example.config;

import com.example.entity.*;
import com.example.repository.*;
import jakarta.persistence.Id;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final DossierAdministratifRepository dossierRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (departmentRepository.count() == 0) {
            loadTestData();
        }
    }

    private void loadTestData() {
        log.info("Loading test data...");

        // ========== DEPARTMENTS (Filières) ==========
        Department ginf = createDepartment(
                "Computer Engineering",
                "GINF",
                "Génie Informatique - Computer Engineering"
        );

        Department gstr = createDepartment(
                "Telecommunications and Network Systems Engineering",
                "GSTR",
                "Génie des Systèmes de Télécommunications et Réseaux"
        );

        Department gil = createDepartment(
                "Industrial Engineering and Logistics",
                "GIL",
                "Génie Industriel et Logistique"
        );

        log.info("Created {} departments", departmentRepository.count());

        // ========== ADMIN USER ==========
        User adminUser = createUser("admin", "admin@school.com", "admin123", Role.ADMIN);
        log.info("Created admin user: admin/admin123");

        // ========== STUDENTS ==========

        // Étudiants GINF
        Student student1 = createStudent(1,"Ahmed", "Bennani", "ahmed.bennani@student.com",
                "0612345678", LocalDate.of(2000, 5, 15), ginf
        );
        User user1 = createUser("ahmed.bennani", "ahmed.bennani@student.com", "password", Role.STUDENT);
        student1.setUser(user1);
        studentRepository.save(student1);

        Student student2 = createStudent(
                2, "Fatima", "El Amrani", "fatima.elamrani@student.com",
                "0612345679", LocalDate.of(2001, 8, 22), ginf
        );
        User user2 = createUser("fatima.elamrani", "fatima.elamrani@student.com", "password", Role.STUDENT);
        student2.setUser(user2);
        studentRepository.save(student2);

        // Étudiants GSTR
        Student student3 = createStudent(
                 3,"Omar", "Tahiri", "omar.tahiri@student.com",
                "0612345680", LocalDate.of(2000, 11, 10), gstr
        );
        User user3 = createUser("omar.tahiri", "omar.tahiri@student.com", "password", Role.STUDENT);
        student3.setUser(user3);
        studentRepository.save(student3);

        Student student4 = createStudent(
                4, "Salma", "Benali", "salma.benali@student.com",
                "0612345681", LocalDate.of(2001, 3, 5), gstr
        );
        User user4 = createUser("salma.benali", "salma.benali@student.com", "password", Role.STUDENT);
        student4.setUser(user4);
        studentRepository.save(student4);

        // Étudiants GIL
        Student student5 = createStudent(
                5, "Youssef", "Alami", "youssef.alami@student.com",
                "0612345682", LocalDate.of(2000, 7, 18), gil
        );
        User user5 = createUser("youssef.alami", "youssef.alami@student.com", "password", Role.STUDENT);
        student5.setUser(user5);
        studentRepository.save(student5);

        log.info("Created {} students", studentRepository.count());

        // ========== TEACHERS ==========
        Teacher teacher1 = createTeacher(
                "PROF001", "Dr. Mohammed", "EL HADDAD", "m.alaoui@school.com",
                "0698765432", "Computer Science", ginf
        );

        Teacher teacher2 = createTeacher(
                "PROF002", "Dr. Aicha", "Sebti", "a.sebti@school.com",
                "0698765433", "Networks and Telecommunications", gstr
        );

        Teacher teacher3 = createTeacher(
                "PROF003", "Dr. Karim", "Hassani", "k.hassani@school.com",
                "0698765434", "Industrial Engineering", gil
        );

        log.info("Created {} teachers", teacherRepository.count());

        // ========== COURSES ==========

        // Cours GINF
        Course course1 = createCourse(
                "Programmation Java Avancée", "GINF301",
                "Concepts avancés de Java: POO, Collections, Streams, Spring Boot",
                6, 30, ginf, teacher1
        );

        Course course2 = createCourse(
                "Bases de Données", "GINF302",
                "Conception et gestion de bases de données relationnelles",
                5, 30, ginf, teacher1
        );

        Course course3 = createCourse(
                "Développement Web", "GINF303",
                "HTML, CSS, JavaScript, React, Spring Boot",
                6, 25, ginf, teacher1
        );

        // Cours GSTR
        Course course4 = createCourse(
                "Réseaux et Protocoles", "GSTR301",
                "TCP/IP, Routage, Switching, Sécurité réseau",
                6, 30, gstr, teacher2
        );

        Course course5 = createCourse(
                "Télécommunications", "GSTR302",
                "Systèmes de télécommunications modernes",
                5, 30, gstr, teacher2
        );

        // Cours GIL
        Course course6 = createCourse(
                "Gestion de Production", "GIL301",
                "Planification et contrôle de la production",
                6, 25, gil, teacher3
        );

        Course course7 = createCourse(
                "Logistique et Supply Chain", "GIL302",
                "Gestion de la chaîne logistique",
                5, 25, gil, teacher3
        );

        log.info("Created {} courses", courseRepository.count());

        // ========== ENROLLMENTS ==========

        // Ahmed (GINF) inscrit aux cours GINF
        createEnrollment(student1, course1, 15.5);
        createEnrollment(student1, course2, 14.0);
        createEnrollment(student1, course3, null); // Cours en cours

        // Fatima (GINF) inscrit aux cours GINF
        createEnrollment(student2, course1, 16.0);
        createEnrollment(student2, course2, null);

        // Omar (GSTR) inscrit aux cours GSTR
        createEnrollment(student3, course4, 13.5);
        createEnrollment(student3, course5, null);

        // Salma (GSTR) inscrit aux cours GSTR
        createEnrollment(student4, course4, null);

        // Youssef (GIL) inscrit aux cours GIL
        createEnrollment(student5, course6, 17.0);
        createEnrollment(student5, course7, null);

        log.info("Created {} enrollments", enrollmentRepository.count());

        log.info("===========================================");
        log.info("Test data loaded successfully!");
        log.info("===========================================");
        log.info("Admin credentials: admin / admin123");
        log.info("Student credentials examples:");
        log.info("  - ahmed.bennani / password (GINF)");
        log.info("  - omar.tahiri / password (GSTR)");
        log.info("  - youssef.alami / password (GIL)");
        log.info("===========================================");
    }

    private Department createDepartment(String name, String code, String description) {
        Department dept = new Department();
        dept.setName(name);
        dept.setCode(code);
        dept.setDescription(description);
        return departmentRepository.save(dept);
    }

    private User createUser(String username, String email, String password, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private Student createStudent(long id, String firstName, String lastName,
                                  String email, String phone, LocalDate dob, Department dept) {
        Student student = new Student();
        student.setId(id);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setPhone(phone);
        student.setDateOfBirth(dob);
        student.setEnrollmentDate(LocalDate.now().minusYears(2));
        student.setDepartment(dept);
        // Sauvegarder l'étudiant d'abord pour obtenir l'ID
        Student savedStudent = studentRepository.save(student);

        // ✅ CRÉATION AUTOMATIQUE DU DOSSIER ADMINISTRATIF
        DossierAdministratif dossier = new DossierAdministratif();
        dossier.setDateCreation(LocalDate.now());
        dossier.setStudent(savedStudent);

        // Générer le numéro d'inscription: FILIERE-ANNEE-ID
        dossier.generateNumeroInscription(dept.getCode(), savedStudent.getId());

        // Sauvegarder le dossier
        DossierAdministratif savedDossier = dossierRepository.save(dossier);

        // Lier le dossier à l'étudiant
        savedStudent.setDossierAdministratif(savedDossier);
        return studentRepository.save(savedStudent);    }

    private Teacher createTeacher(String empNumber, String firstName, String lastName,
                                  String email, String phone, String specialization, Department dept) {
        Teacher teacher = new Teacher();
        teacher.setEmployeeNumber(empNumber);
        teacher.setFirstName(firstName);
        teacher.setLastName(lastName);
        teacher.setEmail(email);
        teacher.setPhone(phone);
        teacher.setSpecialization(specialization);
        teacher.setHireDate(LocalDate.now().minusYears(5));
        teacher.setDepartment(dept);
        return teacherRepository.save(teacher);
    }

    private Course createCourse(String name, String code, String description,
                                int credits, int maxStudents, Department dept, Teacher teacher) {
        Course course = new Course();
        course.setName(name);
        course.setCode(code);
        course.setDescription(description);
        course.setCredits(credits);
        course.setMaxStudents(maxStudents);
        course.setDepartment(dept);
        course.setTeacher(teacher);
        return courseRepository.save(course);
    }

    private void createEnrollment(Student student, Course course, Double grade) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setEnrollmentDate(LocalDate.now().minusMonths(3));

        if (grade != null) {
            enrollment.setGrade(grade);
            enrollment.setStatus(grade >= 10 ?
                    Enrollment.EnrollmentStatus.COMPLETED :
                    Enrollment.EnrollmentStatus.FAILED);
        } else {
            enrollment.setStatus(Enrollment.EnrollmentStatus.ACTIVE);
        }

        enrollmentRepository.save(enrollment);
    }
}