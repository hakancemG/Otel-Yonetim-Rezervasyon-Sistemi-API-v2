package com.otelyonetim.rezervasyon.service.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iyzipay.Options;
import com.iyzipay.model.BasketItem;
import com.iyzipay.model.BasketItemType;
import com.iyzipay.model.CheckoutForm;
import com.iyzipay.model.CheckoutFormInitialize;
import com.iyzipay.request.CreateCheckoutFormInitializeRequest;
import com.iyzipay.request.RetrieveCheckoutFormRequest;
import com.otelyonetim.rezervasyon.entity.Payment;
import com.otelyonetim.rezervasyon.enums.PaymentStatus;
import com.otelyonetim.rezervasyon.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.otelyonetim.rezervasyon.exception.ResourceNotFoundException;import com.iyzipay.model.Refund;
import com.iyzipay.request.CreateRefundRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IyzicoPaymentService implements PaymentGatewayService {

    private final PaymentRepository paymentRepository;

    private final Options options;

    // callback URL'ini application.properties / env üzerinden al
    @Value("${iyzico.callback-url}")
    private String callbackUrl;

    @Override
    public Payment initiatePayment(Payment payment) {
        if (payment == null) throw new IllegalArgumentException("Payment cannot be null");
        if (payment.getAmount() == null) throw new IllegalArgumentException("Payment.amount cannot be null");

        CreateCheckoutFormInitializeRequest request = new CreateCheckoutFormInitializeRequest();
        request.setLocale("tr");
        request.setConversationId(payment.getTransactionId());
        request.setPrice(new BigDecimal(payment.getAmount().toPlainString()));
        request.setPaidPrice(new BigDecimal(payment.getAmount().toPlainString()));
        request.setCurrency(payment.getCurrency() == null ? "TRY" : payment.getCurrency().name());
        request.setCallbackUrl(callbackUrl);

        // 🛒 BasketItem ekle
        BasketItem item = new BasketItem();
        item.setId("RESV_" + payment.getReservation().getId());
        item.setName("Otel Rezervasyonu - " + payment.getReservation().getRoom().getRoomNumber());
        item.setCategory1("Konaklama");
        item.setItemType(BasketItemType.VIRTUAL.name());
        item.setPrice(new BigDecimal(payment.getAmount().toPlainString()));

        List<BasketItem> basketItems = new ArrayList<>();
        basketItems.add(item);
        request.setBasketItems(basketItems);

        CheckoutFormInitialize checkoutForm = CheckoutFormInitialize.create(request, options);

        payment.setGatewayResponseMessage(checkoutForm.getStatus());
        payment.setGatewayTransactionId(checkoutForm.getToken()); // müşteriyi yönlendireceğin token

        return payment;
    }


    @Override
    public void handleWebhook(String payload, String signatureHeader) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(payload);

            String token = jsonNode.get("token").asText();

            // İyzico’ya sor
            RetrieveCheckoutFormRequest request = new RetrieveCheckoutFormRequest();
            request.setToken(token);

            CheckoutForm checkoutForm = CheckoutForm.retrieve(request, options);

            String conversationId = checkoutForm.getConversationId();
            Payment payment = paymentRepository.findByTransactionId(conversationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ödeme bulunamadı: " + conversationId));

            if ("success".equalsIgnoreCase(checkoutForm.getStatus())) {
                payment.setPaymentStatus(PaymentStatus.SUCCESS);
                payment.setGatewayTransactionId(checkoutForm.getPaymentId());
            } else {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setGatewayResponseCode(checkoutForm.getErrorCode());
                payment.setGatewayResponseMessage(checkoutForm.getErrorMessage());
            }

            paymentRepository.save(payment);

        } catch (Exception e) {
            throw new RuntimeException("Webhook işlenemedi: " + e.getMessage(), e);
        }
    }

    @Override
    public String refundPayment(Payment payment, BigDecimal amount) throws Exception {
        if (payment == null) {
            throw new IllegalArgumentException("Payment cannot be null");
        }
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Sadece SUCCESS durumundaki ödemeler iade edilebilir");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("İade miktarı pozitif olmalıdır");
        }

        BigDecimal alreadyRefunded = payment.getRefundedAmount() == null ? BigDecimal.ZERO : payment.getRefundedAmount();
        BigDecimal refundableRemaining = payment.getAmount().subtract(alreadyRefunded);
        if (amount.compareTo(refundableRemaining) > 0) {
            throw new IllegalArgumentException("İade miktarı iade edilebilir toplamı aşıyor. Kalan: " + refundableRemaining);
        }

        CreateRefundRequest request = new CreateRefundRequest();
        request.setLocale("tr");
        request.setConversationId(payment.getTransactionId());
        request.setPaymentTransactionId(payment.getGatewayTransactionId()); // iyzico transaction id
        request.setPrice(amount);
        request.setCurrency(payment.getCurrency() != null ? payment.getCurrency().name() : "TRY");
        request.setIp("127.0.0.1");

        Refund refundResponse = Refund.create(request, options);

        if ("success".equalsIgnoreCase(refundResponse.getStatus())) {
            String refundTxId = refundResponse.getPaymentTransactionId();
            payment.setRefundedAmount(alreadyRefunded.add(amount));
            payment.setGatewayResponseMessage("Refund success: " + refundTxId);
            paymentRepository.save(payment);
            return refundTxId;
        } else {
            throw new RuntimeException("Iyzico refund failed: " +
                    refundResponse.getErrorCode() + " - " + refundResponse.getErrorMessage());
        }
    }


}
