package com.ldd.initialization.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.Order;
import com.ldd.initialization.mapper.OrderMapper;
import com.ldd.initialization.service.impl.PaymentServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentOrderAuthorizationTest {

    private OrderMapper orderMapper;
    private PaymentServiceImpl service;
    private Order order;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), Order.class);
        orderMapper = mock(OrderMapper.class);
        service = new PaymentServiceImpl(null, orderMapper, null, null, null);
        order = new Order();
        order.setOrderNo("ORD-PRIVATE-1");
        order.setUserId(10L);
        order.setStatus(0);
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order);
    }

    @Test
    void anotherUserCannotReadOrder() {
        assertThrows(BizException.class, () -> service.queryOrderStatus("ORD-PRIVATE-1", 20L, false));
    }

    @Test
    void ownerCanReadOrder() {
        assertEquals("ORD-PRIVATE-1", service.queryOrderStatus("ORD-PRIVATE-1", 10L, false).getOrderNo());
    }

    @Test
    void administratorCanReadOrder() {
        assertEquals("ORD-PRIVATE-1", service.queryOrderStatus("ORD-PRIVATE-1", 20L, true).getOrderNo());
    }
}
