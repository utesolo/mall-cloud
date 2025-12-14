package xyh.dp.mall.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyh.dp.mall.common.exception.BusinessException;
import xyh.dp.mall.product.entity.Category;
import xyh.dp.mall.product.entity.Product;
import xyh.dp.mall.product.mapper.CategoryMapper;
import xyh.dp.mall.product.mapper.ProductMapper;
import xyh.dp.mall.product.vo.ProductVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ProductService 商品服务单元测试
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 商品服务测试")
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private Category testCategory;

    /**
     * 初始化测试数据
     */
    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("优质小麦种子");
        testProduct.setCategoryId(1L);
        testProduct.setMainImage("https://example.com/wheat.jpg");
        testProduct.setDescription("高产抗病小麦品种");
        testProduct.setSpecification("1kg/袋");
        testProduct.setPrice(new BigDecimal("25.00"));
        testProduct.setStock(1000);
        testProduct.setSales(500);
        testProduct.setSupplierId(1L);
        testProduct.setStatus("ON_SALE");
        testProduct.setOrigin("山东济南");
        testProduct.setVariety("济麦22");
        testProduct.setDifficulty("EASY");
        testProduct.setGrowthCycle(240);
        testProduct.setGerminationRate(new BigDecimal("95"));
        testProduct.setPurity(new BigDecimal("99"));
        testProduct.setShelfLife(12);
        testProduct.setProductionDate(LocalDate.now().minusMonths(1));
        testProduct.setMinTemperature(new BigDecimal("10"));
        testProduct.setMaxTemperature(new BigDecimal("25"));
        testProduct.setMinHumidity(new BigDecimal("40"));
        testProduct.setMaxHumidity(new BigDecimal("70"));
        testProduct.setLightRequirement("FULL_SUN");
        testProduct.setImages("[\"img1.jpg\",\"img2.jpg\"]");
        testProduct.setRegions("[\"华北\",\"华东\"]");
        testProduct.setPlantingSeasons("[\"春季\",\"秋季\"]");

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("粮食作物");
        testCategory.setStatus("NORMAL");
        testCategory.setSort(1);
    }

    @Nested
    @DisplayName("getById 根据ID查询商品测试")
    class GetByIdTest {

        /**
         * 测试商品存在时正常返回
         */
        @Test
        @DisplayName("商品存在时应返回商品详情")
        void getById_existingProduct_shouldReturnProductVO() {
            // Given
            when(productMapper.selectById(1L)).thenReturn(testProduct);
            when(categoryMapper.selectById(1L)).thenReturn(testCategory);

            // When
            ProductVO result = productService.getById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("优质小麦种子");
            assertThat(result.getCategoryName()).isEqualTo("粮食作物");
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("25.00"));
            assertThat(result.getStock()).isEqualTo(1000);
            assertThat(result.getVariety()).isEqualTo("济麦22");
            assertThat(result.getGerminationRate()).isEqualByComparingTo(new BigDecimal("95"));
        }

        /**
         * 测试商品不存在时抛出异常
         */
        @Test
        @DisplayName("商品不存在时应抛出BusinessException")
        void getById_nonExistingProduct_shouldThrowException() {
            // Given
            when(productMapper.selectById(999L)).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> productService.getById(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("商品不存在");
        }
    }

    @Nested
    @DisplayName("getCategoryList 分类列表查询测试")
    class GetCategoryListTest {

        /**
         * 测试正常返回分类列表
         */
        @Test
        @DisplayName("应返回正常状态的分类列表")
        void getCategoryList_shouldReturnNormalCategories() {
            // Given
            Category category1 = new Category();
            category1.setId(1L);
            category1.setName("粮食作物");
            category1.setStatus("NORMAL");
            category1.setSort(1);

            Category category2 = new Category();
            category2.setId(2L);
            category2.setName("蔬菜种子");
            category2.setStatus("NORMAL");
            category2.setSort(2);

            when(categoryMapper.selectList(any())).thenReturn(Arrays.asList(category1, category2));

            // When
            List<Category> result = productService.getCategoryList();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("粮食作物");
            assertThat(result.get(1).getName()).isEqualTo("蔬菜种子");
        }
    }

    @Nested
    @DisplayName("deductStock 扣减库存测试")
    class DeductStockTest {

        /**
         * 测试库存充足时正常扣减
         */
        @Test
        @DisplayName("库存充足时应成功扣减")
        void deductStock_sufficientStock_shouldSucceed() {
            // Given
            when(productMapper.selectById(1L)).thenReturn(testProduct);
            when(productMapper.deductStock(eq(1L), eq(10), eq(1000))).thenReturn(1);

            // When
            boolean result = productService.deductStock(1L, 10);

            // Then
            assertThat(result).isTrue();
            verify(productMapper, times(1)).deductStock(1L, 10, 1000);
        }

        /**
         * 测试库存不足时抛出异常
         */
        @Test
        @DisplayName("库存不足时应抛出BusinessException")
        void deductStock_insufficientStock_shouldThrowException() {
            // Given
            testProduct.setStock(5);
            when(productMapper.selectById(1L)).thenReturn(testProduct);

            // When/Then
            assertThatThrownBy(() -> productService.deductStock(1L, 10))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("库存不足");
        }

        /**
         * 测试商品不存在时抛出异常
         */
        @Test
        @DisplayName("商品不存在时应抛出BusinessException")
        void deductStock_nonExistingProduct_shouldThrowException() {
            // Given
            when(productMapper.selectById(999L)).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> productService.deductStock(999L, 10))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("商品不存在");
        }

        /**
         * 测试并发扣减失败时抛出异常
         */
        @Test
        @DisplayName("乐观锁更新失败时应抛出BusinessException")
        void deductStock_optimisticLockFailed_shouldThrowException() {
            // Given
            when(productMapper.selectById(1L)).thenReturn(testProduct);
            when(productMapper.deductStock(eq(1L), eq(10), eq(1000))).thenReturn(0);

            // When/Then
            assertThatThrownBy(() -> productService.deductStock(1L, 10))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("库存不足或已被他人购买");
        }
    }

    @Nested
    @DisplayName("restoreStock 恢复库存测试")
    class RestoreStockTest {

        /**
         * 测试正常恢复库存
         */
        @Test
        @DisplayName("商品存在时应成功恢复库存")
        void restoreStock_existingProduct_shouldSucceed() {
            // Given
            when(productMapper.selectById(1L)).thenReturn(testProduct);
            when(productMapper.updateById(any())).thenReturn(1);

            // When
            boolean result = productService.restoreStock(1L, 10);

            // Then
            assertThat(result).isTrue();
            verify(productMapper, times(1)).updateById(any());
        }

        /**
         * 测试商品不存在时返回false
         */
        @Test
        @DisplayName("商品不存在时应返回false")
        void restoreStock_nonExistingProduct_shouldReturnFalse() {
            // Given
            when(productMapper.selectById(999L)).thenReturn(null);

            // When
            boolean result = productService.restoreStock(999L, 10);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("increaseSales 增加销量测试")
    class IncreaseSalesTest {

        /**
         * 测试正常增加销量
         */
        @Test
        @DisplayName("商品存在时应成功增加销量")
        void increaseSales_existingProduct_shouldSucceed() {
            // Given
            when(productMapper.selectById(1L)).thenReturn(testProduct);
            when(productMapper.updateById(any())).thenReturn(1);

            // When
            boolean result = productService.increaseSales(1L, 5);

            // Then
            assertThat(result).isTrue();
            verify(productMapper, times(1)).updateById(any());
        }

        /**
         * 测试销量为null时的初始化
         */
        @Test
        @DisplayName("销量为null时应从0开始增加")
        void increaseSales_nullSales_shouldStartFromZero() {
            // Given
            testProduct.setSales(null);
            when(productMapper.selectById(1L)).thenReturn(testProduct);
            when(productMapper.updateById(any())).thenReturn(1);

            // When
            boolean result = productService.increaseSales(1L, 5);

            // Then
            assertThat(result).isTrue();
        }

        /**
         * 测试商品不存在时返回false
         */
        @Test
        @DisplayName("商品不存在时应返回false")
        void increaseSales_nonExistingProduct_shouldReturnFalse() {
            // Given
            when(productMapper.selectById(999L)).thenReturn(null);

            // When
            boolean result = productService.increaseSales(999L, 5);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("ProductVO 转换测试")
    class ProductVOConversionTest {

        /**
         * 测试JSON字段解析
         */
        @Test
        @DisplayName("应正确解析JSON字段为列表")
        void convertToVO_shouldParseJsonFields() {
            // Given
            when(productMapper.selectById(1L)).thenReturn(testProduct);
            when(categoryMapper.selectById(1L)).thenReturn(testCategory);

            // When
            ProductVO result = productService.getById(1L);

            // Then
            assertThat(result.getImages()).hasSize(2);
            assertThat(result.getImages()).contains("img1.jpg", "img2.jpg");
            assertThat(result.getRegions()).hasSize(2);
            assertThat(result.getRegions()).contains("华北", "华东");
            assertThat(result.getPlantingSeasons()).hasSize(2);
            assertThat(result.getPlantingSeasons()).contains("春季", "秋季");
        }

        /**
         * 测试空JSON字段处理
         */
        @Test
        @DisplayName("空JSON字段应返回空列表")
        void convertToVO_emptyJsonFields_shouldReturnEmptyLists() {
            // Given
            testProduct.setImages(null);
            testProduct.setRegions("");
            testProduct.setPlantingSeasons(null);
            when(productMapper.selectById(1L)).thenReturn(testProduct);
            when(categoryMapper.selectById(1L)).thenReturn(testCategory);

            // When
            ProductVO result = productService.getById(1L);

            // Then
            assertThat(result.getImages()).isEmpty();
            assertThat(result.getRegions()).isEmpty();
            assertThat(result.getPlantingSeasons()).isEmpty();
        }

        /**
         * 测试分类不存在时分类名称为null
         */
        @Test
        @DisplayName("分类不存在时分类名称应为null")
        void convertToVO_noCategoryFound_shouldHaveNullCategoryName() {
            // Given
            when(productMapper.selectById(1L)).thenReturn(testProduct);
            when(categoryMapper.selectById(1L)).thenReturn(null);

            // When
            ProductVO result = productService.getById(1L);

            // Then
            assertThat(result.getCategoryName()).isNull();
        }
    }

    @Nested
    @DisplayName("种子特有属性测试")
    class SeedPropertiesTest {

        /**
         * 测试种子属性正确转换
         */
        @Test
        @DisplayName("应正确转换种子特有属性")
        void productVO_shouldContainSeedProperties() {
            // Given
            when(productMapper.selectById(1L)).thenReturn(testProduct);
            when(categoryMapper.selectById(1L)).thenReturn(testCategory);

            // When
            ProductVO result = productService.getById(1L);

            // Then
            assertThat(result.getOrigin()).isEqualTo("山东济南");
            assertThat(result.getVariety()).isEqualTo("济麦22");
            assertThat(result.getDifficulty()).isEqualTo("EASY");
            assertThat(result.getGrowthCycle()).isEqualTo(240);
            assertThat(result.getGerminationRate()).isEqualByComparingTo(new BigDecimal("95"));
            assertThat(result.getPurity()).isEqualByComparingTo(new BigDecimal("99"));
            assertThat(result.getShelfLife()).isEqualTo(12);
            assertThat(result.getMinTemperature()).isEqualByComparingTo(new BigDecimal("10"));
            assertThat(result.getMaxTemperature()).isEqualByComparingTo(new BigDecimal("25"));
            assertThat(result.getLightRequirement()).isEqualTo("FULL_SUN");
        }
    }
}
