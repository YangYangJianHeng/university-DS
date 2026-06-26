package com.platform.module.badge.service;

import com.platform.module.badge.entity.Certificate;

import java.util.List;
import java.util.Map;

public interface CertificateService {

    /**
     * 管理员批量同步：为拥有指定徽章但未生成奖状的用户生成奖状
     * @param badgeId 徽章ID，null表示同步所有徽章
     * @return 新生成的证书列表
     */
    List<Certificate> syncCertificates(Long badgeId);

    /**
     * 管理员删除某个证书
     */
    void deleteCertificate(Long id);

    /**
     * 管理员查看所有证书列表
     */
    List<Certificate> listAllCertificates(int page, int size);

    /**
     * 管理员统计证书总数
     */
    long countAllCertificates();

    /**
     * 获取用户的证书列表（按获奖时间倒序）
     */
    List<Certificate> getUserCertificates(Long userId);

    /**
     * 生成单个用户的奖状图片（如果尚未生成）
     */
    Certificate generateCertificate(Long userId, Long badgeId);

    /**
     * 获取证书信息（含用户数据和徽章名称）
     */
    Map<String, Object> getCertificateViewData(Long certificateId);

    /**
     * 删除所有旧证书并重新生成（使用新的模板样式）
     * @return 重新生成的证书列表
     */
    List<Certificate> regenerateAllCertificates();
}
