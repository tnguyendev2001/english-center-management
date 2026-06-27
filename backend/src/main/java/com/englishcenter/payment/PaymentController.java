package com.englishcenter.payment;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import com.englishcenter.payment.dto.CancelPaymentRequest;
import com.englishcenter.payment.dto.CreatePaymentRequest;
import com.englishcenter.payment.dto.PaymentResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/api/payments")
    public ApiResponse<List<PaymentResponse>> getPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<PaymentResponse> payments = paymentService.getPayments(page, size);
        PageMeta meta = new PageMeta(
                payments.getNumber(),
                payments.getSize(),
                payments.getTotalElements(),
                payments.getTotalPages()
        );

        return ApiResponse.success(payments.getContent(), meta);
    }

    @PostMapping("/api/invoices/{invoiceId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PaymentResponse> createPayment(
            @PathVariable Long invoiceId,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        return ApiResponse.success(paymentService.createPayment(invoiceId, request));
    }

    @PostMapping("/api/payments/{paymentId}/cancel")
    public ApiResponse<PaymentResponse> cancelPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody CancelPaymentRequest request
    ) {
        return ApiResponse.success(paymentService.cancelPayment(paymentId, request));
    }
}
