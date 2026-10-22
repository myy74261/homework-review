package com.reviewdesk.web.api;

/**
 * 一行反馈差异，type 为 EQUAL、ADD、REMOVE 或 HUNK。
 */
public record DiffLineResponse(String type, String text) {
}

