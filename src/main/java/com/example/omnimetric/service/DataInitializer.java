package com.example.omnimetric.service;

import com.example.omnimetric.model.entity.Asset;
import com.example.omnimetric.model.entity.MetricConfig;
import com.example.omnimetric.repository.AssetRepository;
import com.example.omnimetric.repository.MetricConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * 数据初始化器
 * 应用启动时自动创建模拟素材数据（不少于30条）和预置指标配置。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AssetRepository assetRepository;
    private final MetricConfigRepository metricConfigRepository;
    private final Random random = new Random(42); // 固定种子，保证每次启动数据一致

    private static final List<String> UPLOADERS = List.of("张三", "李四", "王五", "赵六", "钱七", "孙八");
    private static final List<String> STATUSES = List.of("approved", "rejected", "pending");
    private static final List<String> CITIES = List.of("上海", "北京", "广州", "深圳", "杭州", "成都", "武汉");
    private static final List<String> PLATFORMS = List.of("抖音", "快手", "小红书", "微信视频号");
    private static final List<String> TAG_POOL = List.of("短视频", "活动", "美食", "旅游", "科技", "教育", "搞笑", "游戏", "生活", "美妆");
    private static final List<String> TITLE_PREFIXES = List.of("春季", "夏季", "秋季", "冬季", "年度", "节日", "新品", "品牌", "用户", "爆款");
    private static final List<String> TITLE_SUFFIXES = List.of("活动视频", "推广短片", "产品展示", "品牌故事", "精彩回顾", "创意广告", "宣传片", "互动视频");

    public DataInitializer(AssetRepository assetRepository, MetricConfigRepository metricConfigRepository) {
        this.assetRepository = assetRepository;
        this.metricConfigRepository = metricConfigRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (assetRepository.count() > 0) {
            log.info("数据已存在，跳过初始化");
            return;
        }

        log.info("开始初始化模拟数据...");
        initAssets();
        initMetricConfigs();
        log.info("模拟数据初始化完成");
    }

    private void initAssets() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (int i = 1; i <= 35; i++) {
            Asset asset = new Asset();
            asset.setAssetId("A" + String.format("%05d", i));
            asset.setTitle(TITLE_PREFIXES.get(random.nextInt(TITLE_PREFIXES.size()))
                    + TITLE_SUFFIXES.get(random.nextInt(TITLE_SUFFIXES.size())));
            asset.setUploader(UPLOADERS.get(random.nextInt(UPLOADERS.size())));
            asset.setUploadedAt(LocalDateTime.parse(
                    "2026-0" + (random.nextInt(5) + 1) + "-"
                            + String.format("%02d", random.nextInt(28) + 1)
                            + " " + String.format("%02d", random.nextInt(24))
                            + ":" + String.format("%02d", random.nextInt(60))
                            + ":" + String.format("%02d", random.nextInt(60)),
                    dtf));
            asset.setFileSizeBytes((long) (random.nextInt(500) + 10) * 1024 * 1024); // 10MB-510MB
            asset.setStatus(STATUSES.get(random.nextInt(STATUSES.size())));
            asset.setCity(CITIES.get(random.nextInt(CITIES.size())));
            asset.setPlatform(PLATFORMS.get(random.nextInt(PLATFORMS.size())));
            asset.setDurationSeconds(random.nextInt(300) + 15); // 15-315秒

            // 随机生成1-3个标签
            int tagCount = random.nextInt(3) + 1;
            StringBuilder tags = new StringBuilder();
            for (int j = 0; j < tagCount; j++) {
                if (j > 0) tags.append(",");
                tags.append(TAG_POOL.get(random.nextInt(TAG_POOL.size())));
            }
            asset.setTags(tags.toString());

            assetRepository.save(asset);
        }

        log.info("已生成 {} 条素材数据", assetRepository.count());
    }

    private void initMetricConfigs() {
        // 指标1：按审核状态统计素材数量
        MetricConfig config1 = new MetricConfig();
        config1.setName("按审核状态统计素材数量");
        config1.setDescription("统计各审核状态（已通过/已拒绝/待审核）下的素材数量");
        config1.setSourceTable("asset");
        config1.setAggregateField("asset_id");
        config1.setAggregateType("COUNT");
        config1.setGroupByField("status");
        config1.setSortBy("result");
        config1.setSortOrder("desc");
        config1.setEnabled(true);
        metricConfigRepository.save(config1);

        // 指标2：已通过审核素材中各上传人的平均文件大小
        MetricConfig config2 = new MetricConfig();
        config2.setName("已通过素材的各上传人平均文件大小");
        config2.setDescription("统计已通过审核（status=approved）素材中，各上传人的平均文件大小");
        config2.setSourceTable("asset");
        config2.setAggregateField("file_size_bytes");
        config2.setAggregateType("AVG");
        config2.setGroupByField("uploader");
        config2.setFilterConditions("status = 'approved'");
        config2.setSortBy("result");
        config2.setSortOrder("desc");
        config2.setEnabled(true);
        metricConfigRepository.save(config2);

        // 指标3：各城市的素材总时长（自选 - 有业务意义：了解各城市内容产出量）
        // 业务意义：视频总时长反映了各城市的内容产出规模，帮助运营团队评估区域内容供给能力
        MetricConfig config3 = new MetricConfig();
        config3.setName("各城市素材总时长");
        config3.setDescription("统计各城市上传素材的视频总时长，反映区域内容产出规模");
        config3.setSourceTable("asset");
        config3.setAggregateField("duration_seconds");
        config3.setAggregateType("SUM");
        config3.setGroupByField("city");
        config3.setSortBy("result");
        config3.setSortOrder("desc");
        config3.setEnabled(true);
        metricConfigRepository.save(config3);

        log.info("已预置 3 个指标配置");
    }
}
