-- =============================================================
-- 羽毛球场地管理系统 - 04 触发器
-- 核心职责：预约单与 court_slot 时间段状态保持一致。
-- 说明：
--   1. 触发器只更新 court_slot，不回写 booking，避免 MySQL
--      "Can't update table ... already used by statement" 限制。
--   2. 时间段必须由初始化程序（sp_generate_court_slots）预先创建。
--   3. 并发控制由后端事务 SELECT ... FOR UPDATE + 触发器校验双重保证。
-- =============================================================
USE badminton_db;
SET NAMES utf8mb4;

DROP TRIGGER IF EXISTS trg_booking_before_update;
DROP TRIGGER IF EXISTS trg_booking_after_insert;
DROP TRIGGER IF EXISTS trg_booking_after_update;

DELIMITER $$

-- -------------------------------------------------------------
-- 触发器 0：禁止修改预约的场地与时间（保证时间段与订单一致）
-- -------------------------------------------------------------
CREATE TRIGGER trg_booking_before_update
BEFORE UPDATE ON booking
FOR EACH ROW
BEGIN
    IF NEW.court_id <> OLD.court_id
       OR NEW.start_at <> OLD.start_at
       OR NEW.end_at <> OLD.end_at
       OR NEW.duration_minutes <> OLD.duration_minutes THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '不允许修改预约的场地或时间，请取消后重新预约';
    END IF;
END$$

-- -------------------------------------------------------------
-- 触发器 1：预约创建成功后占用对应 30 分钟时间段
-- 仅当 status = 'CONFIRMED' 时生效（待支付订单不占场）。
-- -------------------------------------------------------------
CREATE TRIGGER trg_booking_after_insert
AFTER INSERT ON booking
FOR EACH ROW
BEGIN
    DECLARE v_needed    INT DEFAULT 0;
    DECLARE v_available INT DEFAULT 0;
    DECLARE v_updated   INT DEFAULT 0;
    DECLARE v_court_status VARCHAR(20);

    IF NEW.status = 'CONFIRMED' THEN
        -- 时间必须对齐到 30 分钟
        IF MINUTE(NEW.start_at) NOT IN (0, 30) OR SECOND(NEW.start_at) <> 0
           OR MINUTE(NEW.end_at) NOT IN (0, 30) OR SECOND(NEW.end_at) <> 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '预约失败：预约时间必须对齐到30分钟';
        END IF;

        -- 场地必须处于开放状态
        SELECT operational_status INTO v_court_status
        FROM court WHERE court_id = NEW.court_id;
        IF v_court_status <> 'OPEN' THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '预约失败：场地当前不可预约（维护中或已停用）';
        END IF;

        SET v_needed = NEW.duration_minutes DIV 30;

        -- 区间内所有时间段必须存在且空闲
        SELECT COUNT(*) INTO v_available
        FROM court_slot
        WHERE court_id = NEW.court_id
          AND slot_start >= NEW.start_at
          AND slot_end <= NEW.end_at
          AND slot_status = 'AVAILABLE'
          AND booking_id IS NULL;

        IF v_available <> v_needed THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '预约失败：所选时间段已被占用或尚未初始化';
        END IF;

        UPDATE court_slot
        SET slot_status = 'BOOKED',
            booking_id  = NEW.booking_id
        WHERE court_id = NEW.court_id
          AND slot_start >= NEW.start_at
          AND slot_end <= NEW.end_at
          AND slot_status = 'AVAILABLE';

        SET v_updated = ROW_COUNT();
        IF v_updated <> v_needed THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '预约失败：时间段占用数量不一致，事务已回滚';
        END IF;
    END IF;
END$$

-- -------------------------------------------------------------
-- 触发器 2：预约状态变化后同步时间段状态
--   PENDING_PAYMENT -> CONFIRMED : 占用时间段
--   *               -> IN_PROGRESS: 标记使用中
--   *               -> CANCELLED  : 仅释放本订单占用的时间段
--   *               -> NO_SHOW    : 释放本订单占用的时间段
--   *               -> COMPLETED  : 保留 BOOKED 作为历史占用记录
-- -------------------------------------------------------------
CREATE TRIGGER trg_booking_after_update
AFTER UPDATE ON booking
FOR EACH ROW
BEGIN
    DECLARE v_needed    INT DEFAULT 0;
    DECLARE v_available INT DEFAULT 0;
    DECLARE v_owned     INT DEFAULT 0;

    IF NEW.status <> OLD.status THEN
        IF NEW.status = 'CONFIRMED' THEN
            -- 该订单是否已经占用时间段（重复置为 CONFIRMED 时幂等）
            SELECT COUNT(*) INTO v_owned
            FROM court_slot
            WHERE booking_id = NEW.booking_id
              AND slot_start >= NEW.start_at
              AND slot_end <= NEW.end_at;

            SET v_needed = NEW.duration_minutes DIV 30;

            IF v_owned = 0 THEN
                SELECT COUNT(*) INTO v_available
                FROM court_slot
                WHERE court_id = NEW.court_id
                  AND slot_start >= NEW.start_at
                  AND slot_end <= NEW.end_at
                  AND slot_status = 'AVAILABLE'
                  AND booking_id IS NULL;

                IF v_available <> v_needed THEN
                    SIGNAL SQLSTATE '45000'
                        SET MESSAGE_TEXT = '确认预约失败：时间段已被占用，支付未完成';
                END IF;

                UPDATE court_slot
                SET slot_status = 'BOOKED',
                    booking_id  = NEW.booking_id
                WHERE court_id = NEW.court_id
                  AND slot_start >= NEW.start_at
                  AND slot_end <= NEW.end_at
                  AND slot_status = 'AVAILABLE';
            END IF;

        ELSEIF NEW.status = 'IN_PROGRESS' THEN
            UPDATE court_slot
            SET slot_status = 'IN_USE'
            WHERE booking_id = NEW.booking_id
              AND slot_status IN ('BOOKED', 'IN_USE');

        ELSEIF NEW.status = 'CANCELLED' THEN
            UPDATE court_slot
            SET slot_status = 'AVAILABLE',
                booking_id  = NULL
            WHERE booking_id = NEW.booking_id
              AND slot_status IN ('BOOKED', 'IN_USE');

        ELSEIF NEW.status = 'NO_SHOW' THEN
            UPDATE court_slot
            SET slot_status = 'AVAILABLE',
                booking_id  = NULL
            WHERE booking_id = NEW.booking_id
              AND slot_status IN ('BOOKED', 'IN_USE');
        END IF;
        -- COMPLETED：保留 BOOKED，作为历史占用记录
    END IF;
END$$

DELIMITER ;
