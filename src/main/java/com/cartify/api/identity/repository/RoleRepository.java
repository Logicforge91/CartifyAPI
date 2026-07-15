package com.cartify.api.identity.repository;

import com.cartify.api.identity.domain.RoleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    @EntityGraph(attributePaths = "permissions")
    Optional<RoleEntity> findByName(String name);
}
