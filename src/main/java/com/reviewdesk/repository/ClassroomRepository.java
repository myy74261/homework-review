package com.reviewdesk.repository;

import com.reviewdesk.domain.Classroom;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 教学课堂的数据访问接口。
 */
public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    List<Classroom> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    List<Classroom> findByTeacherIdAndDissolvedAtIsNullOrderByCreatedAtDesc(Long teacherId);

    List<Classroom> findByIdInOrderByCreatedAtDesc(Collection<Long> ids);

    List<Classroom> findByIdInAndDissolvedAtIsNullOrderByCreatedAtDesc(Collection<Long> ids);

    Optional<Classroom> findByJoinCodeIgnoreCase(String joinCode);

    Optional<Classroom> findByJoinCodeIgnoreCaseAndDissolvedAtIsNull(String joinCode);

    boolean existsByJoinCode(String joinCode);
}

