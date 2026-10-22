package com.reviewdesk.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 比较两个审核反馈版本的行级差异。
 */
@Service
public class ReviewDiffService {

    private static final int CONTEXT_LINES = 2;

    /**
     * 使用最长公共子序列生成历史反馈到当前反馈的增删行。
     */
    public DiffResult diff(String oldText, String newText) {
        List<String> oldLines = List.of(oldText.split("\\R", -1));
        List<String> newLines = List.of(newText.split("\\R", -1));
        int[][] lengths = buildLcs(oldLines, newLines);
        List<DiffLine> lines = new ArrayList<>();
        int oldIndex = 0;
        int newIndex = 0;
        int added = 0;
        int removed = 0;

        while (oldIndex < oldLines.size() && newIndex < newLines.size()) {
            if (oldLines.get(oldIndex).equals(newLines.get(newIndex))) {
                lines.add(new DiffLine("EQUAL", oldLines.get(oldIndex)));
                oldIndex++;
                newIndex++;
            } else if (lengths[oldIndex + 1][newIndex]
                    >= lengths[oldIndex][newIndex + 1]) {
                lines.add(new DiffLine("REMOVE", oldLines.get(oldIndex)));
                removed++;
                oldIndex++;
            } else {
                lines.add(new DiffLine("ADD", newLines.get(newIndex)));
                added++;
                newIndex++;
            }
        }
        while (oldIndex < oldLines.size()) {
            lines.add(new DiffLine("REMOVE", oldLines.get(oldIndex++)));
            removed++;
        }
        while (newIndex < newLines.size()) {
            lines.add(new DiffLine("ADD", newLines.get(newIndex++)));
            added++;
        }
        return new DiffResult(compact(lines), added, removed);
    }

    private int[][] buildLcs(List<String> oldLines, List<String> newLines) {
        int[][] lengths = new int[oldLines.size() + 1][newLines.size() + 1];
        for (int oldIndex = oldLines.size() - 1; oldIndex >= 0; oldIndex--) {
            for (int newIndex = newLines.size() - 1; newIndex >= 0; newIndex--) {
                lengths[oldIndex][newIndex] =
                        oldLines.get(oldIndex).equals(newLines.get(newIndex))
                                ? lengths[oldIndex + 1][newIndex + 1] + 1
                                : Math.max(lengths[oldIndex + 1][newIndex],
                                        lengths[oldIndex][newIndex + 1]);
            }
        }
        return lengths;
    }

    private List<DiffLine> compact(List<DiffLine> lines) {
        List<DiffLine> result = new ArrayList<>();
        int equalStart = -1;
        for (int index = 0; index <= lines.size(); index++) {
            boolean equal = index < lines.size() && "EQUAL".equals(lines.get(index).type());
            if (equal) {
                if (equalStart < 0) {
                    equalStart = index;
                }
                continue;
            }

            if (equalStart >= 0) {
                int runLength = index - equalStart;
                if (runLength > CONTEXT_LINES * 2 + 1) {
                    result.addAll(lines.subList(equalStart, equalStart + CONTEXT_LINES));
                    result.add(new DiffLine("HUNK",
                            "··· 省略 " + (runLength - CONTEXT_LINES * 2) + " 行 ···"));
                    result.addAll(lines.subList(index - CONTEXT_LINES, index));
                } else {
                    result.addAll(lines.subList(equalStart, index));
                }
                equalStart = -1;
            }
            if (index < lines.size()) {
                result.add(lines.get(index));
            }
        }
        return result;
    }

    public record DiffLine(String type, String text) {
    }

    public record DiffResult(List<DiffLine> lines, int added, int removed) {
    }
}
