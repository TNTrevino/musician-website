package com.project.backend.services;

import com.project.backend.DTOs.DownloadItemDTO;
import com.project.backend.DTOs.OrderConfirmationDTO;
import com.project.backend.DTOs.PaymentRequestDTO;
import com.project.backend.DTOs.PaymentResponseDTO;
import com.project.backend.models.Order;
import com.project.backend.models.OrderItem;
import com.project.backend.models.Piece;
import com.project.backend.repositories.PiecesRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
  private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

  @Value("${STRIPE_SECRET}")
  private String stripeSecret;

  @Value("${FRONTEND_URL}")
  private String baseUrl;

  // Stripe metadata values are capped at 500 characters
  private static final int MAX_CART_PIECES = 50;

  @Autowired PiecesRepository piecesRepository;
  @Autowired OrderFulfillmentService fulfillmentService;

  public PaymentResponseDTO checkoutProducts(PaymentRequestDTO paymentRequest) {
    logger.info("Initiating payment checkout for {} products", paymentRequest.getProducts().size());
    Stripe.apiKey = stripeSecret;

    if (paymentRequest.getProducts().size() > MAX_CART_PIECES) {
      logger.warn("Checkout rejected: cart has {} pieces", paymentRequest.getProducts().size());
      return PaymentResponseDTO.builder()
          .status("FAILED")
          .message("Too many pieces in cart")
          .build();
    }

    List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
    List<Long> pieceIds = new ArrayList<>();

    paymentRequest.getProducts().stream()
        .forEach(
            dto -> {
              logger.debug("Looking for piece with id: {}", dto.getId());
              Piece piece =
                  piecesRepository
                      .findById(dto.getId())
                      .orElseThrow(() -> new RuntimeException("Piece not found: " + dto.getId()));

              if (piece.getFileName() == null || piece.getFileName().isBlank()) {
                logger.error("Piece {} has no file and cannot be sold", piece.getId());
                throw new RuntimeException("Piece is not available for purchase");
              }

              SessionCreateParams.LineItem.PriceData.ProductData productData =
                  SessionCreateParams.LineItem.PriceData.ProductData.builder()
                      .setName(piece.getTitle())
                      .build();

              SessionCreateParams.LineItem.PriceData priceData =
                  SessionCreateParams.LineItem.PriceData.builder()
                      .setCurrency("usd")
                      .setUnitAmount(Math.round(piece.getPrice() * 100))
                      .setProductData(productData)
                      .build();

              SessionCreateParams.LineItem lineItem =
                  SessionCreateParams.LineItem.builder()
                      .setQuantity(1L)
                      .setPriceData(priceData)
                      .build();

              lineItems.add(lineItem);
              pieceIds.add(piece.getId());
            });

    SessionCreateParams params =
        SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            // Stripe substitutes the literal {CHECKOUT_SESSION_ID} placeholder
            .setSuccessUrl(baseUrl + "/success?session_id={CHECKOUT_SESSION_ID}")
            .setCancelUrl(baseUrl + "/cancel")
            .addAllLineItem(lineItems)
            // fulfillment maps line items back to DB pieces through this, never by name
            .putMetadata(
                "piece_ids",
                pieceIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
            .build();

    Session session = null;

    try {
      session = Session.create(params);
    } catch (StripeException exception) {
      logger.error("Payment session creation failed: {}", exception.getMessage());
      return PaymentResponseDTO.builder()
          .status("FAILED")
          .message("Payment session creation failed")
          .build();
    }

    logger.info("Payment session created with ID: {}", session.getId());
    return PaymentResponseDTO.builder()
        .status("SUCCESS")
        .message("Payment session created")
        .sessionId(session.getId())
        .checkoutUrl(session.getUrl())
        .build();
  }

  /**
   * Called by the success page. Fulfills lazily through the same idempotent path as the webhook,
   * so the buyer gets their downloads even if the webhook hasn't arrived yet.
   */
  public OrderConfirmationDTO confirmSession(String sessionId) {
    Stripe.apiKey = stripeSecret;

    logger.info("Confirming session: {}", sessionId);
    Session session;
    try {
      session = Session.retrieve(sessionId);
    } catch (StripeException ex) {
      logger.error("Stripe error confirming session {}: {}", sessionId, ex.getMessage());
      return OrderConfirmationDTO.builder().status("ERROR").build();
    }

    if (!"paid".equals(session.getPaymentStatus())) {
      return OrderConfirmationDTO.builder().status("PENDING").build();
    }

    Order order = fulfillmentService.fulfill(session);

    // Email send runs outside the fulfill() transaction so a slow SMTP server
    // never holds a DB connection open. The markEmailSent atomic guard inside
    // sendEmailIfNeeded ensures only one caller (webhook or success-page) sends it.
    fulfillmentService.sendEmailIfNeeded(order);

    List<DownloadItemDTO> items =
        order.getItems().stream()
            .map(OrderItem::getPiece)
            .map(piece -> new DownloadItemDTO(piece.getId(), piece.getTitle(), piece.getComposer()))
            .toList();

    return OrderConfirmationDTO.builder()
        .status("SUCCESS")
        .buyerEmail(order.getBuyerEmail())
        .downloadToken(order.getDownloadToken())
        .items(items)
        .build();
  }
}
