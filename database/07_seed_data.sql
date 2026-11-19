-- =============================================================
-- 羽毛球场地管理系统 - 07 初始化测试数据
-- 说明：
--   1. 日期全部相对 CURDATE() 生成，任何一天执行都可用。
--   2. 密码为 BCrypt 哈希：
--        admin123  -> $2b$10$eGY8...
--        123456    -> $2b$10$fzWa...
--   3. 预约数据通过真实 INSERT 写入，触发器会自动占用/释放时间段。
--   4. 今日租赁收入预期 198.00 元（含跨日订单今日部分 45.00）。
-- =============================================================
USE badminton_db;
SET NAMES utf8mb4;

SET @today = CURDATE();
SET @yesterday = DATE_SUB(@today, INTERVAL 1 DAY);
SET @tomorrow = DATE_ADD(@today, INTERVAL 1 DAY);
SET @hash_123456 = '$2b$10$fzWaMEe5KNPzSYQfynNgwu2TZjfpl.9lzHZ4RFh8W5pINf6CzZirS';
SET @hash_admin  = '$2b$10$eGY8QITFntr0rYPOMnxr5e0gQkjd9muq7I0MjE71.wS7mEoEZqPoa';

-- -------------------------------------------------------------
-- 场馆
-- -------------------------------------------------------------
INSERT INTO venue (venue_id, venue_name, address, phone, opening_time, closing_time, status)
VALUES (1, '星羽羽毛球馆', '深圳市龙岗区坪地街道星羽路 88 号', '0755-88886666', '08:00:00', '23:30:00', 'ACTIVE');

-- -------------------------------------------------------------
-- 会员等级
-- -------------------------------------------------------------
INSERT INTO member_level (level_id, level_name, level_rank, discount_rate, minimum_consumption, description, status)
VALUES (1, '普通会员', 1, 1.00, 0.00, '注册即享，无折扣', 'ENABLED'),
       (2, '银卡会员', 2, 0.95, 1000.00, '累计消费满 1000 元，场地 95 折', 'ENABLED'),
       (3, '金卡会员', 3, 0.90, 3000.00, '累计消费满 3000 元，场地 9 折', 'ENABLED'),
       (4, '钻石会员', 4, 0.85, 8000.00, '累计消费满 8000 元，场地 85 折', 'ENABLED');

-- -------------------------------------------------------------
-- 场地（5 块开放 / 1 块维护 / 1 块停用）
-- -------------------------------------------------------------
INSERT INTO court (court_id, venue_id, court_name, court_number, hourly_rate, operational_status, description)
VALUES (1, 1, '1号场地', 'C01', 60.00, 'OPEN', '标准双打场地，木地板'),
       (2, 1, '2号场地', 'C02', 80.00, 'OPEN', '标准双打场地，专业地胶'),
       (3, 1, '3号场地', 'C03', 100.00, 'OPEN', '赛事级场地，灯光升级'),
       (4, 1, '4号场地', 'C04', 120.00, 'OPEN', 'VIP 场地，独立休息区'),
       (5, 1, '5号场地', 'C05', 80.00, 'OPEN', '标准双打场地'),
       (6, 1, '6号场地', 'C06', 60.00, 'MAINTENANCE', '地胶翻新中，暂停开放'),
       (7, 1, '7号场地', 'C07', 60.00, 'DISABLED', '旧场地，已停用');

-- -------------------------------------------------------------
-- 用户（管理员 + 会员）
-- -------------------------------------------------------------
INSERT INTO member (member_id, username, password_hash, nickname, phone, level_id, role, total_consumption, status)
VALUES (1, 'admin', @hash_admin, '星羽管理员', '13800000000', 4, 'ADMIN', 0.00, 'ACTIVE'),
       (2, 'member01', @hash_123456, '张小白', '13800000001', 1, 'MEMBER', 320.00, 'ACTIVE'),
       (3, 'member02', @hash_123456, '李青羽', '13800000002', 2, 'MEMBER', 1600.00, 'ACTIVE'),
       (4, 'member03', @hash_123456, '王羽然', '13800000003', 3, 'MEMBER', 5200.00, 'ACTIVE'),
       (5, 'member04', @hash_123456, '陈星野', '13800000004', 4, 'MEMBER', 9800.00, 'ACTIVE'),
       (6, 'member05', @hash_123456, '赵小满', '13800000005', 1, 'MEMBER', 0.00, 'DISABLED'),
       (7, 'member06', @hash_123456, '林晓风', '13800000006', 2, 'MEMBER', 1200.00, 'ACTIVE'),
       (8, 'member07', @hash_123456, '何雨桐', '13800000007', 3, 'MEMBER', 3600.00, 'ACTIVE'),
       (9, 'member08', @hash_123456, '苏晴', '13800000008', 1, 'MEMBER', 150.00, 'ACTIVE');

-- 何雨桐被管理员手动调整过等级：自动升级不再覆盖
UPDATE member
SET level_manually_adjusted = 1,
    manual_adjusted_at = TIMESTAMP(@yesterday, '15:00:00'),
    manual_adjusted_by = 1
WHERE member_id = 8;

INSERT INTO member_level_change_log (member_id, old_level_id, new_level_id, operator_id, change_type, reason, created_at)
VALUES (8, 2, 3, 1, 'MANUAL', '老客户回馈，管理员手动升级至金卡会员', TIMESTAMP(@yesterday, '15:00:00'));

-- -------------------------------------------------------------
-- 生成时间段：过去 7 天 ~ 未来 30 天（30 分钟粒度）
-- -------------------------------------------------------------
CALL sp_generate_court_slots(DATE_SUB(@today, INTERVAL 7 DAY), DATE_ADD(@today, INTERVAL 30 DAY));

-- -------------------------------------------------------------
-- 历史预约 1：张小白 1号场地 昨天 18:00-20:00（已完成）
-- -------------------------------------------------------------
INSERT INTO booking (booking_no, request_id, member_id, court_id, start_at, end_at, duration_minutes,
                     hourly_rate_snapshot, discount_snapshot, original_amount, payable_amount, paid_amount,
                     status, created_at)
VALUES (CONCAT('BK', DATE_FORMAT(@yesterday, '%Y%m%d'), '0001'), 'seed-b1', 2, 1,
        TIMESTAMP(@yesterday, '18:00:00'), TIMESTAMP(@yesterday, '20:00:00'), 120,
        60.00, 1.00, 120.00, 120.00, 120.00, 'CONFIRMED', TIMESTAMP(@yesterday, '16:20:00'));
SET @b1 = LAST_INSERT_ID();

INSERT INTO court_usage_record (booking_id, court_id, actual_start_at, actual_end_at, actual_minutes,
                                billed_minutes, actual_fee_amount, settlement_status)
VALUES (@b1, 1, TIMESTAMP(@yesterday, '18:05:00'), TIMESTAMP(@yesterday, '19:55:00'), 110, 120, 120.00, 'SETTLED');

UPDATE booking
SET status = 'COMPLETED', actual_start_at = TIMESTAMP(@yesterday, '18:05:00'),
    actual_end_at = TIMESTAMP(@yesterday, '19:55:00'), paid_amount = 120.00
WHERE booking_id = @b1;

-- -------------------------------------------------------------
-- 历史预约 2：李青羽 2号场地 昨天 19:00-21:00（已完成，银卡 95 折）
-- -------------------------------------------------------------
INSERT INTO booking (booking_no, request_id, member_id, court_id, start_at, end_at, duration_minutes,
                     hourly_rate_snapshot, discount_snapshot, original_amount, payable_amount, paid_amount,
                     status, created_at)
VALUES (CONCAT('BK', DATE_FORMAT(@yesterday, '%Y%m%d'), '0002'), 'seed-b2', 3, 2,
        TIMESTAMP(@yesterday, '19:00:00'), TIMESTAMP(@yesterday, '21:00:00'), 120,
        80.00, 0.95, 160.00, 152.00, 114.00, 'CONFIRMED', TIMESTAMP(@yesterday, '17:05:00'));
SET @b2 = LAST_INSERT_ID();

INSERT INTO court_usage_record (booking_id, court_id, actual_start_at, actual_end_at, actual_minutes,
                                billed_minutes, actual_fee_amount, settlement_status)
VALUES (@b2, 2, TIMESTAMP(@yesterday, '19:10:00'), TIMESTAMP(@yesterday, '20:40:00'), 90, 90, 114.00, 'SETTLED');

UPDATE booking
SET status = 'COMPLETED', actual_start_at = TIMESTAMP(@yesterday, '19:10:00'),
    actual_end_at = TIMESTAMP(@yesterday, '20:40:00'), paid_amount = 114.00
WHERE booking_id = @b2;

-- -------------------------------------------------------------
-- 历史预约 3：王羽然 3号场地 昨天 22:00-23:30（跨自然日实际使用至今日 00:30）
-- 用于验证"跨自然日统计不重复计费"
-- -------------------------------------------------------------
INSERT INTO booking (booking_no, request_id, member_id, court_id, start_at, end_at, duration_minutes,
                     hourly_rate_snapshot, discount_snapshot, original_amount, payable_amount, paid_amount,
                     status, created_at)
VALUES (CONCAT('BK', DATE_FORMAT(@yesterday, '%Y%m%d'), '0003'), 'seed-b3', 4, 3,
        TIMESTAMP(@yesterday, '22:00:00'), TIMESTAMP(@yesterday, '23:30:00'), 90,
        100.00, 0.90, 150.00, 135.00, 180.00, 'CONFIRMED', TIMESTAMP(@yesterday, '20:10:00'));
SET @b3 = LAST_INSERT_ID();

INSERT INTO court_usage_record (booking_id, court_id, actual_start_at, actual_end_at, actual_minutes,
                                billed_minutes, actual_fee_amount, settlement_status)
VALUES (@b3, 3, TIMESTAMP(@yesterday, '22:30:00'), TIMESTAMP(@today, '00:30:00'), 120, 120, 180.00, 'SETTLED');

UPDATE booking
SET status = 'COMPLETED', actual_start_at = TIMESTAMP(@yesterday, '22:30:00'),
    actual_end_at = TIMESTAMP(@today, '00:30:00'), paid_amount = 180.00
WHERE booking_id = @b3;

-- -------------------------------------------------------------
-- 今日预约 4：陈星野 4号场地 今日 08:00-10:00（已完成，钻石 85 折）
-- -------------------------------------------------------------
INSERT INTO booking (booking_no, request_id, member_id, court_id, start_at, end_at, duration_minutes,
                     hourly_rate_snapshot, discount_snapshot, original_amount, payable_amount, paid_amount,
                     status, created_at)
VALUES (CONCAT('BK', DATE_FORMAT(@today, '%Y%m%d'), '0004'), 'seed-b4', 5, 4,
        TIMESTAMP(@today, '08:00:00'), TIMESTAMP(@today, '10:00:00'), 120,
        120.00, 0.85, 240.00, 204.00, 153.00, 'CONFIRMED', TIMESTAMP(@today, '07:25:00'));
SET @b4 = LAST_INSERT_ID();

INSERT INTO court_usage_record (booking_id, court_id, actual_start_at, actual_end_at, actual_minutes,
                                billed_minutes, actual_fee_amount, settlement_status)
VALUES (@b4, 4, TIMESTAMP(@today, '08:00:00'), TIMESTAMP(@today, '09:30:00'), 90, 90, 153.00, 'SETTLED');

UPDATE booking
SET status = 'COMPLETED', actual_start_at = TIMESTAMP(@today, '08:00:00'),
    actual_end_at = TIMESTAMP(@today, '09:30:00'), paid_amount = 153.00
WHERE booking_id = @b4;

-- -------------------------------------------------------------
-- 今日预约 5：张小白 1号场地 今日 09:00-11:00（使用中）
-- -------------------------------------------------------------
INSERT INTO booking (booking_no, request_id, member_id, court_id, start_at, end_at, duration_minutes,
                     hourly_rate_snapshot, discount_snapshot, original_amount, payable_amount, paid_amount,
                     status, created_at)
VALUES (CONCAT('BK', DATE_FORMAT(@today, '%Y%m%d'), '0005'), 'seed-b5', 2, 1,
        TIMESTAMP(@today, '09:00:00'), TIMESTAMP(@today, '11:00:00'), 120,
        60.00, 1.00, 120.00, 120.00, 120.00, 'CONFIRMED', TIMESTAMP(@today, '08:10:00'));
SET @b5 = LAST_INSERT_ID();

UPDATE booking
SET status = 'IN_PROGRESS', actual_start_at = TIMESTAMP(@today, '09:05:00')
WHERE booking_id = @b5;

-- -------------------------------------------------------------
-- 今日预约 6：李青羽 2号场地 今日 14:00-16:00（已确认，待使用）
-- -------------------------------------------------------------
INSERT INTO booking (booking_no, request_id, member_id, court_id, start_at, end_at, duration_minutes,
                     hourly_rate_snapshot, discount_snapshot, original_amount, payable_amount, paid_amount,
                     status, created_at)
VALUES (CONCAT('BK', DATE_FORMAT(@today, '%Y%m%d'), '0006'), 'seed-b6', 3, 2,
        TIMESTAMP(@today, '14:00:00'), TIMESTAMP(@today, '16:00:00'), 120,
        80.00, 0.95, 160.00, 152.00, 152.00, 'CONFIRMED', TIMESTAMP(@today, '09:30:00'));

-- -------------------------------------------------------------
-- 明日预约 7：王羽然 3号场地 明日 10:00-12:00
-- -------------------------------------------------------------
INSERT INTO booking (booking_no, request_id, member_id, court_id, start_at, end_at, duration_minutes,
                     hourly_rate_snapshot, discount_snapshot, original_amount, payable_amount, paid_amount,
                     status, created_at)
VALUES (CONCAT('BK', DATE_FORMAT(@tomorrow, '%Y%m%d'), '0007'), 'seed-b7', 4, 3,
        TIMESTAMP(@tomorrow, '10:00:00'), TIMESTAMP(@tomorrow, '12:00:00'), 120,
        100.00, 0.90, 200.00, 180.00, 180.00, 'CONFIRMED', TIMESTAMP(@today, '10:05:00'));

-- -------------------------------------------------------------
-- 今日预约 8：陈星野 4号场地 今日 16:00-17:00（已取消，验证时间段释放）
-- -------------------------------------------------------------
INSERT INTO booking (booking_no, request_id, member_id, court_id, start_at, end_at, duration_minutes,
                     hourly_rate_snapshot, discount_snapshot, original_amount, payable_amount, paid_amount,
                     status, created_at)
VALUES (CONCAT('BK', DATE_FORMAT(@today, '%Y%m%d'), '0008'), 'seed-b8', 5, 4,
        TIMESTAMP(@today, '16:00:00'), TIMESTAMP(@today, '17:00:00'), 60,
        120.00, 0.85, 120.00, 102.00, 0.00, 'CONFIRMED', TIMESTAMP(@today, '08:40:00'));
SET @b8 = LAST_INSERT_ID();

UPDATE booking
SET status = 'CANCELLED', cancelled_at = TIMESTAMP(@today, '09:10:00'), cancel_reason = '临时有事，会员取消'
WHERE booking_id = @b8;

-- -------------------------------------------------------------
-- 今日预约 9：苏晴 1号场地 今日 19:00-20:00（待支付，不占场）
-- -------------------------------------------------------------
INSERT INTO booking (booking_no, request_id, member_id, court_id, start_at, end_at, duration_minutes,
                     hourly_rate_snapshot, discount_snapshot, original_amount, payable_amount, paid_amount,
                     status, created_at)
VALUES (CONCAT('BK', DATE_FORMAT(@today, '%Y%m%d'), '0009'), 'seed-b9', 9, 1,
        TIMESTAMP(@today, '19:00:00'), TIMESTAMP(@today, '20:00:00'), 60,
        60.00, 1.00, 60.00, 60.00, 0.00, 'PENDING_PAYMENT', TIMESTAMP(@today, '09:45:00'));

-- -------------------------------------------------------------
-- 昨日预约 10：林晓风 2号场地 昨天 10:00-11:00（未到场，释放时间段）
-- -------------------------------------------------------------
INSERT INTO booking (booking_no, request_id, member_id, court_id, start_at, end_at, duration_minutes,
                     hourly_rate_snapshot, discount_snapshot, original_amount, payable_amount, paid_amount,
                     status, created_at)
VALUES (CONCAT('BK', DATE_FORMAT(@yesterday, '%Y%m%d'), '0010'), 'seed-b10', 7, 2,
        TIMESTAMP(@yesterday, '10:00:00'), TIMESTAMP(@yesterday, '11:00:00'), 60,
        80.00, 0.95, 80.00, 76.00, 0.00, 'CONFIRMED', TIMESTAMP(@yesterday, '09:00:00'));
SET @b10 = LAST_INSERT_ID();

UPDATE booking
SET status = 'NO_SHOW', cancel_reason = '超时未到场'
WHERE booking_id = @b10;

-- -------------------------------------------------------------
-- 今日预约 11：何雨桐 5号场地 今日 18:00-19:00（金卡手动调级会员）
-- -------------------------------------------------------------
INSERT INTO booking (booking_no, request_id, member_id, court_id, start_at, end_at, duration_minutes,
                     hourly_rate_snapshot, discount_snapshot, original_amount, payable_amount, paid_amount,
                     status, created_at)
VALUES (CONCAT('BK', DATE_FORMAT(@today, '%Y%m%d'), '0011'), 'seed-b11', 8, 5,
        TIMESTAMP(@today, '18:00:00'), TIMESTAMP(@today, '19:00:00'), 60,
        80.00, 0.90, 80.00, 72.00, 72.00, 'CONFIRMED', TIMESTAMP(@today, '09:50:00'));

-- -------------------------------------------------------------
-- 明日预约 12：苏晴 5号场地 明日 18:00-20:00
-- -------------------------------------------------------------
INSERT INTO booking (booking_no, request_id, member_id, court_id, start_at, end_at, duration_minutes,
                     hourly_rate_snapshot, discount_snapshot, original_amount, payable_amount, paid_amount,
                     status, created_at)
VALUES (CONCAT('BK', DATE_FORMAT(@tomorrow, '%Y%m%d'), '0012'), 'seed-b12', 9, 5,
        TIMESTAMP(@tomorrow, '18:00:00'), TIMESTAMP(@tomorrow, '20:00:00'), 120,
        80.00, 1.00, 160.00, 160.00, 160.00, 'CONFIRMED', TIMESTAMP(@today, '10:02:00'));

-- -------------------------------------------------------------
-- 商品
-- -------------------------------------------------------------
INSERT INTO product (product_id, product_name, category, price, stock, status)
VALUES (1, 'YONEX AS-05 羽毛球（12只装）', 'BALL', 45.00, 98, 'ON_SALE'),
       (2, '羽毛球拍租赁（单次）', 'RACKET', 20.00, 19, 'ON_SALE'),
       (3, '宝矿力水特 500ml', 'DRINK', 8.00, 98, 'ON_SALE'),
       (4, 'YONEX 手胶', 'ACCESSORY', 15.00, 39, 'ON_SALE'),
       (5, '羽毛球运动袜', 'ACCESSORY', 25.00, 50, 'ON_SALE'),
       (6, '训练用球（6只装）', 'BALL', 30.00, 40, 'OFF_SALE');

-- -------------------------------------------------------------
-- 商品销售订单（租赁收入与商品收入严格分离）
-- -------------------------------------------------------------
INSERT INTO sales_order (sales_order_id, order_no, member_id, total_amount, payment_status, payment_method, paid_at, created_at)
VALUES (1, CONCAT('SO', DATE_FORMAT(@yesterday, '%Y%m%d'), '0001'), 2, 61.00, 'PAID', 'WECHAT',
        TIMESTAMP(@yesterday, '19:00:00'), TIMESTAMP(@yesterday, '18:58:00')),
       (2, CONCAT('SO', DATE_FORMAT(@today, '%Y%m%d'), '0001'), 4, 35.00, 'PAID', 'ALIPAY',
        TIMESTAMP(@today, '09:20:00'), TIMESTAMP(@today, '09:18:00')),
       (3, CONCAT('SO', DATE_FORMAT(@today, '%Y%m%d'), '0002'), 5, 45.00, 'PAID', 'CASH',
        TIMESTAMP(@today, '09:40:00'), TIMESTAMP(@today, '09:39:00')),
       (4, CONCAT('SO', DATE_FORMAT(@today, '%Y%m%d'), '0003'), NULL, 8.00, 'UNPAID', 'CASH',
        NULL, TIMESTAMP(@today, '10:05:00'));

INSERT INTO sales_order_item (sales_order_id, product_id, quantity, unit_price, subtotal)
VALUES (1, 3, 2, 8.00, 16.00),
       (1, 1, 1, 45.00, 45.00),
       (2, 2, 1, 20.00, 20.00),
       (2, 4, 1, 15.00, 15.00),
       (3, 1, 1, 45.00, 45.00),
       (4, 3, 1, 8.00, 8.00);

-- -------------------------------------------------------------
-- 模拟时钟：以今天 10:00 为演示起点
-- -------------------------------------------------------------
INSERT INTO system_setting (setting_id, simulated_now, base_real_at, clock_mode, clock_paused, clock_speed, demo_mode, updated_by)
VALUES (1, TIMESTAMP(@today, '10:00:00'), NOW(), 'SIMULATED', 0, 1.00, 1, 1);

-- -------------------------------------------------------------
-- 管理操作日志样例
-- -------------------------------------------------------------
INSERT INTO admin_operation_log (operator_id, operator_name, module, action, target_type, target_id, detail, ip, created_at)
VALUES (1, '星羽管理员', 'MEMBER_LEVEL', 'UPDATE_LEVEL', 'MEMBER', '8',
        '将会员何雨桐从银卡会员调整为金卡会员，原因：老客户回馈', '127.0.0.1',
        TIMESTAMP(@yesterday, '15:00:00')),
       (1, '星羽管理员', 'COURT', 'UPDATE_COURT', 'COURT', '6',
        '6号场地地胶翻新，状态调整为 MAINTENANCE', '127.0.0.1',
        TIMESTAMP(@yesterday, '09:30:00')),
       (1, '星羽管理员', 'BOOKING', 'CANCEL_BOOKING', 'BOOKING', '8',
        '管理员取消预约并释放时间段，原因：会员临时有事', '127.0.0.1',
        TIMESTAMP(@today, '09:10:00'));

-- -------------------------------------------------------------
-- 数据校验输出
-- -------------------------------------------------------------
SELECT 'seed completed' AS result,
       (SELECT COUNT(*) FROM court)  AS courts,
       (SELECT COUNT(*) FROM member) AS members,
       (SELECT COUNT(*) FROM booking) AS bookings,
       (SELECT COUNT(*) FROM court_slot) AS slots,
       (SELECT COUNT(*) FROM court_usage_record) AS usage_records,
       (SELECT COUNT(*) FROM sales_order) AS sales_orders;
