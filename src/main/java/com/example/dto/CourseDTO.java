package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private Long id;

    @NotBlank(message = "Course name is required")
    private String name;

    @NotBlank(message = "Course code is required")
    private String code;

    private String description;

    @Positive(message = "Credits must be positive")
    private Integer credits;

    @Positive(message = "Max students must be positive")
    private Integer maxStudents;

    @NotNull(message = "Department is required")
    private Long departmentId;

    private String departmentName;

    private Long teacherId;
    private String teacherName;

    private Integer currentEnrollmentCount;
    private Boolean isFull;
}