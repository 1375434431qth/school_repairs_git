package com.cqust.repair.module.file.service;

import com.cqust.repair.module.file.dto.FileUploadVO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;

/**
 * 图片存储服务（MongoDB GridFS）。
 *
 * @author school-repair
 */
public interface FileService {

    /**
     * 上传图片：后缀 + MIME + 文件头三重校验后写入 GridFS。
     *
     * @param file    上传文件
     * @param bizType BEFORE/AFTER/AVATAR
     * @param userId  上传人
     */
    FileUploadVO upload(MultipartFile file, String bizType, Long userId);

    /**
     * 读取图片资源。
     */
    Resource load(String fileId);

    /**
     * 读取图片 Content-Type。
     */
    String contentType(String fileId);

    /**
     * 删除图片：仅上传者本人、且未提交到工单时可删。
     */
    void delete(String fileId, Long userId);

    /**
     * 判断 fileId 是否存在。
     */
    boolean exists(String fileId);

    /**
     * 标记图片已提交到工单，之后不可删除。
     */
    void markSubmitted(Collection<String> fileIds);
}
