package com.otelyonetim.rezervasyon.dto.PaymentDTO;

import com.otelyonetim.rezervasyon.enums.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLimitedResponseDTO {
    private BigDecimal amount;
    private Currency currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private LocalDateTime paymentDate;
}
