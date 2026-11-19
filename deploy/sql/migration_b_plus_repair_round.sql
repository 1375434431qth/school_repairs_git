-- =============================================================================
-- 方案 B+ 数据迁移脚本：单一工单 + 返修轮次
-- 适用环境：MySQL 8.0+（当前 8.0.27）
-- 执行时机：后端代码切换到 B+ 前执行；DDL 会隐式提交，请在维护窗口操作
-- 执行前务必备份：mysqldump -uroot -p school_repair > school_repair_bak.sql
-- 说明：脚本为一次性迁移，不保证可重复执行；若中途失败，按报错点续跑即可
-- =============================================================================

USE school_repair;

-- -----------------------------------------------------------------------------
-- 1. 工单主表：根工单 + 当前轮次
-- -----------------------------------------------------------------------------
ALTER TABLE t_work_order
  ADD COLUMN root_order_id BIGINT DEFAULT NULL COMMENT '根工单id（首单=自身；历史返修链回溯到首单）' AFTER id,
  ADD COLUMN current_round INT NOT NULL DEFAULT 1 COMMENT '当前维修轮次，从 1 开始' AFTER status,
  ADD KEY idx_root (root_order_id);

-- 状态枚举说明追加 REWORKING（字段类型不变，仅改注释）
ALTER TABLE t_work_order
  MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'CREATED'
  COMMENT 'CREATED/ASSIGNED/ACCEPTED/ARRIVED/FINISHED/REWORKING/CONFIRMED/CLOSED/CANCELLED';

-- -----------------------------------------------------------------------------
-- 2. 维修轮次表：每一轮维修一条记录
-- -----------------------------------------------------------------------------
CREATE TABLE t_repair_round (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  order_id      BIGINT       NOT NULL COMMENT '所属工单（root 单）',
  round_no      INT          NOT NULL DEFAULT 1 COMMENT '轮次，从 1 开始',
  repairer_id   BIGINT       DEFAULT NULL COMMENT '本轮维修工',
  status        VARCHAR(20)  NOT NULL DEFAULT 'ASSIGNED'
                COMMENT 'ASSIGNED/ACCEPTED/ARRIVED/FINISHED',
  dispatch_type VARCHAR(16)  DEFAULT NULL COMMENT 'ASSIGN/GRAB/REASSIGN',
  assign_time   DATETIME     DEFAULT NULL,
  accept_time   DATETIME     DEFAULT NULL,
  arrive_time   DATETIME     DEFAULT NULL,
  finish_time   DATETIME     DEFAULT NULL,
  finish_desc   VARCHAR(500) DEFAULT NULL COMMENT '本轮完工说明',
  rework_reason VARCHAR(200) DEFAULT NULL COMMENT '本轮进入返修的原因（round_no>1）',
  timeout_flag  TINYINT      NOT NULL DEFAULT 0,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_round (order_id, round_no),
  KEY idx_repairer (repairer_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维修轮次';

-- -----------------------------------------------------------------------------
-- 3. 验收记录表：学生每轮验收结论（通过/驳回）
-- -----------------------------------------------------------------------------
CREATE TABLE t_order_acceptance (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  order_id    BIGINT       NOT NULL,
  round_no    INT          NOT NULL COMMENT '被验收的轮次',
  student_id  BIGINT       NOT NULL,
  result      VARCHAR(10)  NOT NULL COMMENT 'PASS通过/REJECT驳回',
  reason      VARCHAR(200) DEFAULT NULL COMMENT '驳回原因（REJECT 必填）',
  deleted     TINYINT      NOT NULL DEFAULT 0,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_round (order_id, round_no),
  KEY idx_result (result, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单验收记录';

-- -----------------------------------------------------------------------------
-- 4. 既有业务表补充轮次维度
-- -----------------------------------------------------------------------------
ALTER TABLE t_order_image
  ADD COLUMN round_no INT NOT NULL DEFAULT 1 COMMENT '所属维修轮次' AFTER order_id,
  ADD KEY idx_order_round (order_id, round_no, img_type);

ALTER TABLE t_repair_record
  ADD COLUMN round_no INT NOT NULL DEFAULT 1 COMMENT '所属维修轮次' AFTER order_id,
  DROP INDEX uk_order,
  ADD UNIQUE KEY uk_order_round (order_id, round_no);

ALTER TABLE t_part_usage
  ADD COLUMN round_no INT NOT NULL DEFAULT 1 COMMENT '所属维修轮次' AFTER order_id,
  ADD KEY idx_order_round (order_id, round_no);

ALTER TABLE t_dispatch_record
  ADD COLUMN round_no INT NOT NULL DEFAULT 1 COMMENT '所属维修轮次' AFTER order_id;

ALTER TABLE t_timeout_record
  ADD COLUMN round_no INT NOT NULL DEFAULT 1 COMMENT '所属维修轮次' AFTER order_id,
  DROP INDEX uk_order_type,
  ADD UNIQUE KEY uk_order_round_type (order_id, round_no, timeout_type);

ALTER TABLE t_evaluation
  ADD COLUMN final_round_no INT NOT NULL DEFAULT 1 COMMENT '评价时最终轮次' AFTER order_id;

-- -----------------------------------------------------------------------------
-- 5. 历史数据回填：root_order_id / current_round
--    兼容原方案 A 的 source_order_id 链（A→B→C），统一回溯到首单
-- -----------------------------------------------------------------------------
UPDATE t_work_order SET root_order_id = id WHERE root_order_id IS NULL;

CREATE TEMPORARY TABLE tmp_root_chain (
  id      BIGINT NOT NULL PRIMARY KEY,
  root_id BIGINT NOT NULL,
  depth   INT    NOT NULL
);

INSERT INTO tmp_root_chain (id, root_id, depth)
WITH RECURSIVE chain AS (
  SELECT w.id AS id, w.id AS root_id, 1 AS depth
  FROM t_work_order w
  WHERE w.source_order_id IS NULL
  UNION ALL
  SELECT c.id AS id, p.root_id AS root_id, p.depth + 1 AS depth
  FROM t_work_order c
  JOIN chain p ON c.source_order_id = p.id
)
SELECT id, root_id, depth FROM chain;

UPDATE t_work_order w
JOIN tmp_root_chain t ON w.id = t.id
SET w.root_order_id = t.root_id;

UPDATE t_work_order w
JOIN (
  SELECT root_id, MAX(depth) AS max_depth
  FROM tmp_root_chain
  GROUP BY root_id
) m ON w.id = m.root_id
SET w.current_round = m.max_depth;

DROP TEMPORARY TABLE tmp_root_chain;

-- -----------------------------------------------------------------------------
-- 6. 历史数据回填：每张历史工单生成第 1 轮记录
-- -----------------------------------------------------------------------------
INSERT INTO t_repair_round
  (order_id, round_no, repairer_id, status, dispatch_type,
   assign_time, accept_time, arrive_time, finish_time, finish_desc,
   timeout_flag, create_time, update_time)
SELECT
  o.id,
  1,
  o.assignee_id,
  CASE
    WHEN o.status = 'ARRIVED' THEN 'ARRIVED'
    WHEN o.status IN ('FINISHED', 'CONFIRMED', 'CLOSED') THEN 'FINISHED'
    WHEN o.status = 'ACCEPTED' THEN 'ACCEPTED'
    ELSE 'ASSIGNED'
  END,
  o.dispatch_type,
  o.assign_time,
  o.accept_time,
  o.arrive_time,
  o.finish_time,
  (SELECT r.result_desc FROM t_repair_record r
    WHERE r.order_id = o.id AND r.deleted = 0 ORDER BY r.id DESC LIMIT 1),
  o.timeout_flag,
  o.create_time,
  o.update_time
FROM t_work_order o
WHERE o.deleted = 0;

-- -----------------------------------------------------------------------------
-- 7. 历史数据回填：验收记录
--    有后继返修单的原单 → 第 1 轮 REJECT（迁移标记）
--    已确认/已完成且无后继返修单 → 第 1 轮 PASS
-- -----------------------------------------------------------------------------
INSERT INTO t_order_acceptance
  (order_id, round_no, student_id, result, reason, create_time, update_time)
SELECT o.id, 1, o.student_id, 'REJECT', '历史返修数据迁移',
       COALESCE(o.confirm_time, o.update_time), o.update_time
FROM t_work_order o
WHERE o.deleted = 0
  AND EXISTS (SELECT 1 FROM t_work_order c
              WHERE c.source_order_id = o.id AND c.deleted = 0);

INSERT INTO t_order_acceptance
  (order_id, round_no, student_id, result, reason, create_time, update_time)
SELECT o.id, 1, o.student_id, 'PASS', NULL,
       COALESCE(o.confirm_time, o.update_time), o.update_time
FROM t_work_order o
WHERE o.deleted = 0
  AND o.status IN ('CONFIRMED', 'CLOSED')
  AND NOT EXISTS (SELECT 1 FROM t_work_order c
                  WHERE c.source_order_id = o.id AND c.deleted = 0);

-- -----------------------------------------------------------------------------
-- 8. 校验（执行后人工核对，预期均为 0 或符合预期）
-- -----------------------------------------------------------------------------
-- 8.1 未回填根单的工单数（预期 0）
SELECT COUNT(*) AS root_null_cnt FROM t_work_order WHERE root_order_id IS NULL;

-- 8.2 根单轮次与链深度是否一致（预期 0）
SELECT COUNT(*) AS round_mismatch_cnt
FROM t_work_order o
JOIN (
  SELECT root_order_id, COUNT(*) AS cnt
  FROM t_work_order
  WHERE deleted = 0
  GROUP BY root_order_id
) x ON o.id = x.root_order_id
WHERE o.current_round <> x.cnt;

-- 8.3 每张工单是否都有第 1 轮（预期 0）
SELECT COUNT(*) AS round_missing_cnt
FROM t_work_order o
LEFT JOIN t_repair_round r ON r.order_id = o.id AND r.round_no = 1
WHERE o.deleted = 0 AND r.id IS NULL;

-- 8.4 历史返修链根单统计
SELECT w.root_order_id, w.order_no AS root_order_no, w.current_round,
       COUNT(c.id) AS legacy_rework_orders
FROM t_work_order w
LEFT JOIN t_work_order c ON c.root_order_id = w.id AND c.id <> w.id AND c.deleted = 0
WHERE w.id = w.root_order_id AND w.deleted = 0
GROUP BY w.id, w.order_no, w.current_round
HAVING legacy_rework_orders > 0;

-- =============================================================================
-- 回滚提示（仅开发环境参考，生产请使用备份恢复）
--   DROP TABLE t_order_acceptance, t_repair_round;
--   ALTER TABLE t_work_order DROP COLUMN root_order_id, DROP COLUMN current_round, DROP KEY idx_root;
--   ALTER TABLE t_order_image DROP COLUMN round_no, DROP KEY idx_order_round;
--   ALTER TABLE t_repair_record DROP INDEX uk_order_round, ADD UNIQUE KEY uk_order (order_id), DROP COLUMN round_no;
--   ALTER TABLE t_part_usage DROP COLUMN round_no, DROP KEY idx_order_round;
--   ALTER TABLE t_dispatch_record DROP COLUMN round_no;
--   ALTER TABLE t_timeout_record DROP INDEX uk_order_round_type, ADD UNIQUE KEY uk_order_type (order_id, timeout_type), DROP COLUMN round_no;
--   ALTER TABLE t_evaluation DROP COLUMN final_round_no;
-- =============================================================================
