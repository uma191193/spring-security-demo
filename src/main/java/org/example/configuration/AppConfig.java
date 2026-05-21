package org.example.configuration;

import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // Marks this class as a source of bean definitions for the Spring context
@EnableWebSecurity // Enables Spring Security's web security support and integrates it with Spring MVC
public class AppConfig {

    /**
     * Configures the SecurityFilterChain bean.
     * This bean defines the filter chain that every incoming HTTP request must pass through.
     * * @SneakyThrows is a Lombok annotation that implicitly sneaks checked exceptions
     * (like Exception thrown by httpSecurity methods) out of the method without requiring a throws clause.
     */
    @Bean
    @SneakyThrows
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

        // 1. Configure URL-based Authorization Rules
        // Spring Security evaluates these rules from top to bottom (most specific to most general).
        httpSecurity.authorizeHttpRequests(httpReq -> httpReq
                // Allows completely unrestricted access to the "/contact" endpoint (no login required)
                .requestMatchers("/contact").permitAll()

                // Forces ALL other endpoints not matched above to require successful authentication
                .anyRequest().authenticated()
        );

        // 2. Enable HTTP Basic Authentication
        // This prompts the browser/client for a standard username/password via an HTTP header (Authorization: Basic ...).
        // Customizer.withDefaults() uses the out-of-the-box Spring Security default configurations.
        httpSecurity.httpBasic(Customizer.withDefaults());

        // 3. Enable Form-Based Login Authentication
        // This automatically generates a default, user-friendly HTML login page at "/login"
        // and handles the submission of login credentials via a POST request.
        httpSecurity.formLogin(Customizer.withDefaults());

        // Finalize, compile, and return the configured security filter chain object
        return httpSecurity.build();
    }

    /**
     * Defines the PasswordEncoder Bean using the BCrypt hashing algorithm.
     * * WHY BCRYPT?
     * BCrypt is a secure, one-way cryptographic hash function. It automatically incorporates
     * a random "salt" for each password encryption, protecting against Rainbow Table attacks.
     * * HOW SPRING SECURITY USES IT:
     * When a user logs in, Spring Security takes the raw password input, hashes it using
     * this bean, and compares that generated hash with the stored hash.
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures an in-memory user repository.
     * * DEPENDENCY INJECTION:
     * Spring automatically injects the `bCryptPasswordEncoder` bean defined above into the
     * `encoder` parameter.
     * * WHAT IS INMEMORYUSERDETAILSMANAGER?
     * It is an implementation of `UserDetailsService` and `UserDetailsManager`. It stores
     * user credentials directly in the server's RAM memory. Great for testing, prototyping,
     * or quick setups, but not meant for dynamic production environments where database persistence is needed.
     */
    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager(BCryptPasswordEncoder encoder) {

        // -------------------------------------------------------------------------
        // User 1: "uma"
        // -------------------------------------------------------------------------
        UserDetails u1 = User.builder()
                .username("uma") // Sets the unique login identifier

                // encoder.encode() converts the plain text "uma@123" into a BCrypt hash string.
                // The stored password will look something like: $2a$10$eXpAmPlEhAsH...
                .password(encoder.encode("uma@123"))

                // Assigns the 'USER' role. Under the hood, Spring Security prefixes this
                // to create an GrantedAuthority named "ROLE_USER".
                .roles("USER")

                // disabled(false) explicitly states the account is active.
                // If set to true, authentication attempts will fail with a DisabledException.
                .disabled(false)
                .build(); // Validates properties and compiles into an immutable UserDetails object

        // -------------------------------------------------------------------------
        // User 2: "raju"
        // -------------------------------------------------------------------------
        UserDetails u2 = User.builder()
                .username("raju")
                .password(encoder.encode("raju@123"))
                .roles("USER")
                .disabled(false)
                .build();

        // -------------------------------------------------------------------------
        // User 3: "john"
        // -------------------------------------------------------------------------
        UserDetails u3 = User.builder()
                .username("john")
                .password(encoder.encode("john@123"))
                .roles("USER")
                .disabled(false)
                .build();

        // Initializes the memory-based registry containing our pre-defined collection of users
        return new InMemoryUserDetailsManager(u1, u2, u3);
    }
}