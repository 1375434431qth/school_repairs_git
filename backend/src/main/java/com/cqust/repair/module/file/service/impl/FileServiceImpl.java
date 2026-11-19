package com.cqust.repair.module.file.service.impl;

import com.cqust.repair.common.enums.ImageTypeEnum;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.config.AppProperties;
import com.cqust.repair.module.file.dto.FileUploadVO;
import com.cqust.repair.module.file.service.FileService;
import com.cqust.repair.module.user.dto.ProfileUpdateRequest;
import com.cqust.repair.module.user.service.UserService;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * GridFS 图片存储实现。
 *
 * @author school-repair
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final String META_UPLOADER = "uploaderId";
    private static final String META_BIZ_TYPE = "bizType";
    private static final String META_ORIGINAL_NAME = "originalName";
    private static final String META_SUBMITTED = "submitted";
    private static final String META_UPLOADED_AT = "uploadedAt";
    private static final String FS_FILES = "fs.files";

    private final GridFsTemplate gridFsTemplate;
    private final MongoTemplate mongoTemplate;
    private final AppProperties appProperties;
    private final UserService userService;

    @Override
    public FileUploadVO upload(MultipartFile file, String bizType, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "上传文件不能为空");
        }
        ImageTypeEnum type = ImageTypeEnum.of(bizType);
        if (type == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "bizType 只能为 BEFORE/AFTER/AVATAR/CAROUSEL");
        }
        if (file.getSize() > appProperties.getUpload().getMaxSize()) {
            throw new BizException(ResultCode.FILE_TOO_LARGE);
        }
        String originalName = sanitizeName(file.getOriginalFilename());
        String ext = extensionOf(originalName);
        if (!Arrays.asList(appProperties.getUpload().getAllowExt()).contains(ext)) {
            throw new BizException(ResultCode.INVALID_FILE_TYPE);
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "读取上传文件失败");
        }
        if (!matchesMagic(bytes, ext)) {
            throw new BizException(ResultCode.INVALID_FILE_TYPE, "文件内容与图片格式不符");
        }

        String contentType = contentTypeOf(ext);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Document metadata = new Document()
                .append(META_UPLOADER, userId)
                .append(META_BIZ_TYPE, type.name())
                .append(META_ORIGINAL_NAME, originalName)
                .append(META_SUBMITTED, false)
                .append(META_UPLOADED_AT, new Date());
        ObjectId objectId = gridFsTemplate.store(new ByteArrayInputStream(bytes), storedName, contentType, metadata);

        FileUploadVO vo = new FileUploadVO();
        vo.setFileId(objectId.toHexString());
        vo.setUrl("/api/v1/files/" + objectId.toHexString());
        vo.setFileName(originalName);
        vo.setFileSize((long) bytes.length);
        vo.setContentType(contentType);

        // 头像上传后直接回写 t_user.avatar
        if (type == ImageTypeEnum.AVATAR) {
            ProfileUpdateRequest profile = new ProfileUpdateRequest();
            profile.setAvatar(vo.getUrl());
            userService.updateProfile(userId, profile);
        }
        return vo;
    }

    @Override
    public Resource load(String fileId) {
        GridFSFile gridFSFile = findFile(fileId);
        GridFsResource resource = gridFsTemplate.getResource(gridFSFile);
        if (!resource.exists()) {
            throw new BizException(ResultCode.NOT_FOUND, "图片不存在");
        }
        return resource;
    }

    @Override
    public String contentType(String fileId) {
        GridFSFile gridFSFile = findFile(fileId);
        String contentType = gridFSFile.getMetadata() == null
                ? null : gridFSFile.getMetadata().getString("_contentType");
        return StringUtils.hasText(contentType) ? contentType : "application/octet-stream";
    }

    @Override
    public void delete(String fileId, Long userId) {
        GridFSFile gridFSFile = findFile(fileId);
        Document metadata = gridFSFile.getMetadata();
        Long uploaderId = metadata == null ? null : metadata.getLong(META_UPLOADER);
        if (uploaderId == null || !uploaderId.equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "只能删除自己上传的图片");
        }
        if (metadata != null && Boolean.TRUE.equals(metadata.getBoolean(META_SUBMITTED, false))) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "图片已提交到工单，不可删除");
        }
        gridFsTemplate.delete(Query.query(Criteria.where("_id").is(new ObjectId(fileId))));
    }

    @Override
    public boolean exists(String fileId) {
        if (!StringUtils.hasText(fileId) || !ObjectId.isValid(fileId)) {
            return false;
        }
        return gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(new ObjectId(fileId)))) != null;
    }

    @Override
    public void markSubmitted(Collection<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        for (String fileId : fileIds) {
            if (!ObjectId.isValid(fileId)) {
                continue;
            }
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(new ObjectId(fileId))),
                    new Update().set("metadata." + META_SUBMITTED, true), FS_FILES);
        }
    }

    private GridFSFile findFile(String fileId) {
        if (!StringUtils.hasText(fileId) || !ObjectId.isValid(fileId)) {
            throw new BizException(ResultCode.NOT_FOUND, "图片不存在");
        }
        GridFSFile file = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(new ObjectId(fileId))));
        if (file == null) {
            throw new BizException(ResultCode.NOT_FOUND, "图片不存在");
        }
        return file;
    }

    /**
     * 去掉客户端文件名中的路径，防止目录穿越。
     */
    private String sanitizeName(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            return "image";
        }
        String name = originalName.replace("\\", "/");
        int index = name.lastIndexOf('/');
        return index >= 0 ? name.substring(index + 1) : name;
    }

    private String extensionOf(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            throw new BizException(ResultCode.INVALID_FILE_TYPE);
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String contentTypeOf(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    /**
     * 文件头校验（jpg/png/webp），防止改后缀绕过。
     */
    private boolean matchesMagic(byte[] data, String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> data.length >= 3
                    && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF;
            case "png" -> data.length >= 8
                    && (data[0] & 0xFF) == 0x89
                    && data[1] == 'P' && data[2] == 'N' && data[3] == 'G';
            case "webp" -> data.length >= 12
                    && "RIFF".equals(new String(data, 0, 4, StandardCharsets.ISO_8859_1))
                    && "WEBP".equals(new String(data, 8, 4, StandardCharsets.ISO_8859_1));
            default -> false;
        };
    }
}
