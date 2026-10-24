package com.reviewdesk.service;

import com.reviewdesk.domain.SubmissionAttachment;
import com.reviewdesk.repository.SubmissionAttachmentRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 保存和读取文档、代码与截图附件。
 */
@Service
@Transactional(readOnly = true)
public class SubmissionAttachmentService {

    private static final int MAX_FILES = 8;
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final Map<String, String> ALLOWED_EXTENSIONS = Map.ofEntries(
            Map.entry("pdf", "DOCUMENT"),
            Map.entry("doc", "DOCUMENT"),
            Map.entry("docx", "DOCUMENT"),
            Map.entry("txt", "DOCUMENT"),
            Map.entry("md", "DOCUMENT"),
            Map.entry("java", "CODE"),
            Map.entry("py", "CODE"),
            Map.entry("js", "CODE"),
            Map.entry("ts", "CODE"),
            Map.entry("html", "CODE"),
            Map.entry("css", "CODE"),
            Map.entry("zip", "ARCHIVE"),
            Map.entry("png", "SCREENSHOT"),
            Map.entry("jpg", "SCREENSHOT"),
            Map.entry("jpeg", "SCREENSHOT"),
            Map.entry("webp", "SCREENSHOT"));

    private final SubmissionAttachmentRepository attachmentRepository;
    private final Path storageRoot;

    public SubmissionAttachmentService(
            SubmissionAttachmentRepository attachmentRepository,
            @Value("${review-desk.storage.root:./data/uploads}") String storageRoot) {
        this.attachmentRepository = attachmentRepository;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    public List<SubmissionAttachment> list(long submissionId) {
        return attachmentRepository.findBySubmissionIdOrderByIdAsc(submissionId);
    }

    @Transactional
    public List<SubmissionAttachment> store(
            long submissionId, MultipartFile[] files) throws IOException {
        List<MultipartFile> providedFiles = files == null
                ? List.of()
                : List.of(files).stream().filter(file -> !file.isEmpty()).toList();
        if (providedFiles.isEmpty()) {
            return List.of();
        }
        if (providedFiles.size() > MAX_FILES) {
            throw new IllegalArgumentException("一次最多上传 " + MAX_FILES + " 个附件");
        }
        List<String> extensions = new ArrayList<>();
        for (MultipartFile file : providedFiles) {
            String extension = extensionOf(file.getOriginalFilename());
            if (!ALLOWED_EXTENSIONS.containsKey(extension)) {
                throw new IllegalArgumentException("不支持的附件类型：" + extension);
            }
            if (file.getSize() > MAX_FILE_BYTES) {
                throw new IllegalArgumentException("单个附件不能超过 10 MB");
            }
            extensions.add(extension);
        }

        List<SubmissionAttachment> saved = new ArrayList<>();
        Path submissionDirectory = storageRoot.resolve(String.valueOf(submissionId))
                .normalize();
        if (!submissionDirectory.startsWith(storageRoot)) {
            throw new IllegalArgumentException("附件路径无效");
        }
        Files.createDirectories(submissionDirectory);

        for (int index = 0; index < providedFiles.size(); index++) {
            MultipartFile file = providedFiles.get(index);
            String extension = extensions.get(index);
            String storedName = UUID.randomUUID() + "." + extension;
            Path target = submissionDirectory.resolve(storedName).normalize();
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String originalName = sanitizeOriginalName(file.getOriginalFilename());
            SubmissionAttachment attachment = new SubmissionAttachment(
                    submissionId,
                    originalName,
                    submissionId + "/" + storedName,
                    file.getContentType() == null
                            ? "application/octet-stream" : file.getContentType(),
                    file.getSize(),
                    ALLOWED_EXTENSIONS.get(extension));
            saved.add(attachmentRepository.save(attachment));
        }
        return saved;
    }

    public StoredAttachment load(long submissionId, long attachmentId) throws IOException {
        SubmissionAttachment attachment = attachmentRepository
                .findByIdAndSubmissionId(attachmentId, submissionId)
                .orElseThrow(() -> new IllegalArgumentException("附件不存在"));
        Path file = storageRoot.resolve(attachment.getStoredName()).normalize();
        if (!file.startsWith(storageRoot) || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("附件文件不存在");
        }
        Resource resource = new UrlResource(file.toUri());
        return new StoredAttachment(attachment, resource);
    }

    private String extensionOf(String filename) {
        String extension = StringUtils.getFilenameExtension(filename);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    private String sanitizeOriginalName(String filename) {
        String name = StringUtils.cleanPath(filename == null ? "attachment" : filename);
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        return slash >= 0 ? name.substring(slash + 1) : name;
    }

    public record StoredAttachment(SubmissionAttachment attachment, Resource resource) {
    }
}
