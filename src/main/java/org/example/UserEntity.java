package org.example;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "users")
//Safe, targeted boilerplate reduction
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "studentId")
    private Integer id;

    @NotBlank(message = "Name cannot be empty or blank")
    @Column(name = "studentName")
    private String name;

    @Email(message = "Please provide a valid structured email address")
    @NotBlank(message = "Email is a required field")
    @Column(name = "studentEmail", unique = true)
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Column(name = "studentPassword")
    private String password;

    @NotBlank(message = "Contact number is required")
    @Column(name = "studentContact")
    private String contact;

}