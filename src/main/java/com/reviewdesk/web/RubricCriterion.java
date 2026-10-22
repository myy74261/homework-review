package com.reviewdesk.web;

/**
 * 页面展示用的一条评分量规。
 *
 * @param name 评分维度
 * @param points 该维度分值
 * @param state 当前观察状态
 * @param description 审核时要核对的内容
 */
public record RubricCriterion(String name, int points, String state, String description) {
}

