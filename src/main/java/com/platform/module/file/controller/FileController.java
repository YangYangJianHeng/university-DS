package com.platform.module.file.controller;

import com.platform.common.result.Result;
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
import java.util.List;
import java.util.Map;

@Tag(name = "File Management")
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileService fileService;

    private static final String UPLOAD_DIR = "./uploads/";

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload file")
    public Result<FileEntity> upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam(required = false) Long teamId,
                                     @RequestParam(required = false) Long parentId) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(fileService.upload(file, teamId, parentId, userId));
    }

    @GetMapping("/list")
    @Operation(summary = "List files")
    public Result<List<FileEntity>> list(@RequestParam(required = false) Long teamId,
                                         @RequestParam(required = false) Long parentId) {
        return Result.success(fileService.listFiles(teamId, parentId));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download file")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        FileEntity fileEntity = fileService.getById(id);
        if (fileEntity == null || fileEntity.getIsDeleted() == 1) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(fileEntity.getFileKey()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + fileEntity.getName() + "\"")
                        .body(resource);
            }
        } catch (MalformedURLException e) {
            // ignore
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "Preview file")
    public ResponseEntity<Resource> preview(@PathVariable Long id) {
        FileEntity fileEntity = fileService.getById(id);
        if (fileEntity == null || fileEntity.getIsDeleted() == 1) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(fileEntity.getFileKey()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                String contentType = fileEntity.getMimeType() != null ? fileEntity.getMimeType() : "application/octet-stream";
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
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
        Long parentId = request.get("parentId") != null ? Long.valueOf(request.get("parentId").toString()) : null;
        String name = (String) request.get("name");
        Long userId = UserContext.getCurrentUserId();
        return Result.success(fileService.createFolder(teamId, parentId, name, userId));
    }
}
