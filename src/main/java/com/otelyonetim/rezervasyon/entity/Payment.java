package com.otelyonetim.rezervasyon.entity;

import com.otelyonetim.rezervasyon.enums.Currency;
import com.otelyonetim.rezervasyon.enums.PaymentMethod;
import com.otelyonetim.rezervasyon.enums.PaymentProvider;
import com.otelyonetim.rezervasyon.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ödeme tutarı
    @Column(nullable = false)
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal amount;

    // Ödeme sağlayıcısı (IYZICO, PAYTR, STRIPE...)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentProvider provider;

    // Provider transaction/session ID
    private String gatewayTransactionId;

    // Provider response (bilgi amaçlı)
    private String gatewayResponseCode;
    private String gatewayResponseMessage;

    // Kart bilgileri (sadece provider’dan gelen maskelenmiş veriler)
    private String cardLastDigits;
    private String cardBrand;

    // Kur
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency = Currency.TRY;

    // Tarih
    @Column(nullable = false)
    private LocalDateTime paymentDate;

    // Ödeme yöntemi (CREDIT_CARD vs.)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    // Ödeme durumu
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // Sistem içi transaction ID
    @Column(unique = true)
    private String transactionId;

    // İade için
    private BigDecimal refundedAmount;
    private String refundReason;
    private LocalDateTime refundDate;
    private String refundTransactionId;

    @Column(columnDefinition = "TEXT")
    private String note;

    @PrePersist
    protected void onCreatePayment() {
        super.onCreate();
        if (paymentDate == null) {
            paymentDate = LocalDateTime.now();
        }
    }

    // İlişki
    @OneToOne
    @JoinColumn(name="reservation_id", nullable = false)
    private Reservation reservation;
}
