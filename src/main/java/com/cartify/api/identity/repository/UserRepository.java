package com.cartify.api.identity.repository;

import com.cartify.api.identity.domain.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    @Query("""
            select distinct u from UserEntity u
            left join fetch u.roles r
            left join fetch r.permissions
            where lower(u.email) = lower(:email)
            """)
    Optional<UserEntity> findForAuthenticationByEmail(@Param("email") String email);

    @Query("""
            select distinct u from UserEntity u
            left join fetch u.roles r
            left join fetch r.permissions
            where u.id = :id
            """)
    Optional<UserEntity> findForAuthenticationById(@Param("id") Long id);
}
