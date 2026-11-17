package com.securecare.domain.model;

import com.securecare.domain.enums.InvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("billing")
@CompoundIndex(name = "patient_invoice_status_idx", def = "{'patientId': 1, 'status': 1}")
public record BillingDocument(
        @Id String id,
        @Indexed(unique = true) String invoiceId,
        String patientId,
        String appointmentId,
        List<InvoiceItem> items,
        BigDecimal totalAmount,
        String currency,
        InvoiceStatus status,
        Instant issuedAt,
        Instant paidAt,
        String paymentMethod) {
    public record InvoiceItem(String description, BigDecimal amount) { }
}
