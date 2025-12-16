package xyh.dp.mall.trade.service;

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
import xyh.dp.mall.common.result.Result;
import xyh.dp.mall.trade.dto.CreateOrderDTO;
import xyh.dp.mall.trade.entity.Order;
import xyh.dp.mall.trade.feign.ProductFeignClient;
import xyh.dp.mall.trade.feign.dto.ProductDTO;
import xyh.dp.mall.trade.mapper.OrderMapper;
import xyh.dp.mall.trade.vo.OrderVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * OrderService 订单服务单元测试
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 订单服务测试")
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ProductFeignClient productFeignClient;

    @Mock
    private Executor orderExecutor;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderDTO createOrderDTO;
    private ProductDTO testProduct;
    private Order testOrder;

    /**
     * 初始化测试数据
     */
    @BeforeEach
    void setUp() {
        createOrderDTO = new CreateOrderDTO();
        createOrderDTO.setUserId(1L);
        createOrderDTO.setProductId(1L);
        createOrderDTO.setQuantity(2);
        createOrderDTO.setReceiverName("张三");
        createOrderDTO.setReceiverPhone("13800138000");
        createOrderDTO.setReceiverAddress("山东省济南市历下区xxx街道");
        createOrderDTO.setRemark("尽快发货");

        testProduct = new ProductDTO();
        testProduct.setId(1L);
        testProduct.setName("优质小麦种子");
        testProduct.setMainImage("https://example.com/wheat.jpg");
        testProduct.setPrice(new BigDecimal("25.00"));
        testProduct.setStock(1000);
        testProduct.setStatus("ON_SALE");
        testProduct.setSupplierId(1L);

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNo("ORD202412150001");
        testOrder.setUserId(1L);
        testOrder.setProductId(1L);
        testOrder.setProductName("优质小麦种子");
        testOrder.setProductImage("https://example.com/wheat.jpg");
        testOrder.setPrice(new BigDecimal("25.00"));
        testOrder.setQuantity(2);
        testOrder.setTotalAmount(new BigDecimal("50.00"));
        testOrder.setReceiverName("张三");
        testOrder.setReceiverPhone("13800138000");
        testOrder.setReceiverAddress("山东省济南市历下区xxx街道");
        testOrder.setRemark("尽快发货");
        testOrder.setStatus("PENDING");
        testOrder.setCreateTime(LocalDateTime.now());
        testOrder.setUpdateTime(LocalDateTime.now());
    }

    @Nested
    @DisplayName("getByOrderNo 查询订单测试")
    class GetByOrderNoTest {

        /**
         * 测试订单存在时正常返回
         */
        @Test
        @DisplayName("订单存在时应返回订单详情")
        void getByOrderNo_existingOrder_shouldReturnOrderVO() {
            // Given
            when(orderMapper.selectOne(any())).thenReturn(testOrder);

            // When
            OrderVO result = orderService.getByOrderNo("ORD202412150001");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOrderNo()).isEqualTo("ORD202412150001");
            assertThat(result.getProductName()).isEqualTo("优质小麦种子");
            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(result.getStatus()).isEqualTo("PENDING");
        }

        /**
         * 测试订单不存在时抛出异常
         */
        @Test
        @DisplayName("订单不存在时应抛出BusinessException")
        void getByOrderNo_nonExistingOrder_shouldThrowException() {
            // Given
            when(orderMapper.selectOne(any())).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> orderService.getByOrderNo("ORD999999999999"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("订单不存在");
        }
    }

    @Nested
    @DisplayName("pageQuery 分页查询订单测试")
    class PageQueryTest {

        /**
         * 测试分页查询正常返回
         */
        @Test
        @DisplayName("应正确返回分页订单列表")
        void pageQuery_shouldReturnPagedOrders() {
            // Given
            Page<Order> orderPage = new Page<>(1, 10);
            orderPage.setRecords(Arrays.asList(testOrder));
            orderPage.setTotal(1);
            
            when(orderMapper.selectPage(any(), any())).thenReturn(orderPage);

            // When
            Page<OrderVO> result = orderService.pageQuery(1L, 1, 10, null);

            // Then
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords().get(0).getOrderNo()).isEqualTo("ORD202412150001");
        }

        /**
         * 测试按状态筛选订单
         */
        @Test
        @DisplayName("应支持按状态筛选订单")
        void pageQuery_withStatus_shouldFilterByStatus() {
            // Given
            Page<Order> orderPage = new Page<>(1, 10);
            orderPage.setRecords(Arrays.asList(testOrder));
            orderPage.setTotal(1);
            
            when(orderMapper.selectPage(any(), any())).thenReturn(orderPage);

            // When
            Page<OrderVO> result = orderService.pageQuery(1L, 1, 10, "PENDING");

            // Then
            assertThat(result.getRecords()).hasSize(1);
            verify(orderMapper, times(1)).selectPage(any(), any());
        }

        /**
         * 测试空结果
         */
        @Test
        @DisplayName("无订单时应返回空列表")
        void pageQuery_noOrders_shouldReturnEmptyList() {
            // Given
            Page<Order> orderPage = new Page<>(1, 10);
            orderPage.setRecords(Arrays.asList());
            orderPage.setTotal(0);
            
            when(orderMapper.selectPage(any(), any())).thenReturn(orderPage);

            // When
            Page<OrderVO> result = orderService.pageQuery(1L, 1, 10, null);

            // Then
            assertThat(result.getRecords()).isEmpty();
            assertThat(result.getTotal()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("cancelOrder 取消订单测试")
    class CancelOrderTest {

        /**
         * 测试正常取消待支付订单
         */
        @Test
        @DisplayName("待支付订单应能成功取消")
        void cancelOrder_pendingOrder_shouldSucceed() {
            // Given
            when(orderMapper.selectOne(any())).thenReturn(testOrder);
            when(orderMapper.updateById((Order)any())).thenReturn(1);
            when(productFeignClient.restoreStock(anyLong(), anyInt())).thenReturn(Result.success(true));

            // When
            orderService.cancelOrder("ORD202412150001", 1L);

            // Then
            verify(orderMapper, times(1)).updateById((Order)any());
        }

        /**
         * 测试取消不存在的订单
         */
        @Test
        @DisplayName("订单不存在时应抛出BusinessException")
        void cancelOrder_nonExistingOrder_shouldThrowException() {
            // Given
            when(orderMapper.selectOne(any())).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> orderService.cancelOrder("ORD999999999999", 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("订单不存在");
        }

        /**
         * 测试取消非待支付订单
         */
        @Test
        @DisplayName("非待支付订单不能取消")
        void cancelOrder_nonPendingOrder_shouldThrowException() {
            // Given
            testOrder.setStatus("PAID");
            when(orderMapper.selectOne(any())).thenReturn(testOrder);

            // When/Then
            assertThatThrownBy(() -> orderService.cancelOrder("ORD202412150001", 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("只能取消待支付订单");
        }
    }

    @Nested
    @DisplayName("OrderVO 转换测试")
    class OrderVOConversionTest {

        /**
         * 测试订单VO包含所有字段
         */
        @Test
        @DisplayName("OrderVO应包含所有订单信息")
        void orderVO_shouldContainAllFields() {
            // Given
            when(orderMapper.selectOne(any())).thenReturn(testOrder);

            // When
            OrderVO result = orderService.getByOrderNo("ORD202412150001");

            // Then
            assertThat(result.getOrderNo()).isEqualTo("ORD202412150001");
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getProductId()).isEqualTo(1L);
            assertThat(result.getProductName()).isEqualTo("优质小麦种子");
            assertThat(result.getProductImage()).isEqualTo("https://example.com/wheat.jpg");
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("25.00"));
            assertThat(result.getQuantity()).isEqualTo(2);
            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(result.getReceiverName()).isEqualTo("张三");
            assertThat(result.getReceiverPhone()).isEqualTo("13800138000");
            assertThat(result.getReceiverAddress()).isEqualTo("山东省济南市历下区xxx街道");
            assertThat(result.getRemark()).isEqualTo("尽快发货");
            assertThat(result.getStatus()).isEqualTo("PENDING");
        }
    }

    @Nested
    @DisplayName("CreateOrderDTO 验证测试")
    class CreateOrderDTOTest {

        /**
         * 测试DTO字段设置
         */
        @Test
        @DisplayName("CreateOrderDTO应正确设置所有字段")
        void createOrderDTO_shouldSetAllFields() {
            // Given
            CreateOrderDTO dto = new CreateOrderDTO();
            dto.setUserId(1L);
            dto.setProductId(2L);
            dto.setQuantity(5);
            dto.setReceiverName("李四");
            dto.setReceiverPhone("13900139000");
            dto.setReceiverAddress("北京市朝阳区xxx");
            dto.setRemark("周末送货");

            // Then
            assertThat(dto.getUserId()).isEqualTo(1L);
            assertThat(dto.getProductId()).isEqualTo(2L);
            assertThat(dto.getQuantity()).isEqualTo(5);
            assertThat(dto.getReceiverName()).isEqualTo("李四");
            assertThat(dto.getReceiverPhone()).isEqualTo("13900139000");
            assertThat(dto.getReceiverAddress()).isEqualTo("北京市朝阳区xxx");
            assertThat(dto.getRemark()).isEqualTo("周末送货");
        }
    }

    @Nested
    @DisplayName("订单状态测试")
    class OrderStatusTest {

        /**
         * 测试订单状态常量
         */
        @Test
        @DisplayName("应支持多种订单状态")
        void order_shouldSupportMultipleStatuses() {
            // Given
            String[] validStatuses = {"PENDING", "PAID", "SHIPPED", "COMPLETED", "CANCELLED"};

            // Then
            for (String status : validStatuses) {
                Order order = new Order();
                order.setStatus(status);
                assertThat(order.getStatus()).isEqualTo(status);
            }
        }
    }

    @Nested
    @DisplayName("金额计算测试")
    class AmountCalculationTest {

        /**
         * 测试订单总金额计算
         */
        @Test
        @DisplayName("总金额应等于单价乘以数量")
        void totalAmount_shouldEqualPriceTimesQuantity() {
            // Given
            BigDecimal price = new BigDecimal("25.00");
            int quantity = 3;
            BigDecimal expectedTotal = new BigDecimal("75.00");

            // When
            BigDecimal actualTotal = price.multiply(new BigDecimal(quantity));

            // Then
            assertThat(actualTotal).isEqualByComparingTo(expectedTotal);
        }

        /**
         * 测试大数量订单金额计算
         */
        @Test
        @DisplayName("大数量订单金额计算应正确")
        void totalAmount_largeQuantity_shouldCalculateCorrectly() {
            // Given
            BigDecimal price = new BigDecimal("99.99");
            int quantity = 1000;
            BigDecimal expectedTotal = new BigDecimal("99990.00");

            // When
            BigDecimal actualTotal = price.multiply(new BigDecimal(quantity));

            // Then
            assertThat(actualTotal).isEqualByComparingTo(expectedTotal);
        }
    }
}
