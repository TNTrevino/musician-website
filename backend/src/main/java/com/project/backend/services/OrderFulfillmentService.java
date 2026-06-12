package com.project.backend.services;

import com.project.backend.models.Order;
import com.project.backend.models.OrderItem;
import com.project.backend.models.OrderStatus;
import com.project.backend.models.Piece;
import com.project.backend.repositories.OrderRepository;
import com.project.backend.repositories.PiecesRepository;
import com.stripe.model.checkout.Session;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class OrderFulfillmentService {
  private static final Logger logger = LoggerFactory.getLogger(OrderFulfillmentService.class);
  private static final SecureRandom RANDOM = new SecureRandom();

  @Value("${DOWNLOAD_TOKEN_TTL_DAYS:30}")
  private long tokenTtlDays;

  @Autowired OrderRepository orderRepository;
  @Autowired PiecesRepository piecesRepository;

  /**
   * Idempotent fulfillment entry point, called by both the Stripe webhook and the success-page
   * confirm endpoint. Whichever arrives first creates the order; the unique constraint on
   * stripe_session_id settles a concurrent race.
   */
  public Order fulfill(Session session) {
    Order order =
        orderRepository
            .findByStripeSessionId(session.getId())
            .orElseGet(() -> createOrder(session));

    sendEmailIfNeeded(order);
    return order;
  }

  private Order createOrder(Session session) {
    if (!"paid".equals(session.getPaymentStatus())) {
      logger.warn("Refusing to fulfill unpaid session {}", session.getId());
      throw new IllegalStateException("Session is not paid");
    }

    List<Piece> pieces = parsePieces(session);

    Order order =
        new Order()
            .setStripeSessionId(session.getId())
            .setBuyerEmail(
                session.getCustomerDetails() != null
                    ? session.getCustomerDetails().getEmail()
                    : null)
            .setStatus(OrderStatus.FULFILLED)
            .setAmountTotal(session.getAmountTotal())
            .setCurrency(session.getCurrency())
            .setDownloadToken(generateToken())
            .setTokenExpiresAt(Instant.now().plus(Duration.ofDays(tokenTtlDays)))
            .setFulfilledAt(Instant.now());

    pieces.forEach(
        piece ->
            order
                .getItems()
                .add(
                    new OrderItem()
                        .setOrder(order)
                        .setPiece(piece)
                        .setUnitAmount(Math.round(piece.getPrice() * 100))));

    try {
      Order saved = orderRepository.save(order);
      logger.info("Fulfilled session {} as order {}", session.getId(), saved.getId());
      return saved;
    } catch (DataIntegrityViolationException ex) {
      // lost the webhook-vs-redirect race; the winner's row is the order
      logger.info("Session {} was fulfilled concurrently, reusing existing order", session.getId());
      return orderRepository
          .findByStripeSessionId(session.getId())
          .orElseThrow(() -> new IllegalStateException("Order vanished after race", ex));
    }
  }

  private List<Piece> parsePieces(Session session) {
    String pieceIds =
        session.getMetadata() != null ? session.getMetadata().get("piece_ids") : null;
    if (pieceIds == null || pieceIds.isBlank()) {
      logger.error("Session {} has no piece_ids metadata", session.getId());
      throw new IllegalStateException("Session has no piece_ids metadata");
    }

    List<Long> ids = Arrays.stream(pieceIds.split(",")).map(Long::parseLong).toList();
    List<Piece> pieces = piecesRepository.findAllById(ids);

    if (pieces.size() != ids.size()) {
      logger.error("Session {} references pieces missing from the DB: {}", session.getId(), ids);
      throw new IllegalStateException("Session references unknown pieces");
    }

    return pieces;
  }

  private void sendEmailIfNeeded(Order order) {
    // the atomic markEmailSent guard means only one caller can claim the send
    if (orderRepository.markEmailSent(order.getId()) != 1) {
      return;
    }

    // TODO(purchase-email): send the download-links email here; on failure call
    // orderRepository.resetEmailSent(order.getId()) so a webhook retry can try again
    logger.info("Claimed email send for order {}", order.getId());
  }

  private static String generateToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
