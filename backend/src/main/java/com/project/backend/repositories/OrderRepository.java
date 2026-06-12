package com.project.backend.repositories;

import com.project.backend.models.Order;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

  Optional<Order> findByStripeSessionId(String stripeSessionId);

  Optional<Order> findByDownloadToken(String downloadToken);

  // returns 1 only for the caller that claims the email send, so the
  // webhook and the success-page redirect can never both send it
  @Modifying
  @Query(
      """
      UPDATE Order o
      SET o.emailSentAt = CURRENT_TIMESTAMP
      WHERE o.id = :id AND o.emailSentAt IS NULL
      """)
  int markEmailSent(@Param(value = "id") Long id);

  @Modifying
  @Query(
      """
      UPDATE Order o
      SET o.emailSentAt = NULL
      WHERE o.id = :id
      """)
  int resetEmailSent(@Param(value = "id") Long id);

  @Modifying
  @Query(
      """
      UPDATE Order o
      SET o.downloadCount = o.downloadCount + 1
      WHERE o.id = :id AND o.downloadCount < o.maxDownloads
      """)
  int incrementDownloadCount(@Param(value = "id") Long id);
}
