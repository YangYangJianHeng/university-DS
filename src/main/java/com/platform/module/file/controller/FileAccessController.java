package com.platform.module.file.controller;

import com.platform.common.exception.BusinessException;
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

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文件安全访问 - 通过签名token而非JWT token访问文件
 * 避免JWT token通过URL参数泄露
 */
@Tag(name = "File Secure Access")
@RestController
@RequestMapping("/api/public/file")
public class FileAccessController {

    private final FileService fileService;
    private final FileAccessSigner fileAccessSigner;

    private static final String UPLOAD_DIR = "./uploads/";

    public FileAccessController(FileService fileService, FileAccessSigner fileAccessSigner) {
        this.fileService = fileService;
        this.fileAccessSigner = fileAccessSigner;
    }

    /**
     * 通过签名token下载文件（无需JWT认证）
     */
    @GetMapping("/download")
    @Operation(summary = "Download file via signed token")
    public ResponseEntity<Resource> download(@RequestParam("token") String token) {
        FileAccessSigner.ValidationResult vr = fileAccessSigner.validate(token);
        if (vr == null) {
            return ResponseEntity.status(401).build();
        }
        return serveFile(vr.getFileId(), false);
    }

    /**
     * 通过签名token预览文件（无需JWT认证）
     */
    @GetMapping("/preview")
    @Operation(summary = "Preview file via signed token")
    public ResponseEntity<Resource> preview(@RequestParam("token") String token) {
        FileAccessSigner.ValidationResult vr = fileAccessSigner.validate(token);
        if (vr == null) {
            return ResponseEntity.status(401).build();
        }
        return serveFile(vr.getFileId(), true);
    }

    private ResponseEntity<Resource> serveFile(Long fileId, boolean isPreview) {
        FileEntity fileEntity = fileService.getById(fileId);
        if (fileEntity == null || fileEntity.getIsDeleted() == 1) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(fileEntity.getFileKey()).normalize();
            // 防止路径穿越攻击
            if (!filePath.startsWith(Paths.get(UPLOAD_DIR).normalize())) {
                return ResponseEntity.status(403).build();
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                if (isPreview) {
                    String contentType = fileEntity.getMimeType() != null
                            ? fileEntity.getMimeType() : "application/octet-stream";
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
}
