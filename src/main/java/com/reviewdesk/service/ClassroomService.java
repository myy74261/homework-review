package com.reviewdesk.service;

import com.reviewdesk.domain.Account;
import com.reviewdesk.domain.Classroom;
import com.reviewdesk.domain.ClassroomMembership;
import com.reviewdesk.repository.ClassroomMembershipRepository;
import com.reviewdesk.repository.ClassroomRepository;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 教师创建课堂、学生使用课堂码加入课堂。
 */
@Service
@Transactional(readOnly = true)
public class ClassroomService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClassroomRepository classroomRepository;
    private final ClassroomMembershipRepository membershipRepository;

    public ClassroomService(ClassroomRepository classroomRepository,
                            ClassroomMembershipRepository membershipRepository) {
        this.classroomRepository = classroomRepository;
        this.membershipRepository = membershipRepository;
    }

    public Classroom require(long classroomId) {
        return classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("课堂不存在"));
    }

    public List<Classroom> teacherClassrooms(Account teacher) {
        return classroomRepository.findByTeacherIdOrderByCreatedAtDesc(teacher.getId());
    }

    public List<Classroom> studentClassrooms(Account student) {
        List<Long> classroomIds = membershipRepository
                .findByStudentIdOrderByJoinedAtDesc(student.getId())
                .stream()
                .map(ClassroomMembership::getClassroomId)
                .toList();
        return classroomIds.isEmpty()
                ? List.of()
                : classroomRepository.findByIdInOrderByCreatedAtDesc(classroomIds);
    }

    @Transactional
    public Classroom create(Account teacher, String name, String courseName) {
        if (!teacher.isTeacher()) {
            throw new IllegalArgumentException("只有教师可以创建课堂");
        }
        if (name == null || name.isBlank() || courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("请填写课堂名称和课程名称");
        }
        return classroomRepository.save(new Classroom(
                name.strip(),
                courseName.strip(),
                teacher.getId(),
                nextJoinCode()));
    }

    @Transactional
    public Classroom join(Account student, String joinCode) {
        if (student.isTeacher()) {
            throw new IllegalArgumentException("教师账号不能加入学生课堂");
        }
        Classroom classroom = classroomRepository
                .findByJoinCodeIgnoreCase(joinCode == null ? "" : joinCode.strip())
                .orElseThrow(() -> new IllegalArgumentException("课堂码无效"));
        if (!membershipRepository.existsByClassroomIdAndStudentId(
                classroom.getId(), student.getId())) {
            membershipRepository.save(new ClassroomMembership(
                    classroom.getId(), student.getId()));
        }
        return classroom;
    }

    public boolean isTeacherOwner(Account teacher, long classroomId) {
        return teacher.isTeacher()
                && classroomRepository.findById(classroomId)
                .map(classroom -> classroom.getTeacherId().equals(teacher.getId()))
                .orElse(false);
    }

    public boolean isStudentMember(Account student, long classroomId) {
        return !student.isTeacher()
                && membershipRepository.existsByClassroomIdAndStudentId(
                        classroomId, student.getId());
    }

    public long memberCount(long classroomId) {
        return membershipRepository.countByClassroomId(classroomId);
    }

    public String nextJoinCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder code = new StringBuilder(8);
            for (int index = 0; index < 8; index++) {
                code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            if (!classroomRepository.existsByJoinCode(code.toString())) {
                return code.toString();
            }
        }
        throw new IllegalStateException("暂时无法生成课堂码，请稍后重试");
    }
}

