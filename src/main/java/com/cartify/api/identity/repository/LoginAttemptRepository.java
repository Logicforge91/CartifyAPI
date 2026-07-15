package com.cartify.api.identity.repository;

import com.cartify.api.identity.domain.LoginAttemptEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginAttemptRepository extends JpaRepository<LoginAttemptEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from LoginAttemptEntity a where a.attemptKey = :key")
    Optional<LoginAttemptEntity> findByKeyForUpdate(@Param("key") String key);
}
