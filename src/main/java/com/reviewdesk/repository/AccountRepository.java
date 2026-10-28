package com.reviewdesk.repository;

import com.reviewdesk.domain.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 教师与学生账号的数据访问接口。
 */
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUsername(String username);

    Optional<Account> findFirstByStudentNumberIgnoreCase(String studentNumber);

    boolean existsByUsername(String username);
}

