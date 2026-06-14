package com.project.backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@Entity
@Table(name = "order_item")
@Accessors(chain = true)
@NoArgsConstructor
public class OrderItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // excluded from toString/equals to avoid recursing through the bidirectional relation
  @NotNull(message = "An order item must belong to an order")
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne
  @JoinColumn(name = "order_id")
  private Order order;

  @NotNull(message = "An order item must reference a piece")
  @ManyToOne
  @JoinColumn(name = "piece_id")
  private Piece piece;

  @Column(name = "quantity")
  private int quantity = 1;

  // in cents, as charged at checkout
  @Column(name = "unit_amount")
  private Long unitAmount;
}
