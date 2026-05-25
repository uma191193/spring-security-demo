package org.example.service;

import org.example.UserEntity;
import org.example.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * ==============================================================================
 * ARCHITECTURAL LAYER: SERVICE IMPL (BUSINESS LOGIC CORES)
 * ==============================================================================
 *
 * @Service: A specialized stereotype annotation that inherits from {@code @Component}.
 * It registers this class as a managed bean within the Spring Application Context.
 * The Service layer is where core business rules, validations, transactions, and
 * data transformations live before committing changes to the database.
 * * DESIGN BEST PRACTICE: This class implements the 'UserService' interface.
 * Programming to interfaces ensures loose coupling, follows the Dependency Inversion
 * Principle, and simplifies mock testing.
 */
@Service
public class UserServiceImpl implements UserService {

    /**
     * @Autowired - Data Layer Dependency Injection.
     * Spring injects an implementation of 'UserRepository' (dynamically generated
     * by Spring Data JPA at runtime) to handle database CRUD operations.
     */
    @Autowired
    public UserRepository userRepository;

    /**
     * @Autowired - Security Component Dependency Injection.
     * Spring scans its Application Context container, locates the single active
     * bean matching the type 'PasswordEncoder' (defined as a BCryptPasswordEncoder
     * in your AppConfig class), and injects it here.
     */
    @Autowired
    public PasswordEncoder encoder;

    /**
     * Overrides the contract method from the UserService interface to cleanly
     * execute business rules and persist user records.
     * * @param userEntity - The un-saved data payload passed down from the Controller layer,
     * which initially contains a plain-text password.
     *
     * @return UserEntity - The finalized, saved entity containing its encrypted password
     * and its freshly assigned MySQL database primary key id.
     */
    @Override
    public UserEntity saveUser(UserEntity userEntity) {

        // 1. EXTRACT & SECURE THE PASSWORD
        // Captures the insecure plain text password (e.g., "123") provided by the client,
        // and passes it to the BCrypt cryptographic engine to generate a one-way secure hash.
        String encodedPassword = encoder.encode(userEntity.getPassword());

        // 2. MUTATE THE STATE
        // Overwrites the user's plain text password inside the entity object with the
        // new secure hash string ($2a$10$...). This prevents plain text leaks from ever hitting the DB.
        userEntity.setPassword(encodedPassword);

        // 3. PERSIST THE DATA
        // Triggers Hibernate's internal 'INSERT INTO users ...' SQL query via Spring Data JPA.
        // It returns a copy of the newly saved entity containing its native database auto-increment ID.
        return userRepository.save(userEntity);
    }
}