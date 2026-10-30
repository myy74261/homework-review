package com.reviewdesk.repository;

import com.reviewdesk.domain.ClassroomCollaborator;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 课堂协作者的数据访问接口。
 */
public interface ClassroomCollaboratorRepository
        extends JpaRepository<ClassroomCollaborator, Long> {

    List<ClassroomCollaborator> findByClassroomIdOrderByCreatedAtAsc(Long classroomId);

    List<ClassroomCollaborator> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    Optional<ClassroomCollaborator> findByClassroomIdAndTeacherId(
            Long classroomId, Long teacherId);

    boolean existsByClassroomIdAndTeacherId(Long classroomId, Long teacherId);

    long countByClassroomId(Long classroomId);

    void deleteByClassroomIdAndTeacherId(Long classroomId, Long teacherId);

    void deleteByClassroomId(Long classroomId);

    void deleteByTeacherId(Long teacherId);
}
