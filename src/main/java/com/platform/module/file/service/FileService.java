package com.platform.module.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.platform.module.file.entity.FileEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService extends IService<FileEntity> {

    FileEntity upload(MultipartFile file, Long teamId, Long projectId, Long parentId, Long uploaderId);

    List<FileEntity> listFiles(Long teamId, Long projectId, Long parentId);

    String getDownloadUrl(Long id);

    String getPreviewUrl(Long id);

    List<FileEntity> getVersions(Long id);

    FileEntity rollback(Long id, Integer version);

    void softDelete(Long id);

    void rename(Long id, String name);

    FileEntity createFolder(Long teamId, Long projectId, Long parentId, String name, Long uploaderId);
}
