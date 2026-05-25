package org.example;

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
    // No code needs to be written here!
    // Methods like save(), findById(), findAll(), and deleteById() are automatically inherited.
}