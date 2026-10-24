package com.reviewdesk.web;

import com.reviewdesk.service.AccessControlService;
import com.reviewdesk.service.ReviewScoreService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 保存教师最终评分。
 */
@Controller
public class ReviewScoreController {

    private final AccessControlService accessControlService;
    private final ReviewScoreService scoreService;

    public ReviewScoreController(AccessControlService accessControlService,
                                 ReviewScoreService scoreService) {
        this.accessControlService = accessControlService;
        this.scoreService = scoreService;
    }

    @PostMapping("/teacher/submissions/{submissionId}/scores")
    public String saveScores(@PathVariable long submissionId,
                             @RequestParam Map<String, String> parameters,
                             Authentication authentication) {
        accessControlService.requireTeacherSubmission(
                authentication.getName(), submissionId);
        Map<Long, Integer> scores = new LinkedHashMap<>();
        parameters.forEach((name, value) -> {
            if (name.startsWith("score_") && !value.isBlank()) {
                scores.put(Long.parseLong(name.substring("score_".length())),
                        Integer.parseInt(value));
            }
        });
        scoreService.saveFinalScores(submissionId, scores);
        return "redirect:/reviews/" + submissionId + "#scores";
    }
}
