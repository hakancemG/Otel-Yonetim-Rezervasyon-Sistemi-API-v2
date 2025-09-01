package com.otelyonetim.rezervasyon.service.gateway;

import com.otelyonetim.rezervasyon.entity.Payment;

import java.math.BigDecimal;

public interface PaymentGatewayService {

    // Ödeme başlat (checkout/session oluştur)
    Payment initiatePayment(Payment payment);

    // Webhook handle et
    void handleWebhook(String payload, String signatureHeader);

    String refundPayment(Payment payment, BigDecimal amount) throws Exception;
}
