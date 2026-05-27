package org.example.controller;

import org.example.UserEntity;
import org.example.service.JwtService;
import org.example.service.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ==============================================================================
 * ARCHITECTURAL LAYER: REST CONTROLLER (IDENTITY & ACCESS MANAGEMENT)
 * ==============================================================================
 * * @RestController - Core Spring MVC stereotype annotation that combines {@code @Controller} and {@code @ResponseBody}.
 * It registers this class as a request-handling component in the Spring Application Context.
 * Crucially, it tells Spring that the return value of every method should be serialized directly
 * into the HTTP response body (typically as JSON or XML) instead of looking for an HTML view template.
 *
 * @RequestMapping - Defines the base URL namespace for this entire controller.
 * All individual endpoint paths inside this class will be prefixed with "/user".
 * For example: http://localhost:9000/user
 */
@RestController
@RequestMapping(value = "/user")
public class UserController {

    /**
     * ==============================================================================
     * DEPENDENCY: BUSINESS LOGIC SERVICE ENGINE
     * ==============================================================================
     *
     * @Autowired - Performs Dependency Injection (DI).
     * Spring searches its Bean Factory container for an instance of UserServiceImpl,
     * manages its lifecycle, and automatically wires it into this controller layer at startup.
     * Note: Injecting the concrete class (UserServiceImpl) works fine, but it is generally a best
     * practice to inject the Interface (UserService) to maintain low coupling and support mocking in unit tests.
     */
    @Autowired
    public UserServiceImpl userService;

    /**
     * ==============================================================================
     * DEPENDENCY: JSON WEB TOKEN (JWT) UTILITY SERVICE
     * ==============================================================================
     *
     * @Autowired - Automatically injects the stateless token processing service.
     * This custom component is responsible for orchestrating the cryptographic generation,
     * signing, and verification procedures of short-lived JWT payloads distributed to
     * successfully validated API clients.
     */
    @Autowired
    private JwtService jwtService;

    /**
     * ==============================================================================
     * DEPENDENCY: CENTRALIZED AUTHENTICATION COORDINATOR
     * ==============================================================================
     *
     * @Autowired - Injects the centralized Spring Security execution coordinator.
     * This bean is defined in your AppConfig class. It serves as the primary gateway
     * to manually pass user-submitted credentials directly into Spring Security's validation engine.
     */
    @Autowired
    public AuthenticationManager authenticationManager;

    /**
     * ==============================================================================
     * ENDPOINT: USER LOGIN & CREDENTIAL ISSUANCE
     * ==============================================================================
     *
     * @PostMapping - Specialized mapping that routes HTTP POST requests hitting "/user/login" to this method.
     * @RequestBody - Extracts incoming JSON payload fields (email/password) and maps them to a UserEntity wrapper.
     * * * PROGRAMMATIC AUTHENTICATION FLOW:
     * 1. Token Packaging: Takes the raw username/password from the request body and encapsulates them
     * inside an unauthenticated UsernamePasswordAuthenticationToken instance.
     * 2. Processing Delegation: The unauthenticated token is passed directly to the authenticationManager.authenticate() method.
     * The manager routes it to your registered DaoAuthenticationProvider, which loads the true user record from storage via
     * your UserDetailsService, computes and validates the hashed password, and returns a fully authorized Authentication token.
     * 3. Result Inspection: If authentication fails, Spring Security normally throws an internal exception (e.g., BadCredentialsException).
     * If it passes, the returned object yields true for '.isAuthenticated()', indicating a successful user session verification.
     * 4. Token Assembly: Upon identity confirmation, the JwtService executes cryptographic signing algorithms to generate a
     * compact string-based JWT token containing the subject's principal identity (email).
     */
    @PostMapping(value = "/login")
    public ResponseEntity<String> login(@RequestBody UserEntity userEntity) {

        // Step 1: Wrap raw incoming client credentials into an unauthenticated security wrapper object
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                userEntity.getEmail(),
                userEntity.getPassword()
        );

        try {
            // Step 2: Push the unauthenticated token downstream into the authentication processing loop
            Authentication authenticate = authenticationManager.authenticate(token);

            // Step 3: Check if validation provider matched the incoming request against system storage
            if (authenticate.isAuthenticated()) {
                // Step 4: Generate a cryptographic token package and return it to the client with an HTTP 200 OK status
                String jwtToken = jwtService.generateToken(userEntity.getEmail());
                return new ResponseEntity<>(jwtToken, HttpStatus.OK);
            }

        } catch (Exception e) {
            // Logger hook: Safely catch authorization sub-exceptions (e.g., BadCredentialsException, LockedException)
            // to suppress internal stack traces from leaking via the public REST gateway.
        }

        // Fallback execution block handling bad configurations or unauthenticated credentials
        return new ResponseEntity<>("Invalid Credentials", HttpStatus.BAD_REQUEST);
    }

    /**
     * ==============================================================================
     * ENDPOINT: SYSTEM REGISTRATION & USER INGESTION
     * ==============================================================================
     *
     * @PostMapping - A specialized shortcut variant of {@code @RequestMapping(method = RequestMethod.POST)}.
     * It maps HTTP POST requests hitting "/user/add" directly to this method.
     * POST is semantically used here because this endpoint is responsible for resource creation.
     * * @RequestBody - Actively triggers Spring's internal HttpMessageConverter mechanism (usually backed by Jackson).
     * It reads the raw JSON payload arriving in the incoming HTTP request body, parses it, and maps the matching
     * fields directly into a freshly instantiated Java object of type UserEntity.
     * * @ResponseEntity - A generic wrapper that represents the entire HTTP response.
     * It gives you absolute, programmatic control over configuring the Response Body, HTTP Status Codes, and Headers.
     */
    @PostMapping(value = "/add")
    public ResponseEntity<UserEntity> addUser(@RequestBody UserEntity userEntity) {

        // 1. Delegates business logic and data persistence layers to the service layer.
        // The service layer hashes the password and saves the entity to the MySQL database via Spring Data JPA.
        // It returns the newly persisted Entity containing its autogenerated database 'id' and hashed password.
        UserEntity userEntity1 = userService.saveUser(userEntity);

        // 2. Returns a standard HTTP response package containing:
        // - Body: The serialized JSON representation of our freshly saved userEntity1.
        // - Status Code: HttpStatus.OK (200 OK)
        // Optimization Tip: Since this is a creation endpoint, returning HttpStatus.CREATED (201 Created)
        // is technically more compliant with strict RESTful design guidelines!
        return new ResponseEntity<>(userEntity1, HttpStatus.OK);
    }

}