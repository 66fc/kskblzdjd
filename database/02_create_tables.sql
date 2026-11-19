-- =============================================================
-- 羽毛球场地管理系统 - 02 创建数据表
-- 说明：
--   1. 所有金额使用 DECIMAL，禁止 FLOAT/DOUBLE。
--   2. 场地"运营状态"与"时间段占用状态"分离：
--      court.operational_status 表示场地是否可营业；
--      court_slot.slot_status    表示某天某 30 分钟段是否被占用。
--   3. 时间统一使用 DATETIME，业务"当前时间"由 system_setting 模拟时钟提供。
-- =============================================================
USE badminton_db;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS admin_operation_log;
DROP TABLE IF EXISTS sales_order_item;
DROP TABLE IF EXISTS sales_order;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS court_usage_record;
DROP TABLE IF EXISTS court_slot;
DROP TABLE IF EXISTS booking;
DROP TABLE IF EXISTS member_level_change_log;
DROP TABLE IF EXISTS member;
DROP TABLE IF EXISTS member_level;
DROP TABLE IF EXISTS court;
DROP TABLE IF EXISTS venue;
DROP TABLE IF EXISTS system_setting;

SET FOREIGN_KEY_CHECKS = 1;

-- -------------------------------------------------------------
-- 1. 场馆表
-- -------------------------------------------------------------
CREATE TABLE venue (
    venue_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '场馆ID',
    venue_name    VARCHAR(100)    NOT NULL                COMMENT '场馆名称',
    address       VARCHAR(255)    NOT NULL                COMMENT '场馆地址',
    phone         VARCHAR(30)     NOT NULL                COMMENT '联系电话',
    opening_time  TIME            NOT NULL DEFAULT '08:00:00' COMMENT '营业开始时间',
    closing_time  TIME            NOT NULL DEFAULT '22:00:00' COMMENT '营业结束时间',
    status        ENUM('ACTIVE','SUSPENDED','CLOSED') NOT NULL DEFAULT 'ACTIVE' COMMENT '场馆状态',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (venue_id),
    UNIQUE KEY uk_venue_name (venue_name),
    CONSTRAINT ck_venue_time CHECK (closing_time > opening_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '场馆表';

-- -------------------------------------------------------------
-- 2. 场地表
-- -------------------------------------------------------------
CREATE TABLE court (
    court_id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '场地ID',
    venue_id           BIGINT UNSIGNED NOT NULL                COMMENT '所属场馆ID',
    court_name         VARCHAR(50)     NOT NULL                COMMENT '场地名称',
    court_number       VARCHAR(20)     NOT NULL                COMMENT '场地编号',
    hourly_rate        DECIMAL(10,2)   NOT NULL                COMMENT '每小时单价(元/小时)',
    operational_status ENUM('OPEN','MAINTENANCE','DISABLED') NOT NULL DEFAULT 'OPEN' COMMENT '运营状态：开放/维护/停用',
    description        VARCHAR(255)    NULL                    COMMENT '场地介绍',
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (court_id),
    UNIQUE KEY uk_court_number (venue_id, court_number),
    KEY idx_court_status (operational_status),
    CONSTRAINT fk_court_venue FOREIGN KEY (venue_id) REFERENCES venue (venue_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_court_rate CHECK (hourly_rate >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '场地表';

-- -------------------------------------------------------------
-- 3. 会员等级表
-- -------------------------------------------------------------
CREATE TABLE member_level (
    level_id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '等级ID',
    level_name          VARCHAR(30)     NOT NULL                COMMENT '等级名称',
    level_rank          INT             NOT NULL                COMMENT '等级序号(越大越高)',
    discount_rate       DECIMAL(4,2)    NOT NULL DEFAULT 1.00   COMMENT '折扣比例(0.85=85折)',
    minimum_consumption DECIMAL(10,2)   NOT NULL DEFAULT 0.00   COMMENT '自动升级消费门槛',
    description         VARCHAR(255)    NULL                    COMMENT '等级介绍',
    status              ENUM('ENABLED','DISABLED') NOT NULL DEFAULT 'ENABLED' COMMENT '是否启用',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (level_id),
    UNIQUE KEY uk_level_name (level_name),
    UNIQUE KEY uk_level_rank (level_rank),
    CONSTRAINT ck_level_discount CHECK (discount_rate > 0 AND discount_rate <= 1),
    CONSTRAINT ck_level_min CHECK (minimum_consumption >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会员等级表';

-- -------------------------------------------------------------
-- 4. 用户表（含管理员与普通会员）
-- -------------------------------------------------------------
CREATE TABLE member (
    member_id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会员ID',
    username               VARCHAR(50)     NOT NULL                COMMENT '登录用户名',
    password_hash          VARCHAR(100)    NOT NULL                COMMENT 'BCrypt 加密密码',
    nickname               VARCHAR(50)     NOT NULL                COMMENT '昵称',
    phone                  VARCHAR(20)     NOT NULL                COMMENT '联系电话',
    avatar                 VARCHAR(255)    NULL                    COMMENT '头像地址',
    role                   ENUM('ADMIN','MEMBER') NOT NULL DEFAULT 'MEMBER' COMMENT '角色',
    level_id               BIGINT UNSIGNED NOT NULL                COMMENT '会员等级ID',
    total_consumption      DECIMAL(12,2)   NOT NULL DEFAULT 0.00   COMMENT '累计消费金额',
    status                 ENUM('ACTIVE','DISABLED') NOT NULL DEFAULT 'ACTIVE' COMMENT '账号状态',
    level_manually_adjusted TINYINT(1)     NOT NULL DEFAULT 0      COMMENT '等级是否被管理员手动指定(1=自动升级不覆盖)',
    manual_adjusted_at     DATETIME        NULL                    COMMENT '最近一次人工调级时间',
    manual_adjusted_by     BIGINT UNSIGNED NULL                    COMMENT '最近一次人工调级管理员',
    created_at             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    updated_at             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (member_id),
    UNIQUE KEY uk_member_username (username),
    UNIQUE KEY uk_member_phone (phone),
    KEY idx_member_level (level_id),
    KEY idx_member_status (status),
    CONSTRAINT fk_member_level FOREIGN KEY (level_id) REFERENCES member_level (level_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_member_manual_by FOREIGN KEY (manual_adjusted_by) REFERENCES member (member_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_member_consumption CHECK (total_consumption >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户/会员表';

-- -------------------------------------------------------------
-- 5. 会员等级变更日志表
-- -------------------------------------------------------------
CREATE TABLE member_level_change_log (
    change_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '变更ID',
    member_id    BIGINT UNSIGNED NOT NULL                COMMENT '被修改会员',
    old_level_id BIGINT UNSIGNED NULL                    COMMENT '修改前等级',
    new_level_id BIGINT UNSIGNED NOT NULL                COMMENT '修改后等级',
    operator_id  BIGINT UNSIGNED NULL                    COMMENT '操作管理员(自动升级为空)',
    change_type  ENUM('MANUAL','AUTO') NOT NULL DEFAULT 'MANUAL' COMMENT '变更类型',
    reason       VARCHAR(255)    NOT NULL                COMMENT '修改原因',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (change_id),
    KEY idx_level_log_member (member_id, created_at),
    CONSTRAINT fk_level_log_member FOREIGN KEY (member_id) REFERENCES member (member_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_level_log_old FOREIGN KEY (old_level_id) REFERENCES member_level (level_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_level_log_new FOREIGN KEY (new_level_id) REFERENCES member_level (level_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_level_log_operator FOREIGN KEY (operator_id) REFERENCES member (member_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会员等级变更日志表';

-- -------------------------------------------------------------
-- 6. 预约表
-- -------------------------------------------------------------
CREATE TABLE booking (
    booking_id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '预约ID',
    booking_no           VARCHAR(32)     NOT NULL                COMMENT '订单编号',
    request_id           VARCHAR(64)     NULL                    COMMENT '客户端幂等请求ID',
    member_id            BIGINT UNSIGNED NOT NULL                COMMENT '预约会员',
    court_id             BIGINT UNSIGNED NOT NULL                COMMENT '预约场地',
    start_at             DATETIME        NOT NULL                COMMENT '预约开始时间',
    end_at               DATETIME        NOT NULL                COMMENT '预约结束时间',
    duration_minutes     INT             NOT NULL                COMMENT '预约时长(分钟)',
    hourly_rate_snapshot DECIMAL(10,2)   NOT NULL                COMMENT '预约时场地单价快照',
    discount_snapshot    DECIMAL(4,2)    NOT NULL                COMMENT '预约时会员折扣快照',
    original_amount      DECIMAL(10,2)   NOT NULL                COMMENT '原价金额',
    payable_amount       DECIMAL(10,2)   NOT NULL                COMMENT '应付金额',
    paid_amount          DECIMAL(10,2)   NOT NULL DEFAULT 0.00   COMMENT '实际支付金额',
    status               ENUM('PENDING_PAYMENT','CONFIRMED','CANCELLED','IN_PROGRESS','COMPLETED','NO_SHOW')
                                         NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT '预约状态',
    actual_start_at      DATETIME        NULL                    COMMENT '实际开始使用时间',
    actual_end_at        DATETIME        NULL                    COMMENT '实际结束使用时间',
    cancel_reason        VARCHAR(255)    NULL                    COMMENT '取消原因',
    created_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    cancelled_at         DATETIME        NULL                    COMMENT '取消时间',
    PRIMARY KEY (booking_id),
    UNIQUE KEY uk_booking_no (booking_no),
    UNIQUE KEY uk_booking_request (request_id),
    KEY idx_booking_member (member_id, start_at),
    KEY idx_booking_court (court_id, start_at),
    KEY idx_booking_status (status),
    KEY idx_booking_created (created_at),
    CONSTRAINT fk_booking_member FOREIGN KEY (member_id) REFERENCES member (member_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_booking_court FOREIGN KEY (court_id) REFERENCES court (court_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_booking_time CHECK (end_at > start_at),
    CONSTRAINT ck_booking_duration CHECK (duration_minutes > 0 AND duration_minutes % 30 = 0),
    CONSTRAINT ck_booking_amount CHECK (payable_amount >= 0 AND paid_amount >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '场地预约表';

-- -------------------------------------------------------------
-- 7. 场地时间段表（核心：以 30 分钟为粒度的占用状态）
-- -------------------------------------------------------------
CREATE TABLE court_slot (
    slot_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '时间段ID',
    court_id    BIGINT UNSIGNED NOT NULL                COMMENT '场地ID',
    slot_start  DATETIME        NOT NULL                COMMENT '时间段开始',
    slot_end    DATETIME        NOT NULL                COMMENT '时间段结束(开始+30分钟)',
    slot_status ENUM('AVAILABLE','BOOKED','IN_USE','CLOSED') NOT NULL DEFAULT 'AVAILABLE' COMMENT '时间段状态',
    booking_id  BIGINT UNSIGNED NULL                    COMMENT '关联预约ID',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (slot_id),
    UNIQUE KEY uk_court_slot_start (court_id, slot_start),
    KEY idx_slot_status_time (slot_status, slot_start),
    KEY idx_slot_booking (booking_id),
    CONSTRAINT fk_slot_court FOREIGN KEY (court_id) REFERENCES court (court_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_slot_booking FOREIGN KEY (booking_id) REFERENCES booking (booking_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '场地时间段表(30分钟粒度)';

-- -------------------------------------------------------------
-- 8. 场地实际使用记录表
-- -------------------------------------------------------------
CREATE TABLE court_usage_record (
    usage_id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '使用记录ID',
    booking_id         BIGINT UNSIGNED NOT NULL                COMMENT '预约订单ID',
    court_id           BIGINT UNSIGNED NOT NULL                COMMENT '场地ID',
    actual_start_at    DATETIME        NOT NULL                COMMENT '实际开始使用时间',
    actual_end_at      DATETIME        NOT NULL                COMMENT '实际结束使用时间',
    actual_minutes     INT             NOT NULL                COMMENT '实际使用分钟数',
    billed_minutes     INT             NOT NULL                COMMENT '计费分钟数(按30分钟向上取整)',
    actual_fee_amount  DECIMAL(10,2)   NOT NULL                COMMENT '实际产生场地费用',
    settlement_status  ENUM('UNSETTLED','SETTLED','VOID') NOT NULL DEFAULT 'SETTLED' COMMENT '结算状态',
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (usage_id),
    UNIQUE KEY uk_usage_booking (booking_id),
    KEY idx_usage_court_time (court_id, actual_start_at),
    KEY idx_usage_range (actual_start_at, actual_end_at),
    CONSTRAINT fk_usage_booking FOREIGN KEY (booking_id) REFERENCES booking (booking_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_usage_court FOREIGN KEY (court_id) REFERENCES court (court_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_usage_time CHECK (actual_end_at > actual_start_at),
    CONSTRAINT ck_usage_minutes CHECK (actual_minutes > 0 AND billed_minutes >= actual_minutes),
    CONSTRAINT ck_usage_fee CHECK (actual_fee_amount >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '场地实际使用记录表';

-- -------------------------------------------------------------
-- 9. 商品表
-- -------------------------------------------------------------
CREATE TABLE product (
    product_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    product_name VARCHAR(100)    NOT NULL                COMMENT '商品名称',
    category     VARCHAR(30)     NOT NULL                COMMENT '分类：BALL/RACKET/DRINK/ACCESSORY/OTHER',
    price        DECIMAL(10,2)   NOT NULL                COMMENT '销售单价',
    stock        INT             NOT NULL DEFAULT 0      COMMENT '库存数量',
    status       ENUM('ON_SALE','OFF_SALE') NOT NULL DEFAULT 'ON_SALE' COMMENT '上架状态',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (product_id),
    UNIQUE KEY uk_product_name (product_name),
    KEY idx_product_category (category),
    CONSTRAINT ck_product_price CHECK (price >= 0),
    CONSTRAINT ck_product_stock CHECK (stock >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品表';

-- -------------------------------------------------------------
-- 10. 销售订单表（商品零售，独立于场地租赁）
-- -------------------------------------------------------------
CREATE TABLE sales_order (
    sales_order_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '销售订单ID',
    order_no       VARCHAR(32)     NOT NULL                COMMENT '销售单号',
    member_id      BIGINT UNSIGNED NULL                    COMMENT '购买会员(可散客)',
    total_amount   DECIMAL(10,2)   NOT NULL DEFAULT 0.00   COMMENT '订单总额',
    payment_status ENUM('UNPAID','PAID','REFUNDED') NOT NULL DEFAULT 'UNPAID' COMMENT '支付状态',
    payment_method ENUM('CASH','WECHAT','ALIPAY','CARD','MIXED') NOT NULL DEFAULT 'CASH' COMMENT '支付方式',
    paid_at        DATETIME        NULL                    COMMENT '支付时间',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (sales_order_id),
    UNIQUE KEY uk_sales_order_no (order_no),
    KEY idx_sales_member (member_id),
    KEY idx_sales_paid (paid_at),
    KEY idx_sales_status (payment_status),
    CONSTRAINT fk_sales_member FOREIGN KEY (member_id) REFERENCES member (member_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_sales_total CHECK (total_amount >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品销售订单表';

-- -------------------------------------------------------------
-- 11. 销售订单明细表
-- -------------------------------------------------------------
CREATE TABLE sales_order_item (
    item_id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    sales_order_id BIGINT UNSIGNED NOT NULL                COMMENT '销售订单ID',
    product_id     BIGINT UNSIGNED NOT NULL                COMMENT '商品ID',
    quantity       INT             NOT NULL                COMMENT '数量',
    unit_price     DECIMAL(10,2)   NOT NULL                COMMENT '成交单价',
    subtotal       DECIMAL(10,2)   NOT NULL                COMMENT '小计',
    PRIMARY KEY (item_id),
    KEY idx_item_order (sales_order_id),
    KEY idx_item_product (product_id),
    CONSTRAINT fk_item_order FOREIGN KEY (sales_order_id) REFERENCES sales_order (sales_order_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_item_product FOREIGN KEY (product_id) REFERENCES product (product_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_item_qty CHECK (quantity > 0),
    CONSTRAINT ck_item_price CHECK (unit_price >= 0),
    CONSTRAINT ck_item_subtotal CHECK (subtotal >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售订单明细表';

-- -------------------------------------------------------------
-- 12. 系统设置 / 模拟时钟（单行配置）
-- -------------------------------------------------------------
CREATE TABLE system_setting (
    setting_id    TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '配置ID(固定为1)',
    simulated_now DATETIME        NOT NULL COMMENT '模拟时间基准点',
    base_real_at  DATETIME        NOT NULL COMMENT '基准点对应的真实时间',
    clock_mode    ENUM('REAL','SIMULATED') NOT NULL DEFAULT 'SIMULATED' COMMENT '时钟模式',
    clock_paused  TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否暂停',
    clock_speed   DECIMAL(5,2)    NOT NULL DEFAULT 1.00 COMMENT '时间流速倍率',
    demo_mode     TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '演示模式',
    updated_by    BIGINT UNSIGNED NULL COMMENT '最后修改管理员',
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (setting_id),
    CONSTRAINT ck_setting_speed CHECK (clock_speed > 0 AND clock_speed <= 100),
    CONSTRAINT fk_setting_user FOREIGN KEY (updated_by) REFERENCES member (member_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统设置与模拟时钟';

-- -------------------------------------------------------------
-- 13. 管理操作日志表
-- -------------------------------------------------------------
CREATE TABLE admin_operation_log (
    log_id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    operator_id   BIGINT UNSIGNED NULL                    COMMENT '操作管理员ID',
    operator_name VARCHAR(50)     NOT NULL                COMMENT '操作管理员名称',
    module        VARCHAR(30)     NOT NULL                COMMENT '功能模块',
    action        VARCHAR(50)     NOT NULL                COMMENT '操作动作',
    target_type   VARCHAR(30)     NULL                    COMMENT '目标类型',
    target_id     VARCHAR(64)     NULL                    COMMENT '目标ID',
    detail        VARCHAR(1000)   NULL                    COMMENT '操作详情',
    ip            VARCHAR(45)     NULL                    COMMENT '来源IP',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (log_id),
    KEY idx_log_operator (operator_id),
    KEY idx_log_time (created_at),
    KEY idx_log_module (module),
    CONSTRAINT fk_log_operator FOREIGN KEY (operator_id) REFERENCES member (member_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理操作日志表';
