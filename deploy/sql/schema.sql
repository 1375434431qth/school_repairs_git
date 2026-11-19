CREATE DATABASE IF NOT EXISTS school_repair DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE school_repair;

-- 1. 用户表
CREATE TABLE t_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username      VARCHAR(32)  NOT NULL COMMENT '登录账号',
  password      VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
  real_name     VARCHAR(32)  NOT NULL COMMENT '姓名',
  phone         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  role          VARCHAR(16)  NOT NULL COMMENT 'STUDENT/REPAIRER/MANAGER',
  building_id   BIGINT       DEFAULT NULL COMMENT '所属楼栋（学生所在楼/维修工负责区域；管理员不看此字段，见 t_manager_building）',
  room_id       BIGINT       DEFAULT NULL COMMENT '学生所在房间',
  avatar        VARCHAR(200) DEFAULT NULL,
  skill_tags    VARCHAR(200) DEFAULT NULL COMMENT '维修工技能标签，逗号分隔',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  KEY idx_role (role),
  KEY idx_building (building_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 宿舍楼
CREATE TABLE t_building (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  name        VARCHAR(50) NOT NULL COMMENT '如 一号宿舍楼',
  code        VARCHAR(20) NOT NULL COMMENT '如 A1',
  area        VARCHAR(50) DEFAULT NULL COMMENT '区域（东区/西区），派单按区域筛选',
  floors      INT         NOT NULL DEFAULT 6,
  remark      VARCHAR(200) DEFAULT NULL,
  deleted     TINYINT     NOT NULL DEFAULT 0,
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code),
  KEY idx_area (area)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宿舍楼';

-- 3. 房间
CREATE TABLE t_room (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  building_id BIGINT      NOT NULL,
  room_no     VARCHAR(20) NOT NULL COMMENT '如 302',
  floor       INT         NOT NULL DEFAULT 1,
  deleted     TINYINT     NOT NULL DEFAULT 0,
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_building_room (building_id, room_no),
  KEY idx_building (building_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间';

-- 4. 故障类型（两级树）
CREATE TABLE t_fault_type (
  id               BIGINT      NOT NULL AUTO_INCREMENT,
  parent_id        BIGINT      NOT NULL DEFAULT 0 COMMENT '0为一级分类',
  name             VARCHAR(50) NOT NULL COMMENT '如 水龙头漏水',
  category         VARCHAR(20) NOT NULL COMMENT 'WATER_ELEC/DOOR_WINDOW/NETWORK/FURNITURE/OTHER',
  default_priority TINYINT     NOT NULL DEFAULT 3 COMMENT '默认优先级 1紧急~4低',
  expect_hours     INT         NOT NULL DEFAULT 24 COMMENT '期望完成时长(小时)，用于超时判定',
  sort             INT         NOT NULL DEFAULT 0,
  status           TINYINT     NOT NULL DEFAULT 1,
  deleted          TINYINT     NOT NULL DEFAULT 0,
  create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故障类型';

-- 5. 工单主表（核心）
CREATE TABLE t_work_order (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  root_order_id    BIGINT       DEFAULT NULL COMMENT '根工单id（首单=自身，返修不新建工单）',
  order_no         VARCHAR(32)  NOT NULL COMMENT '工单号 WR+yyyyMMdd+6位序列',
  student_id       BIGINT       NOT NULL,
  building_id      BIGINT       NOT NULL COMMENT '冗余，便于按楼栋统计与派单',
  room_id          BIGINT       NOT NULL,
  room_no          VARCHAR(20)  NOT NULL COMMENT '冗余快照',
  fault_type_id    BIGINT       NOT NULL,
  fault_type_name  VARCHAR(50)  NOT NULL COMMENT '冗余快照',
  fault_category   VARCHAR(20)  NOT NULL COMMENT '冗余快照，统计按此字段聚合，避免字典改名/停用影响历史',
  description      VARCHAR(500) NOT NULL COMMENT '故障描述',
  contact_phone    VARCHAR(20)  DEFAULT NULL,
  priority         TINYINT      NOT NULL DEFAULT 3 COMMENT '1紧急 2高 3普通 4低',
  status           VARCHAR(20)  NOT NULL DEFAULT 'CREATED'
                   COMMENT 'CREATED/ASSIGNED/ACCEPTED/ARRIVED/FINISHED/REWORKING/CONFIRMED/CLOSED/CANCELLED（拒单不改状态，见 7.4 BR-02）',
  current_round    INT          NOT NULL DEFAULT 1 COMMENT '当前维修轮次，从1开始',
  expect_hours     INT          NOT NULL DEFAULT 24,
  deadline_time    DATETIME     DEFAULT NULL COMMENT '计划完成时间 = create_time + expect_hours',
  assignee_id      BIGINT       DEFAULT NULL COMMENT '维修工',
  dispatch_type    VARCHAR(16)  DEFAULT NULL COMMENT 'ASSIGN派单/GRAB抢单',
  assign_time      DATETIME     DEFAULT NULL,
  accept_time      DATETIME     DEFAULT NULL,
  arrive_time      DATETIME     DEFAULT NULL,
  finish_time      DATETIME     DEFAULT NULL,
  confirm_time     DATETIME     DEFAULT NULL,
  close_time       DATETIME     DEFAULT NULL,
  cancel_time      DATETIME     DEFAULT NULL COMMENT '取消时间',
  timeout_flag     TINYINT      NOT NULL DEFAULT 0 COMMENT '0未超时 1已超时',
  timeout_type     VARCHAR(20)  DEFAULT NULL COMMENT '最近一次超时发生时所处环节 ACCEPT/ARRIVE/FINISH，明细见 t_timeout_record',
  is_rework        TINYINT      NOT NULL DEFAULT 0 COMMENT '1表示返修单',
  source_order_id  BIGINT       DEFAULT NULL COMMENT '返修来源工单',
  evaluate_flag    TINYINT      NOT NULL DEFAULT 0 COMMENT '0未评价 1已评价',
  cancel_reason    VARCHAR(200) DEFAULT NULL,
  version          INT          NOT NULL DEFAULT 0 COMMENT '乐观锁',
  deleted          TINYINT      NOT NULL DEFAULT 0,
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_student (student_id, create_time),
  KEY idx_root (root_order_id),
  KEY idx_status_building (status, building_id),
  KEY idx_assignee (assignee_id, status),
  KEY idx_create_time (create_time),
  KEY idx_finish_time (finish_time),
  KEY idx_rework (is_rework, finish_time),
  KEY idx_category (fault_category),
  KEY idx_timeout (timeout_flag, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单主表';

-- 6. 工单图片（只存 GridFS 引用）
CREATE TABLE t_order_image (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  order_id    BIGINT      NOT NULL,
  round_no    INT         NOT NULL DEFAULT 1 COMMENT '所属维修轮次',
  img_type    VARCHAR(10) NOT NULL COMMENT 'BEFORE报修前 AFTER完工后（AVATAR 不写本表）',
  file_id     VARCHAR(64) NOT NULL COMMENT 'MongoDB GridFS ObjectId',
  file_name   VARCHAR(100) DEFAULT NULL,
  file_size   BIGINT      DEFAULT NULL,
  sort        INT         NOT NULL DEFAULT 0,
  deleted     TINYINT     NOT NULL DEFAULT 0,
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_order (order_id, img_type),
  KEY idx_order_round (order_id, round_no, img_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单图片';

-- 6.1 维修轮次（每一轮维修一条）
CREATE TABLE t_repair_round (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  order_id      BIGINT       NOT NULL,
  round_no      INT          NOT NULL DEFAULT 1,
  repairer_id   BIGINT       DEFAULT NULL,
  status        VARCHAR(20)  NOT NULL DEFAULT 'ASSIGNED',
  dispatch_type VARCHAR(16)  DEFAULT NULL,
  assign_time   DATETIME     DEFAULT NULL,
  accept_time   DATETIME     DEFAULT NULL,
  arrive_time   DATETIME     DEFAULT NULL,
  finish_time   DATETIME     DEFAULT NULL,
  finish_desc   VARCHAR(500) DEFAULT NULL,
  rework_reason VARCHAR(200) DEFAULT NULL,
  timeout_flag  TINYINT      NOT NULL DEFAULT 0,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_round (order_id, round_no),
  KEY idx_repairer (repairer_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维修轮次';

-- 6.2 工单验收记录（每轮一条 PASS/REJECT）
CREATE TABLE t_order_acceptance (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  order_id    BIGINT       NOT NULL,
  round_no    INT          NOT NULL,
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

-- 7. 派单记录（含优先级调整留痕）
CREATE TABLE t_dispatch_record (
  id              BIGINT      NOT NULL AUTO_INCREMENT,
  order_id        BIGINT      NOT NULL,
  round_no        INT         NOT NULL DEFAULT 1 COMMENT '所属维修轮次',
  operator_id     BIGINT      NOT NULL COMMENT '操作人：ASSIGN/CHANGE_PRIORITY/RELEASE=管理员，GRAB/REJECT=维修工',
  assignee_id     BIGINT      DEFAULT NULL,
  dispatch_type   VARCHAR(16) NOT NULL COMMENT 'ASSIGN/GRAB/REJECT/RELEASE/CHANGE_PRIORITY',
  before_priority TINYINT     DEFAULT NULL,
  after_priority  TINYINT     DEFAULT NULL,
  remark          VARCHAR(200) DEFAULT NULL,
  deleted         TINYINT     NOT NULL DEFAULT 0,
  create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_order (order_id),
  KEY idx_operator (operator_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='派单记录';

-- 8. 维修过程记录（一单一条，重复提交覆盖）
CREATE TABLE t_repair_record (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  order_id      BIGINT       NOT NULL,
  round_no      INT          NOT NULL DEFAULT 1 COMMENT '所属维修轮次',
  repairer_id   BIGINT       NOT NULL,
  process_desc  VARCHAR(1000) DEFAULT NULL COMMENT '维修过程描述',
  result_desc   VARCHAR(500) DEFAULT NULL COMMENT '完工说明',
  no_parts_flag TINYINT      NOT NULL DEFAULT 0 COMMENT '1表示确认无需更换配件（BR-06 完工前置校验）',
  cost_minutes  INT          DEFAULT NULL COMMENT '维修耗时 = finish_time - arrive_time（分钟）',
  version       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁（整单覆盖时防并发）',
  deleted       TINYINT      NOT NULL DEFAULT 0,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_round (order_id, round_no),
  KEY idx_repairer (repairer_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维修过程记录';

-- 9. 更换配件明细
CREATE TABLE t_part_usage (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  order_id    BIGINT       NOT NULL,
  round_no    INT          NOT NULL DEFAULT 1 COMMENT '所属维修轮次',
  part_id     BIGINT       DEFAULT NULL COMMENT '可为空：非库存登记件',
  part_name   VARCHAR(50)  NOT NULL,
  quantity    DECIMAL(8,2) NOT NULL DEFAULT 1,
  unit        VARCHAR(10)  DEFAULT NULL COMMENT '个/米/套',
  unit_price  DECIMAL(10,2) DEFAULT 0.00,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_order (order_id),
  KEY idx_order_round (order_id, round_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配件使用明细';

-- 10. 评价
CREATE TABLE t_evaluation (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  order_id       BIGINT       NOT NULL,
  final_round_no INT          NOT NULL DEFAULT 1 COMMENT '评价时最终轮次',
  student_id     BIGINT       NOT NULL,
  repairer_id BIGINT       NOT NULL,
  score       TINYINT      NOT NULL COMMENT '1~5',
  tags        VARCHAR(100) DEFAULT NULL COMMENT '速度快,态度好,技术专业',
  content     VARCHAR(500) DEFAULT NULL,
  anonymous   TINYINT      NOT NULL DEFAULT 0,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order (order_id),
  KEY idx_repairer (repairer_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价';

-- 11. 超时记录
CREATE TABLE t_timeout_record (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  order_id     BIGINT      NOT NULL,
  round_no     INT         NOT NULL DEFAULT 1 COMMENT '所属维修轮次',
  timeout_type VARCHAR(20) NOT NULL COMMENT 'ACCEPT/ARRIVE/FINISH',
  plan_time    DATETIME    NOT NULL,
  actual_time  DATETIME    DEFAULT NULL,
  over_minutes INT         NOT NULL DEFAULT 0,
  mark_type    VARCHAR(10) NOT NULL DEFAULT 'AUTO' COMMENT 'AUTO自动/MANUAL手动',
  mark_by      BIGINT      DEFAULT NULL,
  mark_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标记动作发生时间（业务时间）',
  deleted      TINYINT     NOT NULL DEFAULT 0,
  create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'BaseEntity 自动填充',
  update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_round_type (order_id, round_no, timeout_type),
  KEY idx_mark_time (mark_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='超时记录';

-- 12. 站内通知
CREATE TABLE t_notice (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  order_id    BIGINT       DEFAULT NULL,
  type        VARCHAR(30)  NOT NULL COMMENT 'ORDER_ASSIGNED(派单/抢单)/ORDER_ACCEPTED/ORDER_ARRIVED/ORDER_REJECTED/ORDER_FINISHED/ORDER_EVALUATED/ORDER_REWORK/ORDER_TIMEOUT/ORDER_URGED，共 9 种，以 8.5 表为准',
  title       VARCHAR(100) NOT NULL,
  content     VARCHAR(300) DEFAULT NULL,
  read_flag   TINYINT      NOT NULL DEFAULT 0,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_read (user_id, read_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知';

-- 13. 管理员-楼栋关联（多对多：一个管理员可负责多栋，一栋也可有多个管理员）
CREATE TABLE t_manager_building (
  id          BIGINT   NOT NULL AUTO_INCREMENT,
  manager_id  BIGINT   NOT NULL COMMENT 't_user.id（role=MANAGER）',
  building_id BIGINT   NOT NULL COMMENT 't_building.id',
  deleted     TINYINT  NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_manager_building (manager_id, building_id),
  KEY idx_building (building_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员负责楼栋关联';
