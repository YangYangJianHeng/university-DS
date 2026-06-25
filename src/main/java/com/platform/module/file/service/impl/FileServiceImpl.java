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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, FileEntity> implements FileService {

    private static final String UPLOAD_DIR = "./uploads/";

    @Override
    public FileEntity upload(MultipartFile file, Long teamId, Long parentId, Long uploaderId) {
        // Ensure upload directory exists
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            throw new BusinessException("Failed to create upload directory");
        }

        // Generate unique file key
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileKey = UUID.randomUUID().toString() + extension;

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
        fileEntity.setParentId(parentId);
        fileEntity.setName(originalFilename);
        fileEntity.setType("FILE");
        fileEntity.setFileKey(fileKey);
        fileEntity.setFileSize(file.getSize());
        fileEntity.setMimeType(file.getContentType());
        fileEntity.setVersion(1);
        fileEntity.setUploaderId(uploaderId);
        fileEntity.setIsDeleted(0);
        save(fileEntity);

        return fileEntity;
    }

    @Override
    public List<FileEntity> listFiles(Long teamId, Long parentId) {
        return list(new LambdaQueryWrapper<FileEntity>()
                .eq(FileEntity::getTeamId, teamId)
                .eq(FileEntity::getParentId, parentId != null ? parentId : 0)
                .eq(FileEntity::getIsDeleted, 0)
                .orderByDesc(FileEntity::getCreatedAt));
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
    public FileEntity createFolder(Long teamId, Long parentId, String name, Long uploaderId) {
        FileEntity folder = new FileEntity();
        folder.setTeamId(teamId);
        folder.setParentId(parentId != null ? parentId : 0);
        folder.setName(name);
        folder.setType("FOLDER");
        folder.setIsDeleted(0);
        folder.setUploaderId(uploaderId);
        save(folder);
        return folder;
    }
}
