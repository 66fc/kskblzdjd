-- =============================================================
-- 羽毛球场地管理系统 - 08 测试用例（真实执行，可重复运行）
-- 运行方式：mysql --default-character-set=utf8mb4 -uroot -p < 08_test_cases.sql
-- 说明：涉及写操作的用例均在事务中执行并 ROLLBACK，不污染演示数据。
-- =============================================================
USE badminton_db;
SET NAMES utf8mb4;

SET @today = CURDATE();
SET @yesterday = DATE_SUB(@today, INTERVAL 1 DAY);
SET @tomorrow = DATE_ADD(@today, INTERVAL 1 DAY);
SET @test_day = DATE_ADD(@today, INTERVAL 10 DAY);

DROP PROCEDURE IF EXISTS sp_test_try_booking;
DROP PROCEDURE IF EXISTS sp_test_room_check;

DELIMITER $$

-- 辅助过程：尝试插入预约并捕获触发器错误，用于"重复预约拦截"测试
CREATE PROCEDURE sp_test_try_booking(
    IN p_member_id BIGINT UNSIGNED,
    IN p_court_id  BIGINT UNSIGNED,
    IN p_start     DATETIME,
    IN p_end       DATETIME,
    IN p_rate      DECIMAL(10,2),
    IN p_discount  DECIMAL(4,2),
    IN p_payable   DECIMAL(10,2)
)
BEGIN
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;
    DECLARE v_no  VARCHAR(32);
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
        BEGIN
            GET DIAGNOSTICS CONDITION 1 v_msg = MESSAGE_TEXT;
        END;

    SET v_no = CONCAT('TEST', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), FLOOR(RAND() * 1000));

    INSERT INTO booking (booking_no, request_id, member_id, court_id, start_at, end_at, duration_minutes,
                         hourly_rate_snapshot, discount_snapshot, original_amount, payable_amount, paid_amount,
                         status)
    VALUES (v_no, CONCAT('test-', v_no), p_member_id, p_court_id, p_start, p_end,
            TIMESTAMPDIFF(MINUTE, p_start, p_end), p_rate, p_discount,
            ROUND(p_rate * TIMESTAMPDIFF(MINUTE, p_start, p_end) / 60, 2), p_payable, p_payable, 'CONFIRMED');

    IF v_msg IS NULL THEN
        SELECT '插入成功（未被拦截）' AS result;
    ELSE
        SELECT CONCAT('拦截成功：', v_msg) AS result;
    END IF;
END$$

DELIMITER ;

-- =============================================================
-- 测试一：预约成功，触发器占用对应时间段
-- =============================================================
SELECT '===== 测试一：预约成功自动占用时间段 =====' AS test_case;
START TRANSACTION;
SELECT '预约前 court5 12:00-13:00 时间段' AS step;
SELECT slot_start, slot_end, slot_status, booking_id
FROM court_slot
WHERE court_id = 5 AND slot_start >= TIMESTAMP(@test_day, '12:00:00') AND slot_end <= TIMESTAMP(@test_day, '13:00:00');

INSERT INTO booking (booking_no, request_id, member_id, court_id, start_at, end_at, duration_minutes,
                     hourly_rate_snapshot, discount_snapshot, original_amount, payable_amount, paid_amount, status)
VALUES (CONCAT('TEST-OCC-', DATE_FORMAT(NOW(), '%H%i%s')), CONCAT('test-occ-', UNIX_TIMESTAMP()),
        2, 5, TIMESTAMP(@test_day, '12:00:00'), TIMESTAMP(@test_day, '13:00:00'), 60,
        80.00, 1.00, 80.00, 80.00, 80.00, 'CONFIRMED');

SELECT '预约成功后 court5 12:00-13:00 时间段' AS step;
SELECT slot_start, slot_end, slot_status, booking_id
FROM court_slot
WHERE court_id = 5 AND slot_start >= TIMESTAMP(@test_day, '12:00:00') AND slot_end <= TIMESTAMP(@test_day, '13:00:00');
ROLLBACK;

-- =============================================================
-- 测试二：取消预约，仅释放本订单占用的时间段
-- =============================================================
SELECT '===== 测试二：取消预约仅释放自身时间段 =====' AS test_case;
START TRANSACTION;
INSERT INTO booking (booking_no, request_id, member_id, court_id, start_at, end_at, duration_minutes,
                     hourly_rate_snapshot, discount_snapshot, original_amount, payable_amount, paid_amount, status)
VALUES (CONCAT('TEST-A-', DATE_FORMAT(NOW(), '%H%i%s')), CONCAT('test-a-', UNIX_TIMESTAMP()),
        2, 5, TIMESTAMP(@test_day, '14:00:00'), TIMESTAMP(@test_day, '15:00:00'), 60,
        80.00, 1.00, 80.00, 80.00, 80.00, 'CONFIRMED');
SET @test_booking_a = LAST_INSERT_ID();

INSERT INTO booking (booking_no, request_id, member_id, court_id, start_at, end_at, duration_minutes,
                     hourly_rate_snapshot, discount_snapshot, original_amount, payable_amount, paid_amount, status)
VALUES (CONCAT('TEST-B-', DATE_FORMAT(NOW(), '%H%i%s')), CONCAT('test-b-', UNIX_TIMESTAMP()),
        3, 5, TIMESTAMP(@test_day, '15:00:00'), TIMESTAMP(@test_day, '16:00:00'), 60,
        80.00, 0.95, 80.00, 76.00, 76.00, 'CONFIRMED');

SELECT '取消前：两个相邻订单均占用' AS step;
SELECT booking_id, slot_start, slot_status FROM court_slot
WHERE court_id = 5 AND slot_start >= TIMESTAMP(@test_day, '14:00:00') AND slot_end <= TIMESTAMP(@test_day, '16:00:00')
ORDER BY slot_start;

UPDATE booking SET status = 'CANCELLED', cancelled_at = NOW(), cancel_reason = '测试取消'
WHERE booking_id = @test_booking_a;

SELECT '取消后：仅 A 订单时间段释放，B 订单仍占用' AS step;
SELECT booking_id, slot_start, slot_status FROM court_slot
WHERE court_id = 5 AND slot_start >= TIMESTAMP(@test_day, '14:00:00') AND slot_end <= TIMESTAMP(@test_day, '16:00:00')
ORDER BY slot_start;
ROLLBACK;

-- =============================================================
-- 测试三：重复预约同一时间段被数据库触发器拦截
-- =============================================================
SELECT '===== 测试三：重复预约拦截 =====' AS test_case;
START TRANSACTION;
CALL sp_test_try_booking(2, 5, TIMESTAMP(@test_day, '17:00:00'), TIMESTAMP(@test_day, '18:00:00'), 80.00, 1.00, 80.00);
SELECT '第一次预约结果（应成功）' AS step;
CALL sp_test_try_booking(3, 5, TIMESTAMP(@test_day, '17:30:00'), TIMESTAMP(@test_day, '18:30:00'), 80.00, 0.95, 76.00);
SELECT '第二次预约结果（应被拦截）' AS step;
ROLLBACK;

-- 维护中场地不可预约
SELECT '===== 测试三补充：维护中场地不可预约 =====' AS test_case;
START TRANSACTION;
CALL sp_test_try_booking(2, 6, TIMESTAMP(@test_day, '19:00:00'), TIMESTAMP(@test_day, '20:00:00'), 60.00, 1.00, 60.00);
ROLLBACK;

-- =============================================================
-- 测试四：视图查询指定区间可预约场地
-- =============================================================
SELECT '===== 测试四：视图查询可预约场地 =====' AS test_case;
SELECT '查询明日 14:00-16:00 完整空闲的场地（每块场地需 4 个时间段）' AS step;
SELECT venue_id, venue_name, court_id, court_name, hourly_rate, COUNT(*) AS free_slots
FROM v_available_court_slots
WHERE slot_start >= TIMESTAMP(@tomorrow, '14:00:00')
  AND slot_end <= TIMESTAMP(@tomorrow, '16:00:00')
GROUP BY venue_id, venue_name, court_id, court_name, hourly_rate
HAVING COUNT(*) = 4
ORDER BY court_id;

SELECT '对照：明日 11:00-13:00，3号场地 10:00-12:00 已预约，应被排除' AS step;
SELECT court_id, court_name, COUNT(*) AS free_slots
FROM v_available_court_slots
WHERE slot_start >= TIMESTAMP(@tomorrow, '11:00:00')
  AND slot_end <= TIMESTAMP(@tomorrow, '13:00:00')
GROUP BY court_id, court_name
HAVING COUNT(*) = 4
ORDER BY court_id;

-- =============================================================
-- 测试五：单一场地某日实际使用统计
-- =============================================================
SELECT '===== 测试五：单场地日统计（昨日 1 号场地，预期 110 分钟 / 120.00 元）=====' AS test_case;
CALL sp_daily_court_stats_by_court(@yesterday, 1);

SELECT '对照：昨日 3 号场地（跨日订单昨日部分，预期 90 分钟 / 135.00 元）' AS step;
CALL sp_daily_court_stats_by_court(@yesterday, 3);

SELECT '对照：昨日 5 号场地（无使用记录，预期 0 / 0.00）' AS step;
CALL sp_daily_court_stats_by_court(@yesterday, 5);

-- =============================================================
-- 测试六：全部场地汇总统计
-- =============================================================
SELECT '===== 测试六：全部场地汇总（昨日，预期 290 分钟 / 4.83 小时 / 369.00 元）=====' AS test_case;
CALL sp_daily_court_stats_all(@yesterday);

SELECT '今日汇总（预期 120 分钟 / 2.00 小时 / 198.00 元，含跨日订单今日部分）' AS step;
CALL sp_daily_court_stats_all(@today);

SELECT '无数据日期汇总（预期 0，而不是 NULL）' AS step;
CALL sp_daily_court_stats_all(DATE_SUB(@today, INTERVAL 5 DAY));

-- =============================================================
-- 测试七：管理员手动修改会员等级写入变更日志
-- =============================================================
SELECT '===== 测试七：人工调级写入日志 =====' AS test_case;
START TRANSACTION;
SELECT member_id, nickname, level_id, total_consumption, level_manually_adjusted
FROM member WHERE member_id = 7;
SET @old_level = (SELECT level_id FROM member WHERE member_id = 7);

UPDATE member
SET level_id = 3, level_manually_adjusted = 1, manual_adjusted_at = NOW(), manual_adjusted_by = 1
WHERE member_id = 7;

INSERT INTO member_level_change_log (member_id, old_level_id, new_level_id, operator_id, change_type, reason)
VALUES (7, @old_level, 3, 1, 'MANUAL', '测试：管理员手动调整为金卡会员');

SELECT m.member_id, m.nickname, m.level_id, ml.level_name, m.level_manually_adjusted
FROM member m JOIN member_level ml ON ml.level_id = m.level_id WHERE m.member_id = 7;

SELECT change_id, member_id, old_level_id, new_level_id, operator_id, change_type, reason, created_at
FROM member_level_change_log WHERE member_id = 7 ORDER BY change_id DESC LIMIT 1;
ROLLBACK;

-- =============================================================
-- 测试八：普通用户修改会员等级被拒绝（接口层验证）
-- =============================================================
SELECT '===== 测试八：权限校验 =====' AS test_case;
SELECT '普通用户角色为 MEMBER，管理接口要求 ADMIN；详细 HTTP 403 结果见 docs/测试记录.md' AS note;
SELECT member_id, username, role, status FROM member WHERE role = 'MEMBER' ORDER BY member_id LIMIT 3;

-- =============================================================
-- 测试九：模拟时间切换后日期正确变化
-- =============================================================
SELECT '===== 测试九：模拟时钟切换 =====' AS test_case;
START TRANSACTION;
SELECT simulated_now, clock_mode, clock_paused, clock_speed, demo_mode FROM system_setting WHERE setting_id = 1;

UPDATE system_setting
SET simulated_now = TIMESTAMP(DATE_ADD(@today, INTERVAL 3 DAY), '09:30:00'),
    base_real_at = NOW(), clock_mode = 'SIMULATED', clock_paused = 1
WHERE setting_id = 1;

SELECT simulated_now, DATE(simulated_now) AS business_date, clock_paused
FROM system_setting WHERE setting_id = 1;

SELECT '切换后 3 天后仍有已生成的时间段（未来 14 天预生成机制）' AS step;
SELECT COUNT(*) AS slots_on_target_date
FROM court_slot WHERE DATE(slot_start) = DATE_ADD(@today, INTERVAL 3 DAY);
ROLLBACK;

-- =============================================================
-- 测试十：跨自然日使用记录的日统计拆分
-- =============================================================
SELECT '===== 测试十：跨自然日订单拆分 =====' AS test_case;
SELECT usage_id, booking_id, actual_start_at, actual_end_at, actual_minutes, actual_fee_amount
FROM court_usage_record WHERE court_id = 3 ORDER BY usage_id DESC LIMIT 1;

SELECT '昨日部分：22:30-24:00 = 90 分钟，180 * 90/120 = 135.00 元' AS step;
CALL sp_daily_court_stats_by_court(@yesterday, 3);

SELECT '今日部分：00:00-00:30 = 30 分钟，180 * 30/120 = 45.00 元' AS step;
CALL sp_daily_court_stats_by_court(@today, 3);

SELECT '两天合计应等于订单实际金额 180.00 元' AS step;
SELECT SUM(amount) AS two_day_total FROM (
    SELECT ROUND(180.00 * TIMESTAMPDIFF(MINUTE, GREATEST(actual_start_at, TIMESTAMP(@yesterday, '00:00:00')),
                 LEAST(actual_end_at, TIMESTAMP(@today, '00:00:00'))) / actual_minutes, 2) AS amount
    FROM court_usage_record WHERE court_id = 3 AND DATE(actual_start_at) = @yesterday
    UNION ALL
    SELECT ROUND(180.00 * TIMESTAMPDIFF(MINUTE, GREATEST(actual_start_at, TIMESTAMP(@today, '00:00:00')),
                 LEAST(actual_end_at, TIMESTAMP(@tomorrow, '00:00:00'))) / actual_minutes, 2)
    FROM court_usage_record WHERE court_id = 3 AND DATE(actual_start_at) = @yesterday
) t;

SELECT '===== 测试结束：全部用例执行完毕 =====' AS result;
