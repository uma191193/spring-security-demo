package org.example.service;

import org.example.UserEntity;
import org.example.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;

/**
 * ==============================================================================
 * ARCHITECTURAL LAYER: SECURITY USER DATA PROVIDER (IDENTITY BRIDGE)
 * ==============================================================================
 * This class implements Spring Security's native {@link UserDetailsService} interface.
 * It serves as the primary data-retrieval engine for the security framework, decoupling
 * how user credentials are saved in your database from how Spring Security processes them.
 * * NOTE: This class is registered as a managed bean inside your AppConfig file and
 * passed directly into the DaoAuthenticationProvider constructor.
 */
public class UserDetailsServiceImpl implements UserDetailsService {

    /**
     * @Autowired - Performs Dependency Injection (DI) to connect your custom data access layer.
     * Spring injects the dynamic proxy instance of UserRepository here so we can safely query the
     * persistence tier (MySQL database via Spring Data JPA).
     */
    @Autowired
    public UserRepository userRepository;

    /**
     * ==============================================================================
     * CORE AUTHENTICATION HOOK: TRANSLATING DATABASE ENTITY TO SECURITY PRINCIPAL
     * ==============================================================================
     * Whenever a client submits credentials (via Form Login or HTTP Basic), Spring's
     * AuthenticationManager intercepts them and calls this exact method to find the user.
     * * * THE MULTI-STEP PIPELINE:
     * 1. Query: It pulls down your customized domain layer model (UserEntity) matching the input identifier.
     * 2. Translation: It transforms your raw database domain model into an immutable framework-compliant
     * instance of {@link UserDetails}.
     * 3. Verification: The returned UserDetails wrapper is then used by the framework to automatically
     * cross-check passwords and check account status.
     * * @param email - The unique identifier passed by the login client. (Even though the interface parameter
     * name says 'username', you are cleanly mapping your system's unique email attribute to serve that purpose).
     *
     * @return a fully populated UserDetails instance that Spring Security understands.
     * @throws UsernameNotFoundException if the input criteria cannot be matched in the storage system.
     */
    @Override
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {

        // Step 1: Hit the database to find the custom user account
        UserEntity byEmail = userRepository.findByEmail(email);

        // Defensive Design Warning: If 'byEmail' evaluates to null, executing methods on it below
        // will throw a NullPointerException. In strict enterprise designs, it is highly recommended
        // to do a null-check here and explicitly throw a new UsernameNotFoundException("User not found with email: " + email);

        // Step 2: Extract your database credentials and adapt them into Spring's native User object.
        // org.springframework.security.core.userdetails.User is a built-in blueprint that implements UserDetails.
        // - Arg 1: Unique ID (your system's email string)
        // - Arg 2: The storage password hash (retrieved from the DB so Spring can safely verify it)
        // - Arg 3: GrantedAuthorities collection. Collections.emptyList() means this user has no roles assigned yet.
        return new User(
                byEmail.getEmail(),
                byEmail.getPassword(),
                Collections.emptyList()
        );
    }
}