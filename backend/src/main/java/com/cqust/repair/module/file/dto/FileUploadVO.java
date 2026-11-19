package com.cqust.repair.module.file.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片上传响应。
 *
 * @author school-repair
 */
@Data
public class FileUploadVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** GridFS ObjectId */
    private String fileId;
    /** 预览地址 */
    private String url;
    private String fileName;
    private Long fileSize;
    private String contentType;
}
