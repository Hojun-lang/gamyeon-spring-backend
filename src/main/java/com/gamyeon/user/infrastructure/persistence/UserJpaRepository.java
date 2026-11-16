package com.gamyeon.user.infrastructure.persistence;

import com.gamyeon.user.domain.OAuthProvider;
import com.gamyeon.user.domain.User;
import com.gamyeon.user.domain.UserStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserJpaRepository extends JpaRepository<User, Long> {

  Optional<User> findByProviderAndProviderId(OAuthProvider provider, String providerId);

  Optional<User> findByEmail(String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.id = :id")
  Optional<User> findByIdForUpdate(@Param("id") Long id);

  @Query(
      """
      select u.id from User u
      where u.status = :status and u.withdrawnAt < :withdrawnBefore
      order by u.withdrawnAt asc, u.id asc
      """)
  List<Long> findHardDeleteTargetIds(
      @Param("status") UserStatus status,
      @Param("withdrawnBefore") LocalDateTime withdrawnBefore,
      Pageable pageable);
}
