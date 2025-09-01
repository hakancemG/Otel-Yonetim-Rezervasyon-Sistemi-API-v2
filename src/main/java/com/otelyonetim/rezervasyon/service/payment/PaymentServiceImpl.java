package com.otelyonetim.rezervasyon.service.payment;

import com.otelyonetim.rezervasyon.dto.PaymentDTO.PaymentCreateDTO;
import com.otelyonetim.rezervasyon.dto.PaymentDTO.PaymentFullResponseDTO;
import com.otelyonetim.rezervasyon.dto.PaymentDTO.PaymentLimitedResponseDTO;
import com.otelyonetim.rezervasyon.dto.PaymentDTO.PaymentUpdateDTO;
import com.otelyonetim.rezervasyon.entity.Payment;
import com.otelyonetim.rezervasyon.entity.Reservation;
import com.otelyonetim.rezervasyon.enums.PaymentProvider;
import com.otelyonetim.rezervasyon.enums.PaymentStatus;
import com.otelyonetim.rezervasyon.exception.ResourceNotFoundException;
import com.otelyonetim.rezervasyon.mapper.PaymentMapper;
import com.otelyonetim.rezervasyon.repository.PaymentRepository;
import com.otelyonetim.rezervasyon.repository.ReservationRepository;
import com.otelyonetim.rezervasyon.service.gateway.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentGatewayService paymentGatewayService;

    @Override
    @Transactional
    public PaymentFullResponseDTO createPayment(PaymentCreateDTO paymentCreateDTO, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdAndNotDeleted(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Rezervasyon bulunamadı. ID: " + reservationId));

        if (paymentCreateDTO.getAmount().compareTo(reservation.getTotalPrice()) != 0) {
            throw new IllegalArgumentException("Ödeme tutarı rezervasyon tutarı ile eşleşmiyor.");
        }

        Payment payment = PaymentMapper.toEntity(paymentCreateDTO);
        payment.setReservation(reservation);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setTransactionId(generateTransactionId());
        payment.setProvider(PaymentProvider.IYZICO); // Kritik: provider atanmalı

        // Önce DB'ye kaydet
        Payment savedPayment = paymentRepository.save(payment);

        // Iyzico'ya yönlendir
        savedPayment = paymentGatewayService.initiatePayment(savedPayment);
        paymentRepository.save(savedPayment); // iyzico’dan dönen bilgileri güncelle

        return PaymentMapper.toFullDTO(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentFullResponseDTO getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ödeme bulunamadı. ID: " + id));
        return PaymentMapper.toFullDTO(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentFullResponseDTO> getAllPayments() {
        List<Payment> payments = paymentRepository.findAll();
        return PaymentMapper.toFullDTOList(payments);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentLimitedResponseDTO> getAllPaymentsLimited() {
        List<Payment> payments = paymentRepository.findAll();
        return PaymentMapper.toLimitedDTOList(payments);
    }

    @Override
    @Transactional
    public PaymentFullResponseDTO updatePayment(Long id, PaymentUpdateDTO paymentUpdateDTO) {
        Payment existingPayment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ödeme bulunamadı. ID: " + id));

        PaymentMapper.updateEntityFromDTO(paymentUpdateDTO, existingPayment);
        Payment updatedPayment = paymentRepository.save(existingPayment);
        return PaymentMapper.toFullDTO(updatedPayment);
    }

    @Override
    @Transactional
    public void deletePayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ödeme bulunamadı. ID: " + id));
        paymentRepository.delete(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentFullResponseDTO> getPaymentsByReservationId(Long reservationId) {
        List<Payment> payments = paymentRepository.findByReservationId(reservationId);
        return PaymentMapper.toFullDTOList(payments);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentFullResponseDTO> getPaymentsByStatus(PaymentStatus status) {
        List<Payment> payments = paymentRepository.findByStatus(status);
        return PaymentMapper.toFullDTOList(payments);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentFullResponseDTO> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Payment> payments = paymentRepository.findByPaymentDateBetween(startDate, endDate);
        return PaymentMapper.toFullDTOList(payments);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentFullResponseDTO getPaymentByTransactionId(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Ödeme bulunamadı. Transaction ID: " + transactionId));
        return PaymentMapper.toFullDTO(payment);
    }

    @Override
    @Transactional
    public PaymentFullResponseDTO processRefund(Long id, BigDecimal refundAmount, String refundReason) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ödeme bulunamadı. ID: " + id));

        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Sadece SUCCESS durumundaki ödemeler iade edilebilir");
        }

        try {
            // iyzico refund çağrısı
            String refundTxId = paymentGatewayService.refundPayment(payment, refundAmount);

            payment.setRefundedAmount(refundAmount);
            payment.setRefundReason(refundReason);
            payment.setRefundDate(LocalDateTime.now());
            payment.setRefundTransactionId(refundTxId);
            payment.setPaymentStatus(PaymentStatus.REFUNDED);

            Payment refundedPayment = paymentRepository.save(payment);
            return PaymentMapper.toFullDTO(refundedPayment);
        } catch (Exception e) {
            throw new RuntimeException("İade başarısız: " + e.getMessage(), e);
        }
    }



    @Override
    @Transactional
    public PaymentFullResponseDTO updatePaymentStatus(Long id, PaymentStatus status) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ödeme bulunamadı. ID: " + id));

        payment.setPaymentStatus(status);
        Payment updatedPayment = paymentRepository.save(payment);
        return PaymentMapper.toFullDTO(updatedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        return paymentRepository.getTotalRevenueBetweenDates(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalRefunds(LocalDateTime startDate, LocalDateTime endDate) {
        return paymentRepository.getTotalRefundsBetweenDates(startDate, endDate);
    }

    private String generateTransactionId() {
        return "TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}