package com.reviewdesk.repository;

import com.reviewdesk.domain.ClassroomMembership;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomMembershipRepository
        extends JpaRepository<ClassroomMembership, Long> {

    boolean existsByClassroomIdAndStudentId(Long classroomId, Long studentId);

    Optional<ClassroomMembership> findByClassroomIdAndStudentId(
            Long classroomId, Long studentId);

    List<ClassroomMembership> findByStudentIdOrderByJoinedAtDesc(Long studentId);

    long countByClassroomId(Long classroomId);
}

