package com.ldd.initialization.controller;

import com.ldd.initialization.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class PaymentControllerReturnTest {

    @Test
    void alipayReturnRedirectsToFrontendWithoutReturningOrderData() {
        PaymentService paymentService = mock(PaymentService.class);
        PaymentController controller = new PaymentController(paymentService);
        ReflectionTestUtils.setField(controller, "frontendUrl", "http://localhost:3000/");

        var response = controller.alipayReturn("ORD-123");

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertTrue(response.getHeaders().getLocation().toString()
                .startsWith("http://localhost:3000/payment/result?out_trade_no=ORD-123"));
        verifyNoInteractions(paymentService);
    }
}
