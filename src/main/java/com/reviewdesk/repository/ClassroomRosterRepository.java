package com.reviewdesk.repository;

import com.reviewdesk.domain.ClassroomRosterEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 课堂学生名单的数据访问接口。
 */
public interface ClassroomRosterRepository extends JpaRepository<ClassroomRosterEntry, Long> {

    boolean existsByClassroomId(Long classroomId);

    Optional<ClassroomRosterEntry> findByClassroomIdAndStudentNumberIgnoreCase(
            Long classroomId, String studentNumber);

    List<ClassroomRosterEntry> findByStudentNumberIgnoreCase(String studentNumber);

    List<ClassroomRosterEntry> findByClassroomIdOrderByStudentNumberAsc(Long classroomId);

    long countByClassroomId(Long classroomId);

    long countByClassroomIdAndStudentAccountIdIsNotNull(Long classroomId);
}
