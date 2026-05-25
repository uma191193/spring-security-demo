package org.example;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity // Marks this class as a JPA entity mapped to a database table
@Table(name = "users") // Explicitly names the target MySQL table as "users"
@Getter // Lombok: Generates getter methods for all fields
@Setter // Lombok: Generates setter methods for all fields
@NoArgsConstructor // Lombok: Generates the mandatory public no-argument constructor for Hibernate
@AllArgsConstructor // Lombok: Generates a constructor with all fields
@ToString(exclude = "password") // Lombok: Generates toString(), excluding password for security logging
public class UserEntity {

    // -------------------------------------------------------------------------
    // PRIMARY KEY CONFIGURATION
    // -------------------------------------------------------------------------
    @Id // Identifies this field as the unique Primary Key for the table
    // IDENTITY: Relies on MySQL's native AUTO_INCREMENT mechanism.
    // It prevents ID-skipping gaps caused by default Hibernate sequences.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Maps the 'id' field to the 'student_id' column in the database.
    // Note: Spring Boot automatically converts camelCase (studentId) to snake_case (student_id) by default.
    @Column(name = "student_id")
    private Integer id;

    // -------------------------------------------------------------------------
    // DATA FIELDS & VALIDATIONS
    // -------------------------------------------------------------------------

    // @NotBlank: Validation annotation checked at the Spring/application level.
    // It catches errors BEFORE hitting the database if a user passes null, "" or "   ".
    @NotBlank(message = "Name cannot be empty or blank")
    @Column(name = "student_name") // Maps to the 'student_name' column
    private String name;

    @Email(message = "Please provide a valid structured email address") // Validates syntax (e.g., must contain @ and .)
    @NotBlank(message = "Email is a required field")
    // unique = true: Hands an index rule over to MySQL forcing the column to reject duplicate emails.
    @Column(name = "student_email", unique = true)
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Column(name = "student_password")
    private String password;

    @NotBlank(message = "Contact number is required")
    @Column(name = "student_contact")
    private String contact;

}