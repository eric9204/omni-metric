package com.example.omnimetric.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 素材明细数据表
 * 存储视频素材的元数据信息，作为指标查询的基础数据源
 */
@Entity
@Table(name = "asset", indexes = {
    @Index(name = "idx_asset_status", columnList = "status"),
    @Index(name = "idx_asset_uploader", columnList = "uploader"),
    @Index(name = "idx_asset_city", columnList = "city"),
    @Index(name = "idx_asset_platform", columnList = "platform"),
    @Index(name = "idx_asset_uploaded_at", columnList = "uploaded_at")
})
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 素材ID（业务标识） */
    @Column(name = "asset_id", length = 64, nullable = false, unique = true)
    private String assetId;

    /** 素材标题 */
    @Column(name = "title", length = 255)
    private String title;

    /** 上传人 */
    @Column(name = "uploader", length = 100, nullable = false)
    private String uploader;

    /** 上传时间 */
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    /** 文件大小（字节） */
    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    /** 审核状态：approved/rejected/pending */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    /** 标签（逗号分隔，如"短视频,活动"） */
    @Column(name = "tags", length = 500)
    private String tags;

    /** 城市 */
    @Column(name = "city", length = 100)
    private String city;

    /** 投放平台 */
    @Column(name = "platform", length = 100)
    private String platform;

    /** 视频时长（秒） */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUploader() { return uploader; }
    public void setUploader(String uploader) { this.uploader = uploader; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
