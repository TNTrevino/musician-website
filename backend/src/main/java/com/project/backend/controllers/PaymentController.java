package com.project.backend.controllers;

import com.project.backend.DTOs.OrderConfirmationDTO;
import com.project.backend.DTOs.PaymentRequestDTO;
import com.project.backend.DTOs.PaymentResponseDTO;
import com.project.backend.services.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment")
public class PaymentController {
  private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

  @Autowired PaymentService paymentService;

  @PostMapping("/checkout")
  public ResponseEntity<PaymentResponseDTO> checkoutProducts(
      @RequestBody PaymentRequestDTO request) {
    logger.info("Checkout endpoint called with {} products", request.getProducts().size());
    PaymentResponseDTO response = paymentService.checkoutProducts(request);
    if ("SUCCESS".equals(response.getStatus())) {
      logger.info("Checkout successful, session: {}", response.getSessionId());
    } else {
      logger.warn("Checkout failed: {}", response.getMessage());
    }
    return ResponseEntity.ok(response);
  }

  @GetMapping("/confirm")
  public ResponseEntity<OrderConfirmationDTO> confirm(
      @RequestParam("session_id") String sessionId) {
    logger.info("Payment confirmation for session: {}", sessionId);
    OrderConfirmationDTO response = paymentService.confirmSession(sessionId);
    logger.info("Payment confirmation status: {}", response.getStatus());
    return ResponseEntity.ok(response);
  }
}
