-- =============================================================
-- 羽毛球场地管理系统 - 09 视图与存储过程调用示例
-- 可直接在 mysql 客户端执行，便于验收演示
-- =============================================================
USE badminton_db;
SET NAMES utf8mb4;

SET @d = CURDATE();
SET @tomorrow = DATE_ADD(@d, INTERVAL 1 DAY);

-- -------------------------------------------------------------
-- 1. 视图：查询指定日期 14:00-16:00 完整空闲的场地
-- 条件：区间内 4 个 30 分钟时间段全部空闲，才返回该场地
-- -------------------------------------------------------------
SELECT venue_id,
       venue_name,
       court_id,
       court_name,
       hourly_rate,
       COUNT(*) AS free_slots
FROM v_available_court_slots
WHERE slot_start >= TIMESTAMP(@tomorrow, '14:00:00')
  AND slot_end <= TIMESTAMP(@tomorrow, '16:00:00')
GROUP BY venue_id, venue_name, court_id, court_name, hourly_rate
HAVING COUNT(*) = 4
ORDER BY court_id;

-- -------------------------------------------------------------
-- 2. 视图：查询某个时间段内仍空闲的 30 分钟段明细
-- -------------------------------------------------------------
SELECT venue_name, court_name, hourly_rate, slot_start, slot_end, slot_status
FROM v_available_court_slots
WHERE slot_start >= TIMESTAMP(@tomorrow, '14:00:00')
  AND slot_end <= TIMESTAMP(@tomorrow, '16:00:00')
ORDER BY court_id, slot_start;

-- -------------------------------------------------------------
-- 3. 存储过程：单一场地某日统计
-- -------------------------------------------------------------
CALL sp_daily_court_stats_by_court(@d, 1);

-- -------------------------------------------------------------
-- 4. 存储过程：全部场地某日汇总
-- -------------------------------------------------------------
CALL sp_daily_court_stats_all(@d);

-- -------------------------------------------------------------
-- 5. 跨自然日订单拆分核对
-- -------------------------------------------------------------
SELECT usage_id,
       actual_start_at,
       actual_end_at,
       actual_minutes,
       actual_fee_amount,
       TIMESTAMPDIFF(MINUTE, actual_start_at, DATE_ADD(DATE(actual_start_at), INTERVAL 1 DAY)) AS day1_minutes,
       TIMESTAMPDIFF(MINUTE, DATE_ADD(DATE(actual_start_at), INTERVAL 1 DAY), actual_end_at) AS day2_minutes
FROM court_usage_record
WHERE DATE(actual_end_at) > DATE(actual_start_at);

-- -------------------------------------------------------------
-- 6. 时间段初始化（幂等，可重复执行）
-- -------------------------------------------------------------
CALL sp_generate_court_slots(@d, DATE_ADD(@d, INTERVAL 14 DAY));
