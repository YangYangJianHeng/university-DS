package com.platform.module.file.controller;

import com.platform.common.result.Result;
import com.platform.common.util.FileAccessSigner;
import com.platform.common.util.UserContext;
import com.platform.module.file.entity.FileEntity;
import com.platform.module.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "File Management")
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileService fileService;
    private final FileAccessSigner fileAccessSigner;

    private static final String UPLOAD_DIR = "./uploads/";

    public FileController(FileService fileService, FileAccessSigner fileAccessSigner) {
        this.fileService = fileService;
        this.fileAccessSigner = fileAccessSigner;
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload file")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam(required = false) Long teamId,
                                     @RequestParam(required = false) Long projectId,
                                     @RequestParam(required = false) Long parentId) {
        Long userId = UserContext.getCurrentUserId();
        // taskId 在上传时尚未确定，创建任务或提交成果时后端再关联
        FileEntity fileEntity = fileService.upload(file, teamId, projectId, null, parentId, userId);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("id", fileEntity.getId());
        result.put("name", fileEntity.getName());
        result.put("mimeType", fileEntity.getMimeType());
        result.put("fileSize", fileEntity.getFileSize());
        return Result.success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "List files")
    public Result<List<FileEntity>> list(@RequestParam(required = false) Long teamId,
                                         @RequestParam(required = false) Long projectId,
                                         @RequestParam(required = false) Long taskId,
                                         @RequestParam(required = false) Long parentId) {
        if (taskId != null) {
            return Result.success(fileService.listFilesByTaskId(taskId));
        }
        return Result.success(fileService.listFiles(teamId, projectId, parentId));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download file")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        FileEntity fileEntity = fileService.getById(id);
        if (fileEntity == null || fileEntity.getIsDeleted() == 1) {
            return ResponseEntity.notFound().build();
        }
        return serveResource(fileEntity, false);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get file info")
    public Result<Map<String, Object>> fileInfo(@PathVariable Long id) {
        FileEntity fileEntity = fileService.getById(id);
        if (fileEntity == null || fileEntity.getIsDeleted() == 1) {
            return Result.error("文件不存在");
        }
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", fileEntity.getId());
        info.put("name", fileEntity.getName());
        info.put("fileKey", fileEntity.getFileKey());
        info.put("fileSize", fileEntity.getFileSize());
        info.put("mimeType", fileEntity.getMimeType());
        // 安全签名URL（5分钟有效期），替代直接拼接token的形式
        info.put("previewToken", fileAccessSigner.sign(id, "preview"));
        info.put("downloadToken", fileAccessSigner.sign(id, "download"));
        info.put("createdAt", fileEntity.getCreatedAt());
        return Result.success(info);
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "Preview file")
    public ResponseEntity<Resource> preview(@PathVariable Long id) {
        FileEntity fileEntity = fileService.getById(id);
        if (fileEntity == null || fileEntity.getIsDeleted() == 1) {
            return ResponseEntity.notFound().build();
        }
        return serveResource(fileEntity, true);
    }

    /**
     * 生成文件访问签名URL（5分钟有效）
     */
    @GetMapping("/{id}/signed-url")
    @Operation(summary = "Generate signed access URL")
    public Result<Map<String, String>> getSignedUrl(@PathVariable Long id) {
        FileEntity fileEntity = fileService.getById(id);
        if (fileEntity == null || fileEntity.getIsDeleted() == 1) {
            return Result.error("文件不存在");
        }
        Map<String, String> urls = new LinkedHashMap<>();
        urls.put("previewUrl", "/api/public/file/preview?token=" + fileAccessSigner.sign(id, "preview"));
        urls.put("downloadUrl", "/api/public/file/download?token=" + fileAccessSigner.sign(id, "download"));
        return Result.success(urls);
    }

    /**
     * 安全地提供文件资源，防止路径穿越攻击
     */
    private ResponseEntity<Resource> serveResource(FileEntity fileEntity, boolean isPreview) {
        try {
            Path uploadDir = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            Path filePath = uploadDir.resolve(fileEntity.getFileKey()).normalize();
            // 防止路径穿越攻击：确保解析后的路径仍然在 uploads 目录下
            if (!filePath.startsWith(uploadDir)) {
                return ResponseEntity.status(403).build();
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                if (isPreview) {
                    String contentType = fileEntity.getMimeType() != null ? fileEntity.getMimeType() : "application/octet-stream";
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .body(resource);
                } else {
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\"" + fileEntity.getName() + "\"")
                            .body(resource);
                }
            }
        } catch (MalformedURLException e) {
            // ignore
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "Get file versions")
    public Result<List<FileEntity>> versions(@PathVariable Long id) {
        return Result.success(fileService.getVersions(id));
    }

    @PostMapping("/{id}/rollback/{version}")
    @Operation(summary = "Rollback file version")
    public Result<FileEntity> rollback(@PathVariable Long id, @PathVariable Integer version) {
        return Result.success(fileService.rollback(id, version));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete file")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.softDelete(id);
        return Result.success();
    }

    @PutMapping("/{id}/rename")
    @Operation(summary = "Rename file")
    public Result<Void> rename(@PathVariable Long id, @RequestBody Map<String, String> request) {
        fileService.rename(id, request.get("name"));
        return Result.success();
    }

    @PostMapping("/folder")
    @Operation(summary = "Create folder")
    public Result<FileEntity> createFolder(@RequestBody Map<String, Object> request) {
        Long teamId = request.get("teamId") != null ? Long.valueOf(request.get("teamId").toString()) : null;
        Long projectId = request.get("projectId") != null ? Long.valueOf(request.get("projectId").toString()) : null;
        Long parentId = request.get("parentId") != null ? Long.valueOf(request.get("parentId").toString()) : null;
        String name = (String) request.get("name");
        Long userId = UserContext.getCurrentUserId();
        return Result.success(fileService.createFolder(teamId, projectId, parentId, name, userId));
    }
}
