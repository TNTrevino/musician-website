package com.project.backend.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

// "order" is a reserved word in SQL, so the table is named "orders"
@Data
@Entity
@Table(name = "orders")
@Accessors(chain = true)
@NoArgsConstructor
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull(message = "An order must have a Stripe session id")
  @Column(name = "stripe_session_id", unique = true)
  private String stripeSessionId;

  @Column(name = "buyer_email")
  private String buyerEmail;

  @NotNull(message = "An order must have a status")
  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private OrderStatus status;

  // in cents, as reported by the Stripe session
  @Column(name = "amount_total")
  private Long amountTotal;

  @Column(name = "currency")
  private String currency;

  @NotNull(message = "An order must have a download token")
  @Column(name = "download_token", unique = true)
  private String downloadToken;

  @NotNull(message = "An order must have a token expiry")
  @Column(name = "token_expires_at")
  private Instant tokenExpiresAt;

  @Column(name = "download_count")
  private int downloadCount;

  @Column(name = "max_downloads")
  private int maxDownloads = 50;

  @Column(name = "email_sent_at")
  private Instant emailSentAt;

  @Column(name = "created_at")
  private Instant createdAt = Instant.now();

  @Column(name = "fulfilled_at")
  private Instant fulfilledAt;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
  private List<OrderItem> items = new ArrayList<>();
}
