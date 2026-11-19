package com.cqust.repair.module.file.controller;

import com.cqust.repair.common.result.R;
import com.cqust.repair.common.util.SecurityUtils;
import com.cqust.repair.module.file.dto.FileUploadVO;
import com.cqust.repair.module.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

/**
 * 图片上传 / 预览 / 删除接口。
 *
 * @author school-repair
 */
@Tag(name = "文件")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传图片")
    @PostMapping("/upload")
    public R<FileUploadVO> upload(@RequestParam("file") MultipartFile file,
                                  @RequestParam("bizType") String bizType) {
        return R.success(fileService.upload(file, bizType, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "预览/下载图片（免登录）")
    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> preview(@PathVariable String fileId) {
        Resource resource = fileService.load(fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileService.contentType(fileId)))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .body(resource);
    }

    @Operation(summary = "删除未提交的图片")
    @DeleteMapping("/{fileId}")
    public R<Void> delete(@PathVariable String fileId) {
        fileService.delete(fileId, SecurityUtils.getCurrentUserId());
        return R.success();
    }
}
