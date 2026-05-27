package org.example.configuration;

import lombok.SneakyThrows;
import org.example.filter.AppFilter;
import org.example.service.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * ==============================================================================
 * ARCHITECTURAL LAYER: SECURITY CONFIGURATION
 * ==============================================================================
 *
 * @Configuration: Tells Spring that this class contains one or more bean definition
 * methods. The Spring container processes this class to generate and manage those
 * beans within the Application Context.
 * * @EnableWebSecurity: Switches off Spring Boot's default autoconfigured security rules
 * and instructs Spring to apply the custom web security filters defined inside this class.
 */
@Configuration
@EnableWebSecurity
public class AppConfig {

    /**
     * ==============================================================================
     * 1. THE SECURITY FILTER CHAIN (THE GATEKEEPER)
     * ==============================================================================
     * This bean defines the series of servlet filters that intercept every single
     * incoming HTTP request before it can ever reach your RestControllers.
     * * * REFACTOR NOTE (CIRCULAR DEPENDENCY FIX): 'AppFilter' is now passed directly as a
     * method parameter instead of being field-injected via @Autowired at the class level.
     * This lazy-loads the filter context execution, breaking the chicken-and-egg dependency
     * loop between AppConfig and AppFilter.
     * * * @SneakyThrows: A Lombok annotation that removes the boilerplate of catching or
     * declaring checked exceptions (like Exception) thrown by HttpSecurity configuration methods.
     */
    @Bean
    @SneakyThrows
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, AppFilter appFilter) throws Exception {

        return httpSecurity
                // Disables Cross-Site Request Forgery (CSRF) protection. Safe to do here because
                // this is a stateless REST API using JWTs/Tokens rather than stateful web cookies.
                .csrf(AbstractHttpConfigurer::disable)

                // Configures URL authorization patterns using Lambda DSL syntax
                .authorizeHttpRequests(auth -> auth
                        // Exposes public endpoints (e.g., login, registration) completely open to the world
                        .requestMatchers("/user/**").permitAll()
                        // Any endpoint not matching the rule above explicitly demands valid authentication
                        .anyRequest().authenticated()
                )

                // Forces a Stateless architectural posture. Instructs Spring Security NEVER to
                // create an HTTP Session (HttpSession) or look for cookies to track users.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Plugs in our customized authentication logic engine
                .authenticationProvider(authenticationProvider())

                // Intercepts the request process to execute our custom filter ('appFilter') before
                // Spring's standard internal processing filter ('UsernamePasswordAuthenticationFilter') run.
                .addFilterBefore(appFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * ==============================================================================
     * 2. THE CRYPTOGRAPHIC PASSWORD ENCODER (THE CRYPTO ENGINE)
     * ==============================================================================
     * Defines how user credentials will be scrambled and verified.
     * * * Return Type: We return the generic interface 'PasswordEncoder'. This ensures
     * seamless loose-coupling when injecting this bean into services (like UserServiceImpl)
     * that require a generic 'PasswordEncoder'.
     * * * BCrypt: A secure, industry-standard cryptographic hashing algorithm. It is a
     * one-way slow hashing mechanism that automatically adds a random 'salt' value to
     * each password, neutralizing pre-computed lookups (like rainbow table attacks).
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * ==============================================================================
     * 3. CUSTOM USER DETAILS SERVICE (THE DATABASE IDENTITY BRIDGE)
     * ==============================================================================
     * Registers your custom application-specific user loading strategy as a managed bean.
     * * * Role: This acts as the central adapter bridging your persistent data layer (e.g., MySQL via JPA)
     * with Spring Security's infrastructure.
     * * * @Primary: Tells Spring that if multiple beans of type UserDetailsService exist
     * (such as our custom one and the in-memory one below), this bean must be favored
     * as the default choice across the dependency injection engine.
     * * * Underlying Mechanism: When someone attempts logging in, this component will intercept the
     * target username, execute a database search, and wrap your domain User entity inside a Spring
     * compliant 'UserDetails' object.
     */
    @Bean
    @Primary
    public UserDetailsServiceImpl userDetailsService() {
        return new UserDetailsServiceImpl();
    }

    /**
     * ==============================================================================
     * 4. IN-MEMORY USER CREDENTIAL REGISTRY (THE MOCK DATA STORE)
     * ==============================================================================
     * Creates an instance of InMemoryUserDetailsManager which implements UserDetailsService.
     * This acts as a localized mock database inside the computer's volatile RAM.
     * * * Dependency Injection: Spring automatically scans its bean registry, discovers our
     * 'passwordEncoder()' bean above, and injects it into the 'encoder' parameter below.
     */
    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager(BCryptPasswordEncoder encoder) {

        // User.builder() constructs an immutable UserDetails model.
        // encoder.encode("rawPassword") translates plain text into a hashed string
        // (e.g., "uma@123" becomes something like "$2a$10$eXpAmPlEhAsH...")

        // -------------------------------------------------------------------------
        // Registered Profile 1: "uma"
        // -------------------------------------------------------------------------
        UserDetails u1 = User.builder()
                .username("uma")
                .password(encoder.encode("uma@123"))
                .roles("USER") // Automatically maps to the GrantedAuthority structure "ROLE_USER"
                .disabled(false) // Account is fully active
                .build();

        // -------------------------------------------------------------------------
        // Registered Profile 2: "raju"
        // -------------------------------------------------------------------------
        UserDetails u2 = User.builder()
                .username("raju")
                .password(encoder.encode("raju@123"))
                .roles("USER")
                .disabled(false)
                .build();

        // -------------------------------------------------------------------------
        // Registered Profile 3: "john"
        // -------------------------------------------------------------------------
        UserDetails u3 = User.builder()
                .username("john")
                .password(encoder.encode("john@123"))
                .roles("USER")
                .disabled(false)
                .build();

        // Feeds the user credentials directly into Spring's active runtime memory layer
        return new InMemoryUserDetailsManager(u1, u2, u3);
    }

    /**
     * ==============================================================================
     * 5. DATA ACCESS OBJECT AUTHENTICATION PROVIDER (THE VALIDATION ENGINE)
     * ==============================================================================
     * The processing core where credential inspection actually happens.
     * * * Framework Requirement (v7.0+): Uses a mandatory parameterized constructor to force
     * the inclusion of a valid UserDetailsService at creation time.
     * * * Coordination Role: This component takes the raw username/password provided by the user,
     * requests your 'userDetailsService()' to pull down the registered profile from storage, and then
     * delegates to 'passwordEncoder()' to cross-check if the submitted credentials match the database hash.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService());

        authenticationProvider.setPasswordEncoder(passwordEncoder());

        return authenticationProvider;
    }

    /**
     * ==============================================================================
     * 6. THE AUTHENTICATION MANAGER (THE CENTRAL COMMAND SYSTEM)
     * ==============================================================================
     * The primary public interface exposed to your application controllers or filters for
     * processing authentication requests.
     * * * Delegation Strategy: The AuthenticationManager doesn't perform checks directly. Instead,
     * it behaves like a conductor, passing incoming authentication tokens across a list of configured
     * Providers (such as your DaoAuthenticationProvider above) until one successfully validates the user.
     * * * AuthenticationConfiguration: A built-in helper utility where Spring aggregates global
     * system configurations to construct a unified runtime manager instance safely.
     */
    @Bean
    @SneakyThrows
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }
}