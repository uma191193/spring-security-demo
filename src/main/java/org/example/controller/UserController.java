package org.example.controller;

import org.example.UserEntity;
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
 * @RestController - Core Spring MVC stereotype annotation that combines {@code @Controller} and {@code @ResponseBody}.
 * It registers this class as a request-handling component in the Spring Application Context.
 * Crucially, it tells Spring that the return value of every method should be serialized directly
 * into the HTTP response body (typically as JSON or XML) instead of looking for an HTML view template.
 */
@RestController
/**
 * @RequestMapping - Defines the base URL namespace for this entire controller.
 * All individual endpoint paths inside this class will be prefixed with "/user".
 * For example: http://localhost:9000/user
 */
@RequestMapping(value = "/user")
public class UserController {

    /**
     * @Autowired - Performs Dependency Injection (DI).
     * Spring searches its Bean Factory container for an instance of UserServiceImpl,
     * manages its lifecycle, and automatically wires it into this controller layer at startup.
     * Note: Injecting the concrete class (UserServiceImpl) works fine, but it is generally a best
     * practice to inject the Interface (UserService) to maintain low coupling and support mocking in unit tests.
     */
    @Autowired
    public UserServiceImpl userService;

    /**
     * @Autowired - Injects the centralized Spring Security execution coordinator.
     * This bean is defined in your AppConfig class. It serves as the primary gateway
     * to manually pass user-submitted credentials directly into Spring Security's validation engine.
     */
    @Autowired
    public AuthenticationManager authenticationManager;

    /**
     * @PostMapping - Specialized mapping that routes HTTP POST requests hitting "/user/login" to this method.
     * * PROGRAMMATIC AUTHENTICATION FLOW:
     * 1. Token Packaging: Takes the raw username/password from the request body and encapsulates them
     * inside an unauthenticated UsernamePasswordAuthenticationToken instance.
     * 2. Processing Delegation: The unauthenticated token is passed directly to the authenticationManager.authenticate() method.
     * The manager routes it to your registered DaoAuthenticationProvider, which loads the true user record from storage via
     * your UserDetailsService, computes and validates the hashed password, and returns a fully authorized Authentication token.
     * 3. Result Inspection: If authentication fails, Spring Security normally throws an internal exception (e.g., BadCredentialsException).
     * If it passes, the returned object yields true for '.isAuthenticated()', indicating a successful user session verification.
     */
    @PostMapping(value = "/login")
    public ResponseEntity<String> login(@RequestBody UserEntity userEntity) {

        // Step 1: Wrap raw incoming client credentials into an unauthenticated security wrapper object
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                userEntity.getEmail(),
                userEntity.getPassword()
        );

        // Step 2: Push the wrapper token through the security ecosystem for deep password/identity validation
        Authentication authenticate = authenticationManager.authenticate(token);

        // Step 3: Evaluate the validation response and hand back an appropriate status statement
        if (authenticate.isAuthenticated()) {
            return new ResponseEntity<>("Authentication is successful ", HttpStatus.OK);
        } else {
            // Note: If bad credentials are sent, Spring usually throws an exception before reaching here,
            // but this serves as a fallback structural check. HttpStatus.UNAUTHORIZED (401) is traditionally
            // used rather than NOT_FOUND (404) for structural authentication failures.
            return new ResponseEntity<>("Authentication failed ", HttpStatus.NOT_FOUND);
        }
    }

    /**
     * @PostMapping - A specialized shortcut variant of {@code @RequestMapping(method = RequestMethod.POST)}.
     * It maps HTTP POST requests hitting "/user/add" directly to this method.
     * POST is semantically used here because this endpoint is responsible for resource creation.
     * @RequestBody - Actively triggers Spring's internal HttpMessageConverter mechanism (usually backed by Jackson).
     * It reads the raw JSON payload arriving in the incoming HTTP request body, parses it, and maps the matching
     * fields directly into a freshly instantiated Java object of type UserEntity.
     * @ResponseEntity - A generic wrapper that represents the entire HTTP response.
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