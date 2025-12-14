package xyh.dp.mall.trade.tracking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyh.dp.mall.trade.tracking.entity.UserTrackingEvent;
import xyh.dp.mall.trade.tracking.mapper.UserTrackingEventMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 训练数据导出服务
 * 将埋点数据导出为CSV格式用于模型训练
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingDataExportService {

    private final UserTrackingEventMapper eventMapper;

    /**
     * 导出训练数据为CSV格式
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return CSV文件内容字节数组
     * @throws IOException IO异常
     */
    public byte[] exportTrainingDataAsCsv(LocalDateTime startTime, LocalDateTime endTime) throws IOException {
        log.info("开始导出训练数据: startTime={}, endTime={}", startTime, endTime);
        
        List<UserTrackingEvent> events = eventMapper.findTrainingEvents(startTime, endTime);
        log.info("查询到训练事件数量: {}", events.size());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            // 写入表头
            writer.write(UserTrackingEvent.csvHeader());
            writer.write("\n");
            
            // 写入数据行
            for (UserTrackingEvent event : events) {
                writer.write(event.toCsvLine());
                writer.write("\n");
            }
            
            writer.flush();
        }
        
        log.info("训练数据导出完成, 文件大小: {} bytes", baos.size());
        return baos.toByteArray();
    }

    /**
     * 导出正样本数据为CSV格式
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return CSV文件内容字节数组
     * @throws IOException IO异常
     */
    public byte[] exportPositiveSamplesAsCsv(LocalDateTime startTime, LocalDateTime endTime) throws IOException {
        log.info("开始导出正样本数据: startTime={}, endTime={}", startTime, endTime);
        
        List<UserTrackingEvent> events = eventMapper.findPositiveEvents(startTime, endTime);
        log.info("查询到正样本事件数量: {}", events.size());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            // 写入表头
            writer.write(UserTrackingEvent.csvHeader());
            writer.write("\n");
            
            // 写入数据行
            for (UserTrackingEvent event : events) {
                writer.write(event.toCsvLine());
                writer.write("\n");
            }
            
            writer.flush();
        }
        
        log.info("正样本数据导出完成, 文件大小: {} bytes", baos.size());
        return baos.toByteArray();
    }

    /**
     * 生成导出文件名
     * 
     * @param prefix 文件名前缀
     * @return 文件名
     */
    public String generateFileName(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return prefix + "_" + timestamp + ".csv";
    }

    /**
     * 获取指定时间范围内的事件统计
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计信息
     */
    public TrainingDataStats getStats(LocalDateTime startTime, LocalDateTime endTime) {
        List<UserTrackingEvent> allEvents = eventMapper.findTrainingEvents(startTime, endTime);
        List<UserTrackingEvent> positiveEvents = eventMapper.findPositiveEvents(startTime, endTime);
        
        int totalCount = allEvents.size();
        int positiveCount = positiveEvents.size();
        int negativeCount = totalCount - positiveCount;
        double positiveRate = totalCount > 0 ? (double) positiveCount / totalCount * 100 : 0;
        
        return new TrainingDataStats(totalCount, positiveCount, negativeCount, positiveRate);
    }

    /**
     * 训练数据统计信息
     */
    public record TrainingDataStats(
            int totalCount,
            int positiveCount,
            int negativeCount,
            double positiveRate
    ) {}
}
