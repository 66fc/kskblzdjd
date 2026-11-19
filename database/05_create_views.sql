-- =============================================================
-- 羽毛球场地管理系统 - 05 视图
-- =============================================================
USE badminton_db;

-- -------------------------------------------------------------
-- 视图 1：可预约场地时间段（核心验收项）
-- 只返回：场馆正常 + 场地开放 + 时间段空闲 的记录。
-- 视图不带时间参数，调用方通过 WHERE 指定区间，并用
-- GROUP BY ... HAVING 保证"整个区间全部空闲"。
-- -------------------------------------------------------------
CREATE OR REPLACE VIEW v_available_court_slots AS
SELECT v.venue_id,
       v.venue_name,
       c.court_id,
       c.court_name,
       c.court_number,
       c.hourly_rate,
       s.slot_id,
       s.slot_start,
       s.slot_end,
       s.slot_status
FROM court_slot s
         JOIN court c ON c.court_id = s.court_id
         JOIN venue v ON v.venue_id = c.venue_id
WHERE s.slot_status = 'AVAILABLE'
  AND c.operational_status = 'OPEN'
  AND v.status = 'ACTIVE';

-- -------------------------------------------------------------
-- 视图 2：预约详情（后台列表/详情使用，聚合会员、场地、实际使用）
-- -------------------------------------------------------------
CREATE OR REPLACE VIEW v_booking_detail AS
SELECT b.booking_id,
       b.booking_no,
       b.member_id,
       m.nickname        AS member_nickname,
       m.phone           AS member_phone,
       ml.level_name     AS member_level_name,
       b.court_id,
       c.court_name,
       c.court_number,
       v.venue_id,
       v.venue_name,
       b.start_at,
       b.end_at,
       b.duration_minutes,
       b.hourly_rate_snapshot,
       b.discount_snapshot,
       b.original_amount,
       b.payable_amount,
       b.paid_amount,
       b.status,
       b.actual_start_at,
       b.actual_end_at,
       b.cancel_reason,
       b.created_at,
       b.cancelled_at,
       u.usage_id,
       u.actual_minutes,
       u.billed_minutes,
       u.actual_fee_amount,
       u.settlement_status
FROM booking b
         JOIN member m ON m.member_id = b.member_id
         JOIN member_level ml ON ml.level_id = m.level_id
         JOIN court c ON c.court_id = b.court_id
         JOIN venue v ON v.venue_id = c.venue_id
         LEFT JOIN court_usage_record u ON u.booking_id = b.booking_id;

-- -------------------------------------------------------------
-- 视图 3：会员消费历史（场地租赁 + 商品零售，两类收入分列）
-- -------------------------------------------------------------
CREATE OR REPLACE VIEW v_member_consumption_history AS
SELECT b.member_id,
       'COURT_RENTAL'          AS consume_type,
       b.booking_no            AS ref_no,
       c.court_name            AS description,
       u.actual_fee_amount     AS amount,
       u.actual_end_at         AS consume_at
FROM court_usage_record u
         JOIN booking b ON b.booking_id = u.booking_id
         JOIN court c ON c.court_id = u.court_id
WHERE u.settlement_status = 'SETTLED'
UNION ALL
SELECT so.member_id,
       'PRODUCT'               AS consume_type,
       so.order_no             AS ref_no,
       '商品零售'               AS description,
       so.total_amount         AS amount,
       so.paid_at              AS consume_at
FROM sales_order so
WHERE so.payment_status = 'PAID'
  AND so.member_id IS NOT NULL;
