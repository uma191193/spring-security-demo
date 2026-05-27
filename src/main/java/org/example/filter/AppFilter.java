package org.example.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.service.JwtService;
import org.example.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ==============================================================================
 * ARCHITECTURAL LAYER: CUSTOM SECURITY INTERCEPTOR (JWT FILTER)
 * ==============================================================================
 *
 * @Component: Registers this class as a managed bean within the Spring Application Context,
 * allowing it to be discovered and injected into the Security Filter Chain.
 * <p>
 * OncePerRequestFilter: A specific Spring Web base class that guarantees this filter
 * will execute exactly once per incoming HTTP request thread, preventing redundant
 * processing caused by internal forward or include dispatch requests.
 */
@Component
public class AppFilter extends OncePerRequestFilter {

    /**
     * ==============================================================================
     * DEPENDENCY: JSON WEB TOKEN UTILITIES
     * ==============================================================================
     *
     * @Autowired - Automatically wires our stateless utility engine used to decode,
     * parse claims, extract user identifiers, and verify signatures from incoming tokens.
     */
    @Autowired
    private JwtService jwtService;

    /**
     * ==============================================================================
     * DEPENDENCY: CUSTOM IDENTITY RETRIEVAL SERVICE
     * ==============================================================================
     * Holds the application-specific domain logic wrapper designed to fetch user metadata
     * records out of the persistence layer.
     * * Declared as 'final' to enforce immutability and promote robust constructor injection.
     */
    private final UserDetailsServiceImpl userDetailsServiceImpl;

    /**
     * ==============================================================================
     * CONSTRUCTOR INJECTION WITH LAZY INITIALIZATION
     * ==============================================================================
     * Explicit constructor injection used to supply dependencies to the filter instance.
     * * * @Lazy: A structural safeguard telling Spring to defer the full initialization of
     * the 'UserDetailsServiceImpl' bean until its first execution cycle. This breaks
     * the circular dependency loop between this filter and the central 'AppConfig' class.
     */
    public AppFilter(@Lazy UserDetailsServiceImpl userDetailsServiceImpl) {
        this.userDetailsServiceImpl = userDetailsServiceImpl;
    }

    /**
     * ==============================================================================
     * INTERNAL FILTER PIPELINE EXECUTION ENGINE
     * ==============================================================================
     * Intercepts every incoming HTTP transaction to analyze incoming request headers,
     * authenticate cryptographically signed payloads, and programmatically initialize
     * Spring Security's runtime authentication storage if a valid session token is found.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extract the HTTP 'Authorization' transmission header from the incoming request envelope
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        // 2. Structural Parsing: Verify that an Authorization header exists and strictly adheres
        // to the industry-standard "Bearer <token>" authentication scheme formatting
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // Truncate the string to strip out the "Bearer " prefix literal
            username = jwtService.extractUsername(token); // Decrypt and extract the user's principal identifier (email)
        }

        // 3. Context Processing Condition: Proceed only if a valid username was extracted AND the current
        // security lifecycle thread has not already been marked as authenticated (avoids redundant checks).
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Fetch the authoritative user profile structure from persistent storage via our database bridge
            UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(username);

            // Cryptographically cross-check token expiration timestamps and payload integrity signatures
            if (jwtService.validateToken(token, userDetails)) {

                // Construct a fully authorized, secure token wrapper holding the user's access rights/roles.
                // Parameter 2 is set to 'null' because credentials (passwords) are unnecessary post-token verification.
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // Build and attach system metadata context details (e.g., remote IP, session ID) onto the authentication token
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // System Authentication: Lock the newly verified token directly into Spring's global context manager.
                // For the remainder of this request lifecycle thread, the client is recognized as fully authenticated.
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        // 4. Pipeline Delegation: Hands execution over to the next sequential component filter in the
        // application's server filter chain (eventually arriving at your RestController endpoint).
        filterChain.doFilter(request, response);
    }
}