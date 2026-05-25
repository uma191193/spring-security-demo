package org.example.repository;

import org.example.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * ==============================================================================
 * ARCHITECTURAL LAYER: DATA ACCESS LAYER (REPOSITORY)
 * ==============================================================================
 *
 * @Repository: A specialized stereotype annotation that indicates this interface
 * encapsulates storage, retrieval, and search behavior for a database table.
 * It also tells Spring to enable automatic translation of low-level database exceptions
 * (like SQLException) into Spring’s portable DataAccessException hierarchy.
 * * * EXTENDING JPAREPOSITORY<UserEntity, Integer>:
 * By extending JpaRepository, you inherit a massive collection of out-of-the-box
 * CRUD, pagination, and sorting methods.
 * - Parameter 1 (UserEntity): Specifies the target Java object/domain model this repository manages.
 * - Parameter 2 (Integer): Specifies the exact Data Type of that Entity's @Id field (Primary Key).
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {

    // No boilerplate code needs to be written here!
    // Basic methods like save(), findById(), findAll(), and deleteById() are automatically inherited.

    /**
     * ==============================================================================
     * DYNAMIC DERIVED QUERY METHOD (SPRING DATA JPA MAGIC)
     * ==============================================================================
     * This defines a custom finder method that Spring Data JPA automatically parses and
     * implements at application startup.
     * * * How It Works (The Naming Strategy):
     * Spring Data scans the method name prefix "findBy" and interprets the trailing word "Email"
     * as a property reference on your target 'UserEntity' class. It then dynamically reads the
     * entity's fields, matches it to the 'email' database column, and auto-generates a proxy
     * implementation executing a standard SQL statement behind the scenes:
     * * SQL Equivalent: SELECT * FROM user_entity WHERE email = ?;
     * * * Return Value: If a matching record is found, it populates and returns a hydrated
     * UserEntity object. If no record matches the given email parameter, it returns null.
     */
    public UserEntity findByEmail(String email);
}