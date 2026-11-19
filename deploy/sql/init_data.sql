USE school_repair;

-- 初始化数据：楼栋 / 房间 / 故障类型 / 管理员-楼栋 / 默认账号
-- 说明：显式指定 id，避免自增 id 依赖历史数据导致关联错位。
-- 默认账号密码均为 123456（BCrypt strength=10，密文由 PasswordGenTest 生成并校验通过）。

INSERT INTO t_building (id, name, code, area, floors) VALUES
(1, '一号宿舍楼', 'A1', '东区', 6),
(2, '二号宿舍楼', 'A2', '东区', 6),
(3, '三号宿舍楼', 'B1', '西区', 8);

INSERT INTO t_room (id, building_id, room_no, floor) VALUES
(1, 1, '101', 1),
(2, 1, '102', 1),
(3, 1, '201', 2),
(4, 2, '101', 1),
(5, 3, '101', 1);

INSERT INTO t_fault_type (id, parent_id, name, category, default_priority, expect_hours, sort) VALUES
(1, 0, '水电类', 'WATER_ELEC', 2, 24, 1),
(2, 0, '门窗类', 'DOOR_WINDOW', 3, 48, 2),
(3, 0, '网络类', 'NETWORK', 3, 24, 3),
(4, 0, '家具类', 'FURNITURE', 4, 72, 4),
(5, 0, '其他', 'OTHER', 4, 48, 5),
(6, 1, '水龙头漏水', 'WATER_ELEC', 2, 8, 1),
(7, 1, '下水道堵塞', 'WATER_ELEC', 2, 12, 2),
(8, 1, '停电/跳闸', 'WATER_ELEC', 1, 4, 3),
(9, 1, '灯管损坏', 'WATER_ELEC', 3, 24, 4),
(10, 2, '门锁损坏', 'DOOR_WINDOW', 2, 12, 1),
(11, 2, '窗户玻璃破损', 'DOOR_WINDOW', 1, 8, 2),
(12, 3, '网线/接口故障', 'NETWORK', 3, 24, 1),
(13, 3, '路由器无信号', 'NETWORK', 3, 12, 2),
(14, 4, '床架松动', 'FURNITURE', 3, 48, 1),
(15, 4, '衣柜门脱落', 'FURNITURE', 3, 48, 2);

-- 管理员负责楼栋：manager01 负责 A1/A2，manager02 负责 B1
INSERT INTO t_manager_building (manager_id, building_id) VALUES
(1, 1),
(1, 2),
(2, 3);

-- 默认账号（密码 123456）
INSERT INTO t_user (id, username, password, real_name, phone, role, building_id, room_id, skill_tags, status) VALUES
(1, 'manager01', '$2a$10$jH/zZMMUcXWxIuDKWVswm..ka4DO7PY.naJ4XWVEfu9OVJWldO9j.', '张管理', '13800000001', 'MANAGER', NULL, NULL, NULL, 1),
(2, 'manager02', '$2a$10$jH/zZMMUcXWxIuDKWVswm..ka4DO7PY.naJ4XWVEfu9OVJWldO9j.', '李管理', '13800000002', 'MANAGER', NULL, NULL, NULL, 1),
(3, 'repairer01', '$2a$10$jH/zZMMUcXWxIuDKWVswm..ka4DO7PY.naJ4XWVEfu9OVJWldO9j.', '王师傅', '13800000003', 'REPAIRER', 1, NULL, '水电,门锁', 1),
(4, 'repairer02', '$2a$10$jH/zZMMUcXWxIuDKWVswm..ka4DO7PY.naJ4XWVEfu9OVJWldO9j.', '赵师傅', '13800000004', 'REPAIRER', 1, NULL, '网络,家具', 1),
(5, 'repairer03', '$2a$10$jH/zZMMUcXWxIuDKWVswm..ka4DO7PY.naJ4XWVEfu9OVJWldO9j.', '孙师傅', '13800000005', 'REPAIRER', 2, NULL, '水电,门窗', 1),
(6, 'student01', '$2a$10$jH/zZMMUcXWxIuDKWVswm..ka4DO7PY.naJ4XWVEfu9OVJWldO9j.', '秦天洪', '13800000006', 'STUDENT', 1, 3, NULL, 1),
(7, 'student02', '$2a$10$jH/zZMMUcXWxIuDKWVswm..ka4DO7PY.naJ4XWVEfu9OVJWldO9j.', '陈学生', '13800000007', 'STUDENT', 1, 1, NULL, 1),
(8, 'student03', '$2a$10$jH/zZMMUcXWxIuDKWVswm..ka4DO7PY.naJ4XWVEfu9OVJWldO9j.', '刘学生', '13800000008', 'STUDENT', 2, 4, NULL, 1);
