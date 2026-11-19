package com.cqust.repair.infra.cache;

/**
 * Redis Key 统一常量，命名格式 {@code sr:{模块}:{业务}:{标识}}。
 *
 * @author school-repair
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /** 访问 token（单端登录） */
    public static final String AUTH_TOKEN = "sr:auth:token:";
    /** 刷新 token */
    public static final String AUTH_REFRESH = "sr:auth:refresh:";
    /** 故障类型树 */
    public static final String DICT_FAULT_TREE = "sr:dict:fault:tree";
    /** 楼栋列表 */
    public static final String DICT_BUILDING_LIST = "sr:dict:building:list";
    /** 某楼栋房间列表 */
    public static final String DICT_ROOM_LIST = "sr:dict:room:list:";
    /** 首页轮播列表 */
    public static final String HOME_CAROUSEL = "sr:home:carousel:list";
    /** 抢单分布式锁 */
    public static final String ORDER_GRAB_LOCK = "sr:order:grab:lock:";
    /** 报修幂等 */
    public static final String ORDER_IDEM = "sr:order:idem:";
    /** 工单号当日序列 */
    public static final String ORDER_NO = "sr:order:no:";
    /** 工单详情缓存 */
    public static final String ORDER_DETAIL = "sr:order:detail:";
    /** 看板分列缓存 */
    public static final String ORDER_BOARD = "sr:order:board:";
    /** 超时扫描 ZSet */
    public static final String ORDER_TIMEOUT_ZSET = "sr:order:timeout:zset";
    /** 报修类型分布统计缓存 */
    public static final String STAT_TYPE_DIST = "sr:stat:type:dist:";
    /** 平均处理时长统计缓存 */
    public static final String STAT_AVG_DURATION = "sr:stat:avg:duration:";
    /** 返修率统计缓存 */
    public static final String STAT_REWORK_RATE = "sr:stat:rework:rate:";
    /** 核心指标卡缓存 */
    public static final String STAT_OVERVIEW = "sr:stat:overview:";
    /** 维修工绩效排行缓存 */
    public static final String STAT_REPAIRER_RANK = "sr:stat:repairer:rank:";
    /** 统计缓存统一前缀（强制刷新用） */
    public static final String STAT_PREFIX = "sr:stat:";
    /** 提交限流 */
    public static final String LIMIT_SUBMIT = "sr:limit:submit:";
    /** 登录失败限流 */
    public static final String LIMIT_LOGIN = "sr:limit:login:";
    /** 未读通知数 */
    public static final String NOTIFY_UNREAD = "sr:notify:unread:";
    /** MQ 通知消息消费幂等 */
    public static final String MQ_NOTICE_IDEM = "sr:mq:notice:";
}
