package com.letterfuture.execution.engine.workflow.repository;


import com.letterfuture.execution.engine.workflow.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, UUID> {

    Optional<Users> findByEmail(String email);

    Optional<Users> findByUsername(String username);

    @Query("SELECT u FROM Users u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
    Optional<Users> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
