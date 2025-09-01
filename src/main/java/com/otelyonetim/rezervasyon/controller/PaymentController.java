package com.otelyonetim.rezervasyon.controller;

import com.otelyonetim.rezervasyon.dto.PaymentDTO.PaymentCreateDTO;
import com.otelyonetim.rezervasyon.dto.PaymentDTO.PaymentFullResponseDTO;
import com.otelyonetim.rezervasyon.dto.PaymentDTO.PaymentLimitedResponseDTO;
import com.otelyonetim.rezervasyon.dto.PaymentDTO.PaymentUpdateDTO;
import com.otelyonetim.rezervasyon.enums.PaymentStatus;
import com.otelyonetim.rezervasyon.service.gateway.PaymentGatewayService;
import com.otelyonetim.rezervasyon.service.payment.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentGatewayService paymentGatewayService;

    // iyzico webhook endpoint
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "x-iyzi-signature", required = false) String signatureHeader) {

        paymentGatewayService.handleWebhook(payload, signatureHeader);
        return ResponseEntity.ok("Webhook received");
    }

    // ödeme başlat (iyzico token’ı ile birlikte dön)
    @PostMapping("/reservation/{reservationId}")
    public ResponseEntity<Map<String, Object>> createPayment(
            @PathVariable Long reservationId,
            @Valid @RequestBody PaymentCreateDTO paymentCreateDTO) {

        PaymentFullResponseDTO createdPayment = paymentService.createPayment(paymentCreateDTO, reservationId);

        // frontend için token ve ödeme detaylarını birlikte dön
        Map<String, Object> response = new HashMap<>();
        response.put("payment", createdPayment);
        response.put("checkoutToken", createdPayment.getGatewayTransactionId()); // iyzico token
        response.put("status", "PENDING");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentFullResponseDTO> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping
    public ResponseEntity<List<PaymentFullResponseDTO>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/limited")
    public ResponseEntity<List<PaymentLimitedResponseDTO>> getAllPaymentsLimited() {
        return ResponseEntity.ok(paymentService.getAllPaymentsLimited());
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentFullResponseDTO> updatePayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentUpdateDTO paymentUpdateDTO) {
        return ResponseEntity.ok(paymentService.updatePayment(id, paymentUpdateDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<List<PaymentFullResponseDTO>> getPaymentsByReservationId(
            @PathVariable Long reservationId) {
        return ResponseEntity.ok(paymentService.getPaymentsByReservationId(reservationId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentFullResponseDTO>> getPaymentsByStatus(@PathVariable PaymentStatus status) {
        return ResponseEntity.ok(paymentService.getPaymentsByStatus(status));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<PaymentFullResponseDTO>> getPaymentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(paymentService.getPaymentsByDateRange(startDate, endDate));
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<PaymentFullResponseDTO> getPaymentByTransactionId(@PathVariable String transactionId) {
        return ResponseEntity.ok(paymentService.getPaymentByTransactionId(transactionId));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentFullResponseDTO> processRefund(
            @PathVariable Long id,
            @RequestParam BigDecimal refundAmount,
            @RequestParam String refundReason) {
        PaymentFullResponseDTO refundedPayment = paymentService.processRefund(id, refundAmount, refundReason);
        return ResponseEntity.ok(refundedPayment);
    }


    @PatchMapping("/{id}/status")
    public ResponseEntity<PaymentFullResponseDTO> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam PaymentStatus status) {
        return ResponseEntity.ok(paymentService.updatePaymentStatus(id, status));
    }

    @GetMapping("/revenue")
    public ResponseEntity<BigDecimal> getTotalRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(paymentService.getTotalRevenue(startDate, endDate));
    }

    @GetMapping("/refunds")
    public ResponseEntity<BigDecimal> getTotalRefunds(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(paymentService.getTotalRefunds(startDate, endDate));
    }
}
