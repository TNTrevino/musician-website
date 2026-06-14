package com.project.backend.services;

import com.project.backend.exceptions.UnprocessableEventException;
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
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderFulfillmentService {
  private static final Logger logger = LoggerFactory.getLogger(OrderFulfillmentService.class);
  private static final SecureRandom RANDOM = new SecureRandom();

  @Value("${DOWNLOAD_TOKEN_TTL_DAYS:30}")
  private long tokenTtlDays;

  @Autowired OrderRepository orderRepository;
  @Autowired PiecesRepository piecesRepository;
  @Autowired PurchaseEmailService purchaseEmailService;

  /**
   * Idempotent fulfillment: finds an existing order or creates one, then ensures the lazy
   * {@code items} collection is initialized before the transaction closes. Called by both the
   * Stripe webhook and the success-page confirm endpoint; the unique constraint on
   * stripe_session_id settles any concurrent race.
   *
   * <p>This method is {@code @Transactional} so the find-or-create and the lazy {@code getItems()}
   * read happen inside a single persistence session. Email send MUST happen after this transaction
   * commits — callers should invoke {@link #sendEmailIfNeeded} on the returned Order outside this
   * transactional boundary. Because callers call this method through the Spring bean proxy,
   * {@code @Transactional} is fully honored (no self-invocation bypass).
   */
  @Transactional
  public Order fulfill(Session session) {
    Order order =
        orderRepository
            .findByStripeSessionId(session.getId())
            .orElseGet(() -> createOrder(session));

    // Touch the lazy collection inside the transaction so items are initialized
    // and available on the returned (possibly detached) object after commit.
    order.getItems().size();

    return order;
  }

  /**
   * Claims and sends the download email if it has not already been sent. The atomic
   * {@code markEmailSent} guard ensures exactly one caller wins the claim, even under the
   * concurrent webhook-vs-redirect race. This method intentionally runs OUTSIDE any open
   * transaction so a slow SMTP server never holds a DB connection open.
   */
  public void sendEmailIfNeeded(Order order) {
    // the atomic markEmailSent guard means only one caller can claim the send
    if (orderRepository.markEmailSent(order.getId()) != 1) {
      return;
    }

    try {
      purchaseEmailService.sendDownloadEmail(order);
    } catch (Exception ex) {
      // release the claim so a webhook retry can attempt the send again;
      // the buyer still gets their downloads on the success page either way
      logger.error("Download email failed for order {}: {}", order.getId(), ex.getMessage(), ex);
      orderRepository.resetEmailSent(order.getId());
    }
  }

  private Order createOrder(Session session) {
    if (!"paid".equals(session.getPaymentStatus())) {
      logger.warn("Refusing to fulfill unpaid session {}", session.getId());
      // Terminal: payment status will not change retroactively; retrying is pointless.
      throw new UnprocessableEventException("Session is not paid: " + session.getId());
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
      // Terminal: metadata is set at checkout creation time and will not change on retry.
      throw new UnprocessableEventException(
          "Session " + session.getId() + " has no piece_ids metadata");
    }

    List<Long> ids;
    try {
      ids = Arrays.stream(pieceIds.split(",")).map(Long::parseLong).toList();
    } catch (NumberFormatException ex) {
      logger.error(
          "Session {} has malformed piece_ids metadata '{}': {}",
          session.getId(),
          pieceIds,
          ex.getMessage());
      // Terminal: malformed metadata will never parse correctly on retry.
      throw new UnprocessableEventException(
          "Session " + session.getId() + " has malformed piece_ids metadata: " + pieceIds, ex);
    }

    List<Piece> pieces = piecesRepository.findAllById(ids);

    if (pieces.size() != ids.size()) {
      logger.error("Session {} references pieces missing from the DB: {}", session.getId(), ids);
      // Terminal: piece ids embedded in metadata will not change; missing pieces won't appear.
      throw new UnprocessableEventException(
          "Session " + session.getId() + " references unknown pieces: " + ids);
    }

    return pieces;
  }

  private static String generateToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
