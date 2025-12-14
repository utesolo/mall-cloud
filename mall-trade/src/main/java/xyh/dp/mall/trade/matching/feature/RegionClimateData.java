package xyh.dp.mall.trade.matching.feature;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 区域气候数据
 * 存储各区域的平均气候数据，用于气候匹配计算
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegionClimateData {

    /**
     * 区域名称
     */
    private String region;

    /**
     * 平均温度(℃)
     */
    private BigDecimal avgTemperature;

    /**
     * 平均湿度(%)
     */
    private BigDecimal avgHumidity;

    /**
     * 主要光照条件
     */
    private String lightCondition;

    /**
     * 适宜种植季节
     */
    private String[] plantingSeasons;

    /**
     * 区域气候数据库（静态数据，实际应从气象API或数据库获取）
     */
    private static final Map<String, RegionClimateData> CLIMATE_DATABASE = new HashMap<>();

    static {
        // 华北地区
        CLIMATE_DATABASE.put("北京", new RegionClimateData("北京", 
                new BigDecimal("12.5"), new BigDecimal("55"), "FULL_SUN", 
                new String[]{"春季", "夏季", "秋季"}));
        CLIMATE_DATABASE.put("北京大兴", new RegionClimateData("北京大兴", 
                new BigDecimal("11.8"), new BigDecimal("58"), "FULL_SUN", 
                new String[]{"春季", "夏季", "秋季"}));
        
        // 华东地区
        CLIMATE_DATABASE.put("山东", new RegionClimateData("山东", 
                new BigDecimal("13.5"), new BigDecimal("62"), "FULL_SUN", 
                new String[]{"春季", "夏季", "秋季"}));
        CLIMATE_DATABASE.put("山东菏泽", new RegionClimateData("山东菏泽", 
                new BigDecimal("14.2"), new BigDecimal("65"), "FULL_SUN", 
                new String[]{"春季", "夏季", "秋季"}));
        CLIMATE_DATABASE.put("江苏", new RegionClimateData("江苏", 
                new BigDecimal("15.2"), new BigDecimal("72"), "PARTIAL_SUN", 
                new String[]{"春季", "夏季", "秋季"}));
        CLIMATE_DATABASE.put("江苏南京", new RegionClimateData("江苏南京", 
                new BigDecimal("15.8"), new BigDecimal("75"), "PARTIAL_SUN", 
                new String[]{"春季", "夏季", "秋季"}));
        
        // 华中地区
        CLIMATE_DATABASE.put("河南", new RegionClimateData("河南", 
                new BigDecimal("14.5"), new BigDecimal("60"), "FULL_SUN", 
                new String[]{"春季", "秋季", "冬季"}));
        CLIMATE_DATABASE.put("河南郑州", new RegionClimateData("河南郑州", 
                new BigDecimal("14.8"), new BigDecimal("58"), "FULL_SUN", 
                new String[]{"春季", "秋季", "冬季"}));
        
        // 华南地区
        CLIMATE_DATABASE.put("广东", new RegionClimateData("广东", 
                new BigDecimal("22.5"), new BigDecimal("80"), "PARTIAL_SUN", 
                new String[]{"春季", "夏季", "秋季", "冬季"}));
        CLIMATE_DATABASE.put("广西", new RegionClimateData("广西", 
                new BigDecimal("21.5"), new BigDecimal("78"), "PARTIAL_SUN", 
                new String[]{"春季", "夏季", "秋季", "冬季"}));
        
        // 西南地区
        CLIMATE_DATABASE.put("四川", new RegionClimateData("四川", 
                new BigDecimal("16.5"), new BigDecimal("82"), "PARTIAL_SUN", 
                new String[]{"春季", "夏季", "秋季"}));
        CLIMATE_DATABASE.put("云南", new RegionClimateData("云南", 
                new BigDecimal("15.5"), new BigDecimal("70"), "FULL_SUN", 
                new String[]{"春季", "夏季", "秋季", "冬季"}));
        
        // 东北地区
        CLIMATE_DATABASE.put("黑龙江", new RegionClimateData("黑龙江", 
                new BigDecimal("3.5"), new BigDecimal("60"), "FULL_SUN", 
                new String[]{"春季", "夏季"}));
        CLIMATE_DATABASE.put("辽宁", new RegionClimateData("辽宁", 
                new BigDecimal("8.2"), new BigDecimal("62"), "FULL_SUN", 
                new String[]{"春季", "夏季", "秋季"}));
    }

    /**
     * 根据区域名称获取气候数据
     * 支持模糊匹配（如"山东菏泽"可匹配"山东"）
     * 
     * @param region 区域名称
     * @return 气候数据
     */
    public static RegionClimateData getByRegion(String region) {
        if (region == null) {
            return getDefaultClimate();
        }
        
        // 精确匹配
        if (CLIMATE_DATABASE.containsKey(region)) {
            return CLIMATE_DATABASE.get(region);
        }
        
        // 模糊匹配
        for (Map.Entry<String, RegionClimateData> entry : CLIMATE_DATABASE.entrySet()) {
            if (region.contains(entry.getKey()) || entry.getKey().contains(region)) {
                return entry.getValue();
            }
        }
        
        // 返回默认值
        return getDefaultClimate();
    }

    /**
     * 获取默认气候数据
     * 
     * @return 默认气候数据
     */
    private static RegionClimateData getDefaultClimate() {
        return new RegionClimateData("默认", 
                new BigDecimal("15"), new BigDecimal("65"), "PARTIAL_SUN", 
                new String[]{"春季", "秋季"});
    }

    /**
     * 检查区域是否属于某个大区
     * 
     * @param region 具体区域
     * @param majorRegion 大区名称（如"华北"、"华东"）
     * @return 是否属于
     */
    public static boolean belongsToMajorRegion(String region, String majorRegion) {
        if (region == null || majorRegion == null) {
            return false;
        }
        
        Map<String, String[]> majorRegionMap = new HashMap<>();
        majorRegionMap.put("华北", new String[]{"北京", "天津", "河北", "山西", "内蒙古"});
        majorRegionMap.put("华东", new String[]{"上海", "江苏", "浙江", "安徽", "福建", "江西", "山东"});
        majorRegionMap.put("华中", new String[]{"河南", "湖北", "湖南"});
        majorRegionMap.put("华南", new String[]{"广东", "广西", "海南"});
        majorRegionMap.put("西南", new String[]{"重庆", "四川", "贵州", "云南", "西藏"});
        majorRegionMap.put("西北", new String[]{"陕西", "甘肃", "青海", "宁夏", "新疆"});
        majorRegionMap.put("东北", new String[]{"辽宁", "吉林", "黑龙江"});
        
        String[] provinces = majorRegionMap.get(majorRegion);
        if (provinces == null) {
            return false;
        }
        
        for (String province : provinces) {
            if (region.contains(province)) {
                return true;
            }
        }
        return false;
    }
}
