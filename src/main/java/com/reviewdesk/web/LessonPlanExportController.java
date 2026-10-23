package com.reviewdesk.web;

import com.reviewdesk.service.TeachingSuggestionService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 将教师编辑后的备课稿导出为 Word 兼容文档或 Markdown。
 */
@Controller
public class LessonPlanExportController {

    private final TeachingSuggestionService suggestionService;

    public LessonPlanExportController(TeachingSuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping("/teacher/assignments/{assignmentId}/lesson-plan/export.doc")
    public ResponseEntity<byte[]> exportWord(@PathVariable long assignmentId) {
        TeachingSuggestionService.LessonPlanWorkspace workspace =
                suggestionService.getOrCreateWorkspace(assignmentId);
        String html = buildWordHtml(workspace);
        return download(html, workspace.plan().getTitle() + ".doc",
                new MediaType("application", "msword", StandardCharsets.UTF_8));
    }

    @GetMapping("/teacher/assignments/{assignmentId}/lesson-plan/export.md")
    public ResponseEntity<byte[]> exportMarkdown(@PathVariable long assignmentId) {
        TeachingSuggestionService.LessonPlanWorkspace workspace =
                suggestionService.getOrCreateWorkspace(assignmentId);
        return download(buildMarkdown(workspace), workspace.plan().getTitle() + ".md",
                MediaType.TEXT_MARKDOWN);
    }

    private String buildWordHtml(
            TeachingSuggestionService.LessonPlanWorkspace workspace) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
                .append("<style>")
                .append("body{font-family:'Microsoft YaHei',sans-serif;line-height:1.75;")
                .append("color:#20332d;max-width:820px;margin:36px auto;padding:0 28px}")
                .append("h1{font-family:'SimSun',serif;font-size:26px}")
                .append("h2{font-size:17px;margin-top:28px;border-left:3px solid #235f4e;")
                .append("padding-left:10px}.meta{color:#777e77;font-size:12px}")
                .append(".content{white-space:pre-wrap;font-size:14px}")
                .append(".evidence{border-left:2px solid #a95535;padding-left:12px;")
                .append("margin:12px 0;color:#4c5a53}")
                .append("</style></head><body>");
        html.append("<h1>").append(escape(workspace.plan().getTitle())).append("</h1>");
        html.append("<p class=\"meta\">课程：")
                .append(escape(workspace.assignment().getCourseName()))
                .append("｜依据：").append(workspace.evidence().size())
                .append(" 条教师确认问题</p>");
        html.append("<h2>课堂讲解建议</h2><div class=\"content\">")
                .append(escape(workspace.plan().getContent()))
                .append("</div>");
        html.append("<h2>附：问题证据</h2>");
        appendEvidence(html, workspace.evidence());
        html.append("<p class=\"meta\">由明鉴 ReviewDesk 生成，内容经教师审核后可继续修改。</p>");
        html.append("</body></html>");
        return html.toString();
    }

    private String buildMarkdown(
            TeachingSuggestionService.LessonPlanWorkspace workspace) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(workspace.plan().getTitle()).append("\n\n")
                .append("- 课程：").append(workspace.assignment().getCourseName()).append("\n")
                .append("- 问题依据：").append(workspace.evidence().size()).append(" 条\n\n")
                .append(workspace.plan().getContent()).append("\n\n")
                .append("## 附：问题证据\n\n");
        for (TeachingSuggestionService.EvidenceView item : workspace.evidence()) {
            markdown.append("- **").append(item.submission().getStudentName())
                    .append(" / ").append(item.issue().getRubricName()).append("**\n")
                    .append("  - 原文：").append(singleLine(item.issue().getEvidenceQuote())).append("\n")
                    .append("  - 教师判断：").append(item.issue().getTeacherNote()).append("\n");
        }
        return markdown.toString();
    }

    private void appendEvidence(StringBuilder html,
                                List<TeachingSuggestionService.EvidenceView> evidence) {
        if (evidence.isEmpty()) {
            html.append("<p>暂无教师确认的问题。</p>");
            return;
        }
        for (TeachingSuggestionService.EvidenceView item : evidence) {
            html.append("<div class=\"evidence\"><strong>")
                    .append(escape(item.submission().getStudentName()))
                    .append(" / ").append(escape(item.issue().getRubricName()))
                    .append("</strong><p>原文：")
                    .append(escape(singleLine(item.issue().getEvidenceQuote())))
                    .append("</p><p>教师判断：")
                    .append(escape(item.issue().getTeacherNote()))
                    .append("</p></div>");
        }
    }

    private ResponseEntity<byte[]> download(String content, String filename,
                                            MediaType mediaType) {
        String encodedName = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build());
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encodedName);
        return ResponseEntity.ok()
                .headers(headers)
                .body(content.getBytes(StandardCharsets.UTF_8));
    }

    private String escape(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String singleLine(String text) {
        return text.replaceAll("\\s+", " ").strip();
    }
}
