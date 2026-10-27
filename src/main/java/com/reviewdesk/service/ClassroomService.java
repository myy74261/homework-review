package com.reviewdesk.service;

import com.reviewdesk.domain.Account;
import com.reviewdesk.domain.Classroom;
import com.reviewdesk.domain.ClassroomMembership;
import com.reviewdesk.repository.ClassroomMembershipRepository;
import com.reviewdesk.repository.ClassroomRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    public List<Classroom> teacherClassrooms(Account teacher) {
        return classroomRepository
                .findByTeacherIdAndDissolvedAtIsNullOrderByCreatedAtDesc(teacher.getId());
    }

    public List<Classroom> studentClassrooms(Account student) {
        List<Long> classroomIds = membershipRepository
                .findByStudentIdOrderByJoinedAtDesc(student.getId())
                .stream()
                .map(ClassroomMembership::getClassroomId)
                .toList();
        return classroomIds.isEmpty()
                ? List.of()
                : classroomRepository
                        .findByIdInAndDissolvedAtIsNullOrderByCreatedAtDesc(classroomIds);
    }

    @Transactional
    public Classroom create(Account teacher, String name, String courseName) {
        return create(
                teacher,
                name,
                courseName,
                Instant.now(),
                Instant.now().plus(120, ChronoUnit.DAYS));
    }

    @Transactional
    public Classroom create(Account teacher, String name, String courseName,
                            Instant startAt, Instant endAt) {
        if (!teacher.isTeacher()) {
            throw new IllegalArgumentException("只有教师可以创建课堂");
        }
        if (name == null || name.isBlank() || courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("请填写课堂名称和课程名称");
        }
        validateSchedule(startAt, endAt);
        return classroomRepository.save(new Classroom(
                name.strip(),
                courseName.strip(),
                teacher.getId(),
                nextJoinCode(),
                startAt,
                endAt));
    }

    @Transactional
    public Classroom join(Account student, String joinCode) {
        if (student.isTeacher()) {
            throw new IllegalArgumentException("教师账号不能加入学生课堂");
        }
        Classroom classroom = classroomRepository
                .findByJoinCodeIgnoreCaseAndDissolvedAtIsNull(
                        joinCode == null ? "" : joinCode.strip())
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
                .filter(classroom -> !classroom.isDissolved())
                .map(classroom -> classroom.getTeacherId().equals(teacher.getId()))
                .orElse(false);
    }

    public boolean isStudentMember(Account student, long classroomId) {
        return !student.isTeacher()
                && classroomRepository.findById(classroomId)
                .filter(classroom -> !classroom.isDissolved())
                .isPresent()
                && membershipRepository.existsByClassroomIdAndStudentId(
                        classroomId, student.getId());
    }

    @Transactional
    public Classroom updateSchedule(Account teacher, long classroomId,
                                    Instant startAt, Instant endAt) {
        Classroom classroom = requireTeacherClassroom(teacher, classroomId);
        validateSchedule(startAt, endAt);
        classroom.updateSchedule(startAt, endAt);
        return classroomRepository.save(classroom);
    }

    @Transactional
    public Classroom dissolve(Account teacher, long classroomId) {
        Classroom classroom = requireTeacherClassroom(teacher, classroomId);
        if (classroom.getEndAt() == null) {
            throw new IllegalStateException("请先设置课程结束时间");
        }
        if (Instant.now().isBefore(classroom.getEndAt())) {
            throw new IllegalStateException("课程尚未结束，不能解散课堂");
        }
        classroom.dissolve();
        return classroomRepository.save(classroom);
    }

    public String lifecycleStatus(Classroom classroom) {
        if (classroom.isDissolved()) {
            return "DISSOLVED";
        }
        if (classroom.getStartAt() == null || classroom.getEndAt() == null) {
            return "UNSCHEDULED";
        }
        Instant now = Instant.now();
        if (now.isBefore(classroom.getStartAt())) {
            return "UPCOMING";
        }
        return now.isBefore(classroom.getEndAt()) ? "ACTIVE" : "ENDED";
    }

    public long memberCount(long classroomId) {
        return membershipRepository.countByClassroomId(classroomId);
    }

    private Classroom requireTeacherClassroom(Account teacher, long classroomId) {
        if (!teacher.isTeacher()) {
            throw new SecurityException("需要教师权限");
        }
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("课堂不存在"));
        if (!classroom.getTeacherId().equals(teacher.getId()) || classroom.isDissolved()) {
            throw new SecurityException("无权管理该课堂");
        }
        return classroom;
    }

    private void validateSchedule(Instant startAt, Instant endAt) {
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("请设置课程起始和结束时间");
        }
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("课程结束时间必须晚于起始时间");
        }
    }

    private String nextJoinCode() {
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

