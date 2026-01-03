package com.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Department extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Course> courses = new ArrayList<>();

    @OneToMany(mappedBy = "department", fetch = FetchType.EAGER)
    private List<Student> students = new ArrayList<>();

//    @OneToMany(mappedBy = "department")
//    private List<Teacher> teachers = new ArrayList<>();
}