package com.otelyonetim.rezervasyon.mapper;

import com.otelyonetim.rezervasyon.dto.PaymentDTO.*;
import com.otelyonetim.rezervasyon.entity.Payment;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class PaymentMapper {

    private PaymentMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    // CreateDTO -> Entity
    public static Payment toEntity(PaymentCreateDTO dto) {
        if (dto == null) return null;
        return Payment.builder()
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .paymentMethod(dto.getPaymentMethod())
                .cardLastDigits(dto.getCardLastDigits())
                .cardBrand(dto.getCardBrand())
                .note(dto.getNote())
                .build();
    }

    // Entity -> FullResponseDTO
    public static PaymentFullResponseDTO toFullDTO(Payment payment) {
        if (payment == null) return null;
        return PaymentFullResponseDTO.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getPaymentStatus())
                .provider(payment.getProvider())
                .transactionId(payment.getTransactionId())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .gatewayResponseCode(payment.getGatewayResponseCode())
                .gatewayResponseMessage(payment.getGatewayResponseMessage())
                .reservationId(payment.getReservation() != null ? payment.getReservation().getId() : null)
                .paymentDate(payment.getPaymentDate())
                .refundedAmount(payment.getRefundedAmount())
                .refundReason(payment.getRefundReason())
                .refundDate(payment.getRefundDate())
                .refundTransactionId(payment.getRefundTransactionId())
                .cardBrand(payment.getCardBrand())
                .cardLastDigits(payment.getCardLastDigits())
                .note(payment.getNote())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    // Entity -> LimitedResponseDTO
    public static PaymentLimitedResponseDTO toLimitedDTO(Payment payment) {
        if (payment == null) return null;
        return PaymentLimitedResponseDTO.builder()
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getPaymentStatus())
                .paymentDate(payment.getPaymentDate())
                .build();
    }

    // UpdateDTO -> Entity
    public static void updateEntityFromDTO(PaymentUpdateDTO dto, Payment payment) {
        if (dto == null || payment == null) return;
        if (dto.getRefundedAmount() != null) payment.setRefundedAmount(dto.getRefundedAmount());
        if (dto.getRefundReason() != null) payment.setRefundReason(dto.getRefundReason());
        if (dto.getNote() != null) payment.setNote(dto.getNote());
    }

    // Entity -> UpdateDTO
    public static PaymentUpdateDTO toUpdateDTO(Payment payment) {
        if (payment == null) return null;
        return PaymentUpdateDTO.builder()
                .refundedAmount(payment.getRefundedAmount())
                .refundReason(payment.getRefundReason())
                .note(payment.getNote())
                .build();
    }

    // List<Payment> -> List<PaymentFullResponseDTO>
    public static List<PaymentFullResponseDTO> toFullDTOList(List<Payment> payments) {
        if (payments == null) return List.of();
        return payments.stream().map(PaymentMapper::toFullDTO).collect(Collectors.toList());
    }

    // List<Payment> -> List<PaymentLimitedResponseDTO>
    public static List<PaymentLimitedResponseDTO> toLimitedDTOList(List<Payment> payments) {
        if (payments == null) return List.of();
        return payments.stream().map(PaymentMapper::toLimitedDTO).collect(Collectors.toList());
    }

    // Optional<Payment> -> Optional<PaymentFullResponseDTO>
    public static Optional<PaymentFullResponseDTO> toFullDTO(Optional<Payment> payment) {
        if (payment == null) return Optional.empty();
        return payment.map(PaymentMapper::toFullDTO);
    }

    // Optional<Payment> -> Optional<PaymentLimitedResponseDTO>
    public static Optional<PaymentLimitedResponseDTO> toLimitedDTO(Optional<Payment> payment) {
        if (payment == null) return Optional.empty();
        return payment.map(PaymentMapper::toLimitedDTO);
    }
}
