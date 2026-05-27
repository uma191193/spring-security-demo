package org.example.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * ==============================================================================
 * ARCHITECTURAL LAYER: UTILITY INFRASTRUCTURE (JWT TOKEN ENGINE)
 * ==============================================================================
 *
 * @Component: Registers this class as a managed utility bean within the Spring Application Context.
 * This allows other downstream components (like AppFilter and UserController) to seamlessly
 * inject and utilize its cryptographic token assembly and parsing methods.
 * <p>
 * Underlying Framework: This service utilizes the industry-standard 'jjwt' (Java JWT) library
 * to compose, sign, serialize, parse, and validate JSON Web Tokens.
 */
@Component
public class JwtService {

    /**
     * ==============================================================================
     * CRYPTOGRAPHIC SIGNING PASSPHRASE (THE SYSTEM SECRET)
     * ==============================================================================
     * A 256-bit (32-byte) hex-encoded random string used as the symmetric master secret key.
     * * Security Notice: In a production environment, hardcoding a secret string literal is
     * a massive vulnerability. It should always be externalized into a secure configuration provider
     * or environment variable (e.g., via ${JWT_SECRET_KEY}) and loaded inside runtime memory.
     */
    public static final String SECRET = "357638792F423F4428472B4B6250655368566D597133743677397A2443264629";

    /**
     * ==============================================================================
     * UTILITY: EXTRACT USER IDENTIFIER
     * ==============================================================================
     * Isolates the "Subject" claim from the token payload metadata. In this architectural
     * design, the user's registered Email address is stamped as the primary subject identifier.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * ==============================================================================
     * UTILITY: EXTRACT EXPIRATION TIMESTAMP
     * ==============================================================================
     * Isolates the "Expiration" claim timestamp from the token metadata envelope to
     * evaluate exactly when this token ceases to be recognized as valid by the server gateway.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * ==============================================================================
     * UTILITY: FLEXIBLE CLAIM RESOLUTION MATRIX
     * ==============================================================================
     * A functional generic template method designed to extract an individual claim out
     * of the token payload without rewriting duplicate claim-parsing boilerplate.
     * * Parameter 'claimsResolver': Accepts a functional mapping callback interface (e.g., Claims::getSubject).
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * ==============================================================================
     * DECRYPTION ENGINE: REVERSE TOKEN DESERIALIZATION
     * ==============================================================================
     * Parses the string-based token blob using our system's verified verification key.
     * * Underlying Mechanism: The parser builder cross-checks the cryptographic signature hash.
     * If the token was altered by an external attacker, the parsing signature verification fails
     * and a SignatureException is immediately thrown before the system reads any internal data.
     */
    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()                          // 1. Replaces deprecated parserBuilder()
                .verifyWith((SecretKey) getSignKey()) // 2. Replaces deprecated setSigningKey()
                .build()
                .parseSignedClaims(token)          // 3. Replaces deprecated parseClaimsJws()
                .getPayload();                     // 4. Replaces deprecated getBody()
    }

    /**
     * ==============================================================================
     * CRITERIA: LIFECYCLE CHRONOLOGY LOOKUP
     * ==============================================================================
     * Evaluates if the token's structural expiration date resides chronologically behind
     * the computer's current real-time instance execution timestamp.
     * * Return: True if the expiration point has elapsed, meaning the token is dead.
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * ==============================================================================
     * VALIDATION: DOUBLE-GATE SESSION ANALYSIS
     * ==============================================================================
     * Assesses whether an arriving client token passes runtime usage rules.
     * For a token to evaluate as genuinely authorized, it must satisfy two conditions:
     * 1. The identity embedded in the token matching the database username.
     * 2. The token lifecycle tracking metrics indicating it has not expired.
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * ==============================================================================
     * ASSEMBLY: TOKEN INTENTIONAL COMPOSITION
     * ==============================================================================
     * Public entrypoint designed to package user registration parameters and trigger
     * generation of a fresh stateless authentication token string wrapper.
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>(); // Standard dictionary placeholder for adding custom attributes (e.g., roles/id)
        return createToken(claims, username);
    }

    /**
     * ==============================================================================
     * GENERATOR: TOKENS BUILDER PIPELINE
     * ==============================================================================
     * Assembles the multi-part structural layout of the JSON Web Token.
     * * Claims: Places the metadata dictionary map into the token body layout.
     * * Subject: Assigns the primary core user owner handle identifier string.
     * * IssuedAt: Injects a millisecond-precision creation genesis timestamp block.
     * * Expiration: Establishes a death criteria time constraint.
     * (Calculated here as Current Time + 60,000 milliseconds = Strictly 1 Minute Lifespan).
     * * SignWith: Signs the compiled configuration using the HS256 algorithm and our symmetric key.
     * * Compact: Serializes the final output into a compact, URL-safe three-part string block separated by dots.
     */
    private String createToken(Map<String, Object> claims, String username) {
        return Jwts.builder()
                .claims(claims)                                      // 1. Replaces deprecated setClaims()
                .subject(username)                                   // 2. Replaces deprecated setSubject()
                .issuedAt(new Date(System.currentTimeMillis()))      // 3. Replaces deprecated setIssuedAt()
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60)) // 4. Replaces deprecated setExpiration()
                .signWith(getSignKey())                              // 5. Replaces deprecated signWith(key, algorithm)
                .compact();
    }

    /**
     * ==============================================================================
     * CRYPTO KEY ADAPTER: BASE64 TO HMACSCRIPT MATRIX
     * ==============================================================================
     * Converts our raw hex/base64 string literal representation into a safe, robust
     * 'java.security.Key' instance required by the cryptographic signing engine.
     * * Keys.hmacShaKeyFor: Guarantees that the resulting bytes match the secure, structural
     * requirements needed for standard HMAC-SHA256 operations.
     */
    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}