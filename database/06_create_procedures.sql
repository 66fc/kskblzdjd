-- =============================================================
-- 羽毛球场地管理系统 - 06 存储过程
-- =============================================================
USE badminton_db;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS sp_generate_court_slots;
DROP PROCEDURE IF EXISTS sp_daily_court_stats_by_court;
DROP PROCEDURE IF EXISTS sp_daily_court_stats_all;

DELIMITER $$

-- -------------------------------------------------------------
-- 存储过程 1：批量生成场地时间段（初始化 / 未来时间段生成）
-- 规则：按场馆营业时间，以 30 分钟为粒度，为所有未停用场地
--       生成 [p_from_date, p_to_date] 的时间段，已存在则跳过。
-- -------------------------------------------------------------
CREATE PROCEDURE sp_generate_court_slots(
    IN p_from_date DATE,
    IN p_to_date   DATE
)
BEGIN
    DECLARE v_court_id   BIGINT UNSIGNED;
    DECLARE v_open_time  TIME;
    DECLARE v_close_time TIME;
    DECLARE v_day        DATE;
    DECLARE v_slot_start DATETIME;
    DECLARE v_done       INT DEFAULT 0;
    DECLARE v_before     BIGINT DEFAULT 0;
    DECLARE v_after      BIGINT DEFAULT 0;

    DECLARE cur_court CURSOR FOR
        SELECT c.court_id, v.opening_time, v.closing_time
        FROM court c
                 JOIN venue v ON v.venue_id = c.venue_id
        WHERE c.operational_status <> 'DISABLED'
          AND v.status = 'ACTIVE';

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    IF p_from_date IS NULL OR p_to_date IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '生成时间段失败：日期不能为空';
    END IF;
    IF p_to_date < p_from_date THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '生成时间段失败：结束日期不能早于开始日期';
    END IF;
    IF DATEDIFF(p_to_date, p_from_date) > 365 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '生成时间段失败：单次最多生成365天';
    END IF;

    SELECT COUNT(*) INTO v_before FROM court_slot;

    OPEN cur_court;
    read_court: LOOP
        FETCH cur_court INTO v_court_id, v_open_time, v_close_time;
        IF v_done = 1 THEN
            LEAVE read_court;
        END IF;

        SET v_day = p_from_date;
        WHILE v_day <= p_to_date DO
            SET v_slot_start = TIMESTAMP(v_day, v_open_time);
            WHILE DATE_ADD(v_slot_start, INTERVAL 30 MINUTE) <= TIMESTAMP(v_day, v_close_time) DO
                INSERT IGNORE INTO court_slot (court_id, slot_start, slot_end, slot_status)
                VALUES (v_court_id, v_slot_start, DATE_ADD(v_slot_start, INTERVAL 30 MINUTE), 'AVAILABLE');
                SET v_slot_start = DATE_ADD(v_slot_start, INTERVAL 30 MINUTE);
            END WHILE;
            SET v_day = DATE_ADD(v_day, INTERVAL 1 DAY);
        END WHILE;
    END LOOP;
    CLOSE cur_court;

    SELECT COUNT(*) INTO v_after FROM court_slot;

    SELECT p_from_date AS from_date,
           p_to_date   AS to_date,
           (v_after - v_before) AS inserted_slots,
           v_after              AS total_slots;
END$$

-- -------------------------------------------------------------
-- 存储过程 2：单一场地某日实际使用统计
-- 数据来源：court_usage_record（不是 booking.duration_minutes）
-- 跨自然日：按区间重叠分钟数折算时长与金额，避免重复计算。
-- -------------------------------------------------------------
CREATE PROCEDURE sp_daily_court_stats_by_court(
    IN p_stat_date DATE,
    IN p_court_id  BIGINT UNSIGNED
)
BEGIN
    DECLARE v_day_start DATETIME;
    DECLARE v_day_end   DATETIME;

    IF p_stat_date IS NULL OR p_court_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '统计失败：日期与场地ID不能为空';
    END IF;

    SET v_day_start = TIMESTAMP(p_stat_date, '00:00:00');
    SET v_day_end   = DATE_ADD(v_day_start, INTERVAL 1 DAY);

    SELECT p_stat_date AS stat_date,
           c.court_id,
           c.court_name,
           COALESCE(SUM(x.overlap_minutes), 0)                    AS total_actual_minutes,
           ROUND(COALESCE(SUM(x.overlap_minutes), 0) / 60, 2)     AS total_actual_hours,
           ROUND(COALESCE(SUM(x.overlap_fee), 0), 2)              AS total_sales_amount,
           COUNT(x.usage_id)                                      AS usage_record_count
    FROM court c
             LEFT JOIN (
        SELECT u.usage_id,
               u.court_id,
               TIMESTAMPDIFF(
                       MINUTE,
                       GREATEST(u.actual_start_at, v_day_start),
                       LEAST(u.actual_end_at, v_day_end)
                   ) AS overlap_minutes,
               u.actual_fee_amount * TIMESTAMPDIFF(
                       MINUTE,
                       GREATEST(u.actual_start_at, v_day_start),
                       LEAST(u.actual_end_at, v_day_end)
                                     ) / NULLIF(u.actual_minutes, 0) AS overlap_fee
        FROM court_usage_record u
        WHERE u.settlement_status <> 'VOID'
          AND u.actual_start_at < v_day_end
          AND u.actual_end_at > v_day_start
    ) x ON x.court_id = c.court_id
    WHERE c.court_id = p_court_id
    GROUP BY c.court_id, c.court_name;
END$$

-- -------------------------------------------------------------
-- 存储过程 3：全部场地某日汇总统计
-- 无记录时返回 0（而不是 NULL）。
-- -------------------------------------------------------------
CREATE PROCEDURE sp_daily_court_stats_all(
    IN p_stat_date DATE
)
BEGIN
    DECLARE v_day_start DATETIME;
    DECLARE v_day_end   DATETIME;

    IF p_stat_date IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '统计失败：日期不能为空';
    END IF;

    SET v_day_start = TIMESTAMP(p_stat_date, '00:00:00');
    SET v_day_end   = DATE_ADD(v_day_start, INTERVAL 1 DAY);

    SELECT p_stat_date AS stat_date,
           COALESCE(SUM(x.overlap_minutes), 0)                AS total_actual_minutes,
           ROUND(COALESCE(SUM(x.overlap_minutes), 0) / 60, 2) AS total_actual_hours,
           ROUND(COALESCE(SUM(x.overlap_fee), 0), 2)          AS total_sales_amount,
           COUNT(DISTINCT CASE WHEN x.overlap_minutes > 0 THEN x.court_id END) AS used_court_count,
           COUNT(x.usage_id)                                  AS usage_record_count
    FROM (
             SELECT u.usage_id,
                    u.court_id,
                    TIMESTAMPDIFF(
                            MINUTE,
                            GREATEST(u.actual_start_at, v_day_start),
                            LEAST(u.actual_end_at, v_day_end)
                        ) AS overlap_minutes,
                    u.actual_fee_amount * TIMESTAMPDIFF(
                            MINUTE,
                            GREATEST(u.actual_start_at, v_day_start),
                            LEAST(u.actual_end_at, v_day_end)
                                          ) / NULLIF(u.actual_minutes, 0) AS overlap_fee
             FROM court_usage_record u
             WHERE u.settlement_status <> 'VOID'
               AND u.actual_start_at < v_day_end
               AND u.actual_end_at > v_day_start
         ) x;
END$$

DELIMITER ;
