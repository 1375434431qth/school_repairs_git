package com.cqust.repair.common.result;

import lombok.Getter;

/**
 * 统一业务状态码。
 *
 * <p>编码规则：通用错误直接用 3 位 HTTP 码；业务错误用 5 位码，
 * 前 3 位等于 HTTP 状态、后 2 位为业务细分。个别语义例外见注释。</p>
 *
 * @author school-repair
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功", 200),
    PARAM_ERROR(400, "参数校验失败", 400),
    ROOM_BUILDING_MISMATCH(40001, "房间与楼栋不匹配", 400),
    ILLEGAL_STATUS_TRANSIT(40002, "非法状态流转", 400),
    ILLEGAL_TIME(40003, "时间参数非法", 400),
    INVALID_FILE_TYPE(40004, "文件类型不合法", 400),
    FILE_TOO_LARGE(40005, "文件超过 5MB", 400),
    IMAGE_COUNT_EXCEED(40006, "图片数量超出限制", 400),
    ILLEGAL_DATE_RANGE(40007, "日期区间非法", 400),
    STAT_RANGE_TOO_LARGE(40008, "统计区间超过 365 天", 400),
    STATUS_NOT_ALLOWED(40009, "当前状态不允许该操作", 400),
    REPAIRER_UNAVAILABLE(40010, "该维修工不可用", 400),
    CANCEL_NOT_ALLOWED(40011, "当前状态不可取消", 400),
    UNAUTHORIZED(401, "未登录或登录已过期", 401),
    FORBIDDEN(403, "无权限访问", 403),
    NO_PERMISSION_OF_BUILDING(40301, "无该楼栋管理权限", 403),
    NOT_FOUND(404, "资源不存在", 404),
    ORDER_NOT_FOUND(40401, "工单不存在", 404),
    ORDER_ALREADY_TAKEN(40901, "工单已被他人接单", 409),
    ALREADY_EVALUATED(40902, "该工单已评价", 409),
    /** 幂等防抖：HTTP 返回 429，见文档 15.4 的例外说明 */
    DUPLICATE_SUBMIT(40903, "请勿重复提交", 429),
    CONCURRENT_MODIFIED(40904, "数据已被修改，请刷新重试", 409),
    /** 幂等成功：HTTP 返回 200 */
    ALREADY_MARKED_TIMEOUT(40905, "该环节已标记超时（幂等返回）", 200),
    TOO_MANY_REQUESTS(429, "操作过于频繁", 429),
    SYSTEM_ERROR(500, "系统异常，请稍后重试", 500),
    FILE_SERVICE_UNAVAILABLE(503, "图片服务暂不可用", 503);

    /** 业务码 */
    private final Integer code;
    /** 提示信息 */
    private final String message;
    /** 对应 HTTP 状态 */
    private final Integer httpStatus;

    ResultCode(Integer code, String message, Integer httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
