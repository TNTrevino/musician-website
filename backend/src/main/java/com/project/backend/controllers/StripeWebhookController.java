package com.project.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.services.OrderFulfillmentService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment")
public class StripeWebhookController {
  private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

  @Value("${STRIPE_WEBHOOK_SECRET}")
  private String webhookSecret;

  @Value("${STRIPE_SECRET}")
  private String stripeSecret;

  @Autowired OrderFulfillmentService fulfillmentService;

  // the body must stay the raw String Stripe sent; signature verification
  // fails on re-serialized JSON
  @PostMapping("/webhook")
  public ResponseEntity<String> handleWebhook(
      @RequestBody String payload, @RequestHeader("Stripe-Signature") String signature) {
    Event event;
    try {
      event = Webhook.constructEvent(payload, signature, webhookSecret);
    } catch (SignatureVerificationException ex) {
      logger.warn("Rejected webhook with invalid signature: {}", ex.getMessage());
      return ResponseEntity.badRequest().body("Invalid signature");
    }

    if (!"checkout.session.completed".equals(event.getType())) {
      logger.debug("Ignoring webhook event type: {}", event.getType());
      return ResponseEntity.ok("Ignored");
    }

    Session session;
    try {
      session = extractSession(event);
    } catch (StripeException ex) {
      logger.error("Failed to load session for event {}: {}", event.getId(), ex.getMessage());
      // non-2xx so Stripe retries the event later
      return ResponseEntity.internalServerError().body("Failed to load session");
    }

    logger.info("Webhook received checkout.session.completed for session {}", session.getId());
    try {
      fulfillmentService.fulfill(session);
    } catch (Exception ex) {
      logger.error("Fulfillment failed for session {}: {}", session.getId(), ex.getMessage(), ex);
      return ResponseEntity.internalServerError().body("Fulfillment failed");
    }

    return ResponseEntity.ok("Fulfilled");
  }

  private Session extractSession(Event event) throws StripeException {
    EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

    if (deserializer.getObject().orElse(null) instanceof Session session) {
      return session;
    }

    // deserialization fails when the SDK's pinned API version drifts from the
    // event's; fall back to re-fetching the session by id from the raw payload
    logger.warn("Event {} payload did not deserialize, retrieving session by id", event.getId());
    Stripe.apiKey = stripeSecret;
    String sessionId;
    try {
      sessionId = new ObjectMapper().readTree(deserializer.getRawJson()).get("id").asText();
    } catch (Exception ex) {
      throw new IllegalStateException("Could not extract session id from event " + event.getId());
    }
    return Session.retrieve(sessionId);
  }
}
