package com.reviewdesk.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reviewdesk.domain.Account;
import com.reviewdesk.domain.Appeal;
import com.reviewdesk.domain.Assignment;
import com.reviewdesk.domain.Classroom;
import com.reviewdesk.domain.Submission;
import com.reviewdesk.repository.AccountRepository;
import com.reviewdesk.repository.AppealRepository;
import com.reviewdesk.repository.IntegrityFlagRepository;
import com.reviewdesk.repository.SubmissionRepository;
import com.reviewdesk.service.AccountService;
import com.reviewdesk.service.AssignmentService;
import com.reviewdesk.service.ClassroomService;
import com.reviewdesk.service.StudentSubmissionService;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * 验证文科、理科和工科课堂的完整作业流程。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "review-desk.demo.cross-discipline-enabled=true")
class CrossDisciplineFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private StudentSubmissionService submissionService;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private AppealRepository appealRepository;

    @Autowired
    private IntegrityFlagRepository integrityFlagRepository;

    static Stream<org.junit.jupiter.params.provider.Arguments> disciplines() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(
                        "chen", "传播学方法课", "文科", "TEST-HUM-01"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "zhou", "普通物理实验", "理科", "TEST-SCI-01"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "wujian", "软件工程实践", "工科", "TEST-ENG-01"));
    }

    @Test
    void simulationDataContainsAllThreeDisciplines() {
        assertThat(accountRepository.findByUsername("chen")).isPresent();
        assertThat(accountRepository.findByUsername("zhou")).isPresent();
        assertThat(accountRepository.findByUsername("wujian")).isPresent();
        assertThat(submissionRepository.count()).isGreaterThanOrEqualTo(20);
        assertThat(appealRepository.count()).isGreaterThanOrEqualTo(3);
        assertThat(integrityFlagRepository.count()).isGreaterThanOrEqualTo(2);
    }

    @ParameterizedTest(name = "{2}课程完整流程")
    @MethodSource("disciplines")
    void assignmentCanBePublishedSubmittedReviewedAndAppealed(
            String teacherUsername,
            String courseName,
            String discipline,
            String studentNumber) throws Exception {
        Account teacher = accountRepository.findByUsername(teacherUsername).orElseThrow();
        Classroom classroom = classroomService.teacherClassrooms(teacher).stream()
                .filter(item -> item.getCourseName().equals(courseName))
                .findFirst()
                .orElseThrow();
        String studentUsername = "flow_" + discipline.toLowerCase()
                .replace("文科", "humanities")
                .replace("理科", "science")
                .replace("工科", "engineering");
        Account student = accountService.register(
                studentUsername,
                "StudentFlow2026!",
                discipline + "测试学生",
                Account.Role.STUDENT,
                studentNumber);
        classroomService.importRoster(
                teacher,
                classroom.getId(),
                List.of(new ClassroomService.RosterEntry(
                        studentNumber, student.getDisplayName())));
        classroomService.join(student, classroom.getJoinCode());

        String title = discipline + "课程完整流程测试作业";
        String format = "工科".equals(discipline) ? "CODE" : "MIXED";
        mockMvc.perform(post("/api/app/teacher/assignments")
                        .with(user(teacherUsername).roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "courseName": "%s",
                                  "classroomId": %d,
                                  "instructions": "提交完整分析过程、证据和结论，并说明方法与局限。",
                                  "dueAt": "2099-06-30T23:59",
                                  "submissionFormat": "%s",
                                  "aiPolicy": "ASSISTED_ALLOWED",
                                  "aiDeclarationRequired": true,
                                  "sourceDeclarationRequired": true,
                                  "appealAllowed": true,
                                  "rubrics": [
                                    {"name":"问题理解","points":25,"description":"是否准确理解任务"},
                                    {"name":"证据与过程","points":35,"description":"过程与证据是否完整"},
                                    {"name":"结论表达","points":25,"description":"结论是否有依据"},
                                    {"name":"规范表达","points":15,"description":"格式和表达是否规范"}
                                  ]
                                }
                                """.formatted(title, courseName, classroom.getId(), format)))
                .andExpect(status().isOk());

        Assignment assignment = assignmentService.listForClassrooms(
                        List.of(classroom.getId())).stream()
                .filter(item -> item.getTitle().equals(title))
                .findFirst()
                .orElseThrow();
        String content = discipline
                + "课程测试提交：本文说明研究或实验目标、数据与材料来源、分析过程、结果以及适用边界。";
        mockMvc.perform(multipart("/student/assignments/{id}/submissions/ajax", assignment.getId())
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "files", "evidence.txt", "text/plain",
                                "课程证据材料".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .param("content", content)
                        .param("aiUsage", "NONE")
                        .param("sourceDeclaration", "课程材料与公开数据")
                        .param("collaborationDeclaration", "无")
                        .param("integrityConfirmed", "true")
                        .with(user(studentUsername).roles("STUDENT"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Submission submission = submissionService.findLatest(
                        assignment.getId(), student)
                .orElseThrow();
        mockMvc.perform(get("/api/app/teacher/reviews/{id}", submission.getId())
                        .with(user(teacherUsername).roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submission.content").value(content));

        mockMvc.perform(put("/api/submissions/{id}/review", submission.getId())
                        .with(user(teacherUsername).roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "教师审核意见",
                                  "content": "已完成证据核对。请继续保持过程透明，并在结论中说明适用边界。",
                                  "baseRevision": 0
                                }
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/submissions/{id}/review/complete", submission.getId())
                        .with(user(teacherUsername).roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/app/student/submissions/{id}/appeals", submission.getId())
                        .with(user(studentUsername).roles("STUDENT"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "希望教师进一步核对过程分与证据评分依据。",
                                  "requestedResolution": "请复核评分说明"
                                }
                                """))
                .andExpect(status().isOk());
        Appeal appeal = appealRepository
                .findBySubmissionIdOrderByCreatedAtDesc(submission.getId())
                .get(0);
        mockMvc.perform(post("/api/app/teacher/appeals/{id}/respond", appeal.getId())
                        .with(user(teacherUsername).roles("TEACHER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ACCEPTED",
                                  "teacherResponse": "已重新核对过程证据，并补充评分依据。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }
}
