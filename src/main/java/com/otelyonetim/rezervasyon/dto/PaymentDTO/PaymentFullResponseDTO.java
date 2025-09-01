package com.otelyonetim.rezervasyon.dto.PaymentDTO;

import com.otelyonetim.rezervasyon.enums.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentFullResponseDTO {
    private Long id;
    private BigDecimal amount;
    private Currency currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private PaymentProvider provider;

    private String transactionId;
    private String gatewayTransactionId;
    private String gatewayResponseCode;
    private String gatewayResponseMessage;

    private Long reservationId;
    private LocalDateTime paymentDate;

    private BigDecimal refundedAmount;
    private String refundReason;
    private LocalDateTime refundDate;
    private String refundTransactionId;

    private String cardHolderName;
    private String cardBrand;
    private String cardLastDigits;
    private LocalDateTime cardExpiryDate;

    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
