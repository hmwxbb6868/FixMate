package com.fixmate.service.common;

import java.util.Map;

/**
 * 工单时间线的用户可见文案。
 *
 * 内部状态码、规则代码与迁移表达式只保留在 work_order_status_log 与 audit_log 中，
 * 不进入用户端展示；这里的映射是唯一出口，未知状态一律给中性兜底，不回退显示原始代码。
 */
public final class UserTimelineText {

    public static final String CREATED = "报修已提交";
    public static final String UNKNOWN = "进度已更新";

    private static final Map<String, String> STATUS_LABELS = Map.ofEntries(
            Map.entry("WAITING_MANUAL_REVIEW", "等待人工处理"),
            Map.entry("WAITING_CUSTOMER_VISIT", "待选上门时段"),
            Map.entry("WAITING_ASSIGNMENT", "等待派单"),
            Map.entry("WAITING_APPOINTMENT", "等待预约"),
            Map.entry("APPOINTED", "已预约"),
            Map.entry("IN_INSPECTION", "上门检测中"),
            Map.entry("WAITING_QUOTE", "等待报价"),
            Map.entry("WAITING_QUOTE_CONFIRMATION", "报价待确认"),
            Map.entry("IN_REPAIR", "维修中"),
            Map.entry("PAUSED_FOR_REQUOTE", "维修暂停，待确认追加报价"),
            Map.entry("WAITING_ACCEPTANCE", "待验收"),
            Map.entry("COMPLETED", "已完成"),
            Map.entry("CANCELLED", "已取消"),
            Map.entry("CLOSED_UNRESOLVED", "已关闭"));

    private UserTimelineText() {
    }

    public static String statusLabel(String status) {
        if (status == null) {
            return UNKNOWN;
        }
        return STATUS_LABELS.getOrDefault(status, UNKNOWN);
    }

    /** 首条日志固定为「报修已提交」；其余以目标状态的用户说法为准。 */
    public static String eventTitle(String fromStatus, String toStatus) {
        if (fromStatus == null) {
            return CREATED;
        }
        return statusLabel(toStatus);
    }
}
