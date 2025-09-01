package com.otelyonetim.rezervasyon.dto.PaymentDTO;

import com.otelyonetim.rezervasyon.enums.Currency;
import com.otelyonetim.rezervasyon.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCreateDTO {

    @NotNull(message = "Miktar giriniz!")
    @DecimalMin(value = "0.0", inclusive = false, message = "Miktar sıfırdan büyük olmalıdır!")
    private BigDecimal amount;

    @NotNull(message = "Kur gereklidir!")
    private Currency currency;

    @NotNull(message = "Ödeme yöntemi giriniz!")
    private PaymentMethod paymentMethod;

    @Size(min = 6, max = 40, message = "Kart sahibi ismi 6-40 karakter arasında olmalıdır!")
    private String cardHolderName;

    @Pattern(regexp = "^[0-9]{4}$", message = "Yalnızca 4 rakam kabul edilmektedir!")
    private String cardLastDigits;

    private String cardBrand;

    @FutureOrPresent(message = "Kart son kullanma tarihi geçmiş olamaz!")
    private LocalDateTime cardExpiryDate;

    private String note;
}
