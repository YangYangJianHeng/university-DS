package com.platform.module.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.common.exception.BusinessException;
import com.platform.module.file.entity.FileEntity;
import com.platform.module.file.mapper.FileMapper;
import com.platform.module.file.service.FileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, FileEntity> implements FileService {

    private static final String UPLOAD_DIR = "./uploads/";

    /** 允许上传的文件扩展名白名单 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        // 文档
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "txt", "csv", "md",
        // 图片（排除了svg避免存储型XSS）
        "jpg", "jpeg", "png", "gif", "bmp", "webp",
        // 压缩包
        "zip", "rar", "7z", "tar", "gz",
        // 音视频
        "mp4", "avi", "mov", "mkv", "mp3", "wav", "flac"
    );

    /** 禁止的 MIME 类型（防止伪造扩展名绕过） */
    private static final Set<String> BLOCKED_MIME_TYPES = Set.of(
        "application/x-msdownload",       // .exe
        "application/x-dosexec",          // .exe (alternative)
        "application/x-msdos-program",    // .exe/.com
        "application/x-sh",               // .sh
        "application/x-bsh",              // .bsh
        "application/x-csh",              // .csh
        "application/x-perl",             // .pl
        "application/x-php",              // .php
        "application/x-httpd-php",        // .php
        "application/x-jsp",              // .jsp
        "application/x-asp",              // .asp
        "application/x-java-jnlp-file",   // .jnlp
        "application/x-ms-shortcut"       // .lnk
    );

    /** 文件最大大小 (50MB) */
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    @Override
    public FileEntity upload(MultipartFile file, Long teamId, Long projectId, Long taskId, Long parentId, Long uploaderId) {
        // 1. 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }

        // 2. 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过50MB");
        }

        // 3. 校验文件扩展名白名单
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("不支持的文件类型: ." + extension);
        }

        // 4. 校验 MIME 类型黑名单（防止伪造扩展名绕过）
        String contentType = file.getContentType();
        if (contentType != null && BLOCKED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("不支持的文件类型");
        }

        // Ensure upload directory exists
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            throw new BusinessException("Failed to create upload directory");
        }

        // Generate unique file key
        String fileKey = UUID.randomUUID().toString() + "." + extension;

        // Save file to disk
        Path targetPath = Paths.get(UPLOAD_DIR, fileKey);
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("Failed to store file");
        }

        // Create file entity
        FileEntity fileEntity = new FileEntity();
        fileEntity.setTeamId(teamId);
        fileEntity.setProjectId(projectId);
        fileEntity.setParentId(parentId != null ? parentId : 0);
        fileEntity.setName(originalFilename);
        fileEntity.setType("FILE");
        fileEntity.setFileKey(fileKey);
        fileEntity.setFileSize(file.getSize());
        fileEntity.setMimeType(contentType);
        fileEntity.setVersion(1);
        fileEntity.setUploaderId(uploaderId);
        fileEntity.setIsDeleted(0);
        save(fileEntity);

        return fileEntity;
    }

    @Override
    public List<FileEntity> listFiles(Long teamId, Long projectId, Long parentId) {
        LambdaQueryWrapper<FileEntity> wrapper = new LambdaQueryWrapper<FileEntity>()
                .eq(FileEntity::getIsDeleted, 0)
                .orderByDesc(FileEntity::getCreatedAt);
        if (teamId != null) {
            wrapper.eq(FileEntity::getTeamId, teamId);
        }
        if (projectId != null) {
            wrapper.eq(FileEntity::getProjectId, projectId);
        }
        wrapper.eq(FileEntity::getParentId, parentId != null ? parentId : 0);
        return list(wrapper);
    }

    @Override
    public List<FileEntity> listFilesByTaskId(Long taskId) {
        LambdaQueryWrapper<FileEntity> wrapper = new LambdaQueryWrapper<FileEntity>()
                .eq(FileEntity::getTaskId, taskId)
                .eq(FileEntity::getIsDeleted, 0)
                .orderByDesc(FileEntity::getCreatedAt);
        return list(wrapper);
    }

    @Override
    public String getDownloadUrl(Long id) {
        FileEntity fileEntity = getById(id);
        if (fileEntity == null || fileEntity.getIsDeleted() == 1) {
            throw BusinessException.notFound("File not found");
        }
        return "/api/file/" + id + "/download";
    }

    @Override
    public String getPreviewUrl(Long id) {
        FileEntity fileEntity = getById(id);
        if (fileEntity == null || fileEntity.getIsDeleted() == 1) {
            throw BusinessException.notFound("File not found");
        }
        if (fileEntity.getMimeType() != null && fileEntity.getMimeType().startsWith("image/")) {
            return "/api/file/" + id + "/preview";
        }
        return null;
    }

    @Override
    public List<FileEntity> getVersions(Long id) {
        // For simplicity, return the file itself as the only version.
        // In a real app, there would be a version history table.
        FileEntity fileEntity = getById(id);
        if (fileEntity == null) {
            throw BusinessException.notFound("File not found");
        }
        return List.of(fileEntity);
    }

    @Override
    public FileEntity rollback(Long id, Integer version) {
        FileEntity fileEntity = getById(id);
        if (fileEntity == null) {
            throw BusinessException.notFound("File not found");
        }
        // In a real app, restore from version history.
        fileEntity.setVersion(version);
        updateById(fileEntity);
        return fileEntity;
    }

    @Override
    public void softDelete(Long id) {
        FileEntity fileEntity = getById(id);
        if (fileEntity == null) {
            throw BusinessException.notFound("File not found");
        }
        fileEntity.setIsDeleted(1);
        updateById(fileEntity);
    }

    @Override
    public void rename(Long id, String name) {
        FileEntity fileEntity = getById(id);
        if (fileEntity == null) {
            throw BusinessException.notFound("File not found");
        }
        fileEntity.setName(name);
        updateById(fileEntity);
    }

    @Override
    public FileEntity createFolder(Long teamId, Long projectId, Long parentId, String name, Long uploaderId) {
        FileEntity folder = new FileEntity();
        folder.setTeamId(teamId);
        folder.setProjectId(projectId);
        folder.setParentId(parentId != null ? parentId : 0);
        folder.setName(name);
        folder.setType("FOLDER");
        folder.setIsDeleted(0);
        folder.setUploaderId(uploaderId);
        save(folder);
        return folder;
    }
}
