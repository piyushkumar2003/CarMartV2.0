package com.airline.payment.controller;

import com.airline.payment.entity.Payment;
import com.airline.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;

    @PostMapping("/process")
    public boolean processPayment(@RequestParam Long bookingId,
            @RequestParam Double amount,
            @RequestParam(defaultValue = "CARD") String mode) {

        System.out.println("Processing payment for Booking: " + bookingId + " Amount: $" + amount + " Mode: " + mode);

        // Mock Payment Logic
        // Simulate latency
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        // Mock Failure Scenario (e.g. random 10% failure)
        boolean success = new Random().nextInt(10) > 0;

        Payment payment = new Payment(bookingId, amount, mode);
        payment.setTransactionId(UUID.randomUUID().toString());

        if (success) {
            payment.setStatus("SUCCESS");
            System.out.println("Payment SUCCESS. Txn: " + payment.getTransactionId());
        } else {
            payment.setStatus("FAILED");
            System.out.println("Payment FAILED for Booking: " + bookingId);
        }

        paymentRepository.save(payment);
        return success;
    }

    @GetMapping("/{bookingId}")
    public java.util.List<Payment> getPayments(@PathVariable Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }
}
