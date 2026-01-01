package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDTO {
    private Long id;

    @NotBlank(message = "Department name is required")
    @Size(min = 2, max = 100, message = "Department name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Department code is required")
    @Size(min = 2, max = 10, message = "Department code must be between 2 and 10 characters")
    private String code;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}