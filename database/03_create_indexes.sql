-- =============================================================
-- 羽毛球场地管理系统 - 03 索引设计
-- 说明：唯一索引与外键索引已在建表语句中创建，本脚本补充
--       面向高频查询的复合索引，并给出用途说明。
-- =============================================================
USE badminton_db;

-- 场地时间段：按场地 + 状态 + 时间检索（可预约查询、维护检查）
ALTER TABLE court_slot
    ADD KEY idx_slot_court_status (court_id, slot_status, slot_start);

-- 预约：按场地 + 状态 + 时间检索（场地日历、冲突检查）
ALTER TABLE booking
    ADD KEY idx_booking_court_status (court_id, status, start_at);

-- 预约：按会员 + 状态检索（我的预约）
ALTER TABLE booking
    ADD KEY idx_booking_member_status (member_id, status);

-- 会员：按角色过滤（后台管理员列表）
ALTER TABLE member
    ADD KEY idx_member_role (role, status);

-- 实际使用记录：按结算状态 + 时间检索（日统计）
ALTER TABLE court_usage_record
    ADD KEY idx_usage_settle_time (settlement_status, actual_start_at);

-- 销售订单：按创建时间检索（销售报表）
ALTER TABLE sales_order
    ADD KEY idx_sales_created (created_at);

-- 操作日志：按模块 + 时间检索
ALTER TABLE admin_operation_log
    ADD KEY idx_log_module_time (module, created_at);
