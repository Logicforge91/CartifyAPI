package com.cartify.api.identity.repository;

import com.cartify.api.identity.domain.AccountTokenEntity;
import com.cartify.api.identity.domain.AccountTokenType;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountTokenRepository extends JpaRepository<AccountTokenEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t from AccountTokenEntity t join fetch t.user
            where t.tokenHash = :hash and t.tokenType = :type
            """)
    Optional<AccountTokenEntity> findByHashAndTypeForUpdate(
            @Param("hash") String hash,
            @Param("type") AccountTokenType type);

    @Modifying
    @Query("delete from AccountTokenEntity t where t.user.id = :userId and t.tokenType = :type")
    void deleteByUserIdAndType(@Param("userId") Long userId, @Param("type") AccountTokenType type);
}
