package com.project.backend.services;

import com.project.backend.models.Order;
import com.project.backend.models.OrderItem;
import jakarta.mail.internet.MimeMessage;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class PurchaseEmailService {
  private static final Logger logger = LoggerFactory.getLogger(PurchaseEmailService.class);
  private static final DateTimeFormatter EXPIRY_FORMAT =
      DateTimeFormatter.ofPattern("MMMM d, yyyy").withZone(ZoneId.of("America/Chicago"));

  @Value("${FRONTEND_URL}")
  private String frontendUrl;

  // Gmail rewrites or rejects mail with a From it doesn't own
  @Value("${EMAIL_USER}")
  private String fromAddress;

  @Autowired private JavaMailSender mailSender;

  public void sendDownloadEmail(Order order) throws Exception {
    if (order.getBuyerEmail() == null || order.getBuyerEmail().isBlank()) {
      logger.error("Order {} has no buyer email, cannot send download email", order.getId());
      throw new IllegalStateException("Order has no buyer email");
    }

    String downloadUrl = frontendUrl + "/downloads/" + order.getDownloadToken();
    String expiry = EXPIRY_FORMAT.format(order.getTokenExpiresAt());

    String pieceList =
        order.getItems().stream()
            .map(OrderItem::getPiece)
            .map(
                piece ->
                    "<li>"
                        + HtmlUtils.htmlEscape(piece.getTitle())
                        + " — "
                        + HtmlUtils.htmlEscape(piece.getComposer())
                        + "</li>")
            .collect(Collectors.joining());

    String html =
        """
        <h2>Thank you for your purchase!</h2>
        <p>Your pieces are ready to download:</p>
        <ul>%s</ul>
        <p><a href="%s">Download your pieces here</a></p>
        <p>This link is valid until %s. If it expires or you have any trouble,
        just reply to this email and we'll get you taken care of.</p>
        <p>— Sebastian Havner</p>
        """
            .formatted(pieceList, downloadUrl, expiry);

    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
    helper.setTo(order.getBuyerEmail());
    helper.setFrom(fromAddress);
    helper.setSubject("Your sheet music is ready to download");
    helper.setText(html, true);

    mailSender.send(message);
    logger.info("Download email sent for order {}", order.getId());
  }
}
