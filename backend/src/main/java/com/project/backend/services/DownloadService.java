package com.project.backend.services;

import com.project.backend.DTOs.DownloadItemDTO;
import com.project.backend.DTOs.DownloadManifestDTO;
import com.project.backend.models.Order;
import com.project.backend.models.OrderItem;
import com.project.backend.models.Piece;
import com.project.backend.repositories.OrderRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DownloadService {
  private static final Logger logger = LoggerFactory.getLogger(DownloadService.class);

  @Autowired OrderRepository orderRepository;
  @Autowired PieceFileService pieceFileService;

  @Transactional(readOnly = true)
  public DownloadManifestDTO getManifest(String token) {
    Order order = getLiveOrder(token);

    List<DownloadItemDTO> items =
        order.getItems().stream()
            .map(OrderItem::getPiece)
            .map(piece -> new DownloadItemDTO(piece.getId(), piece.getTitle(), piece.getComposer()))
            .toList();

    return DownloadManifestDTO.builder()
        .items(items)
        .expiresAt(order.getTokenExpiresAt())
        .remainingDownloads(order.getMaxDownloads() - order.getDownloadCount())
        .build();
  }

  @Transactional
  public PieceDownload getPieceDownload(String token, Long pieceId) {
    Order order = getLiveOrder(token);

    Piece piece =
        order.getItems().stream()
            .map(OrderItem::getPiece)
            .filter(p -> p.getId().equals(pieceId))
            .findFirst()
            .orElseThrow(DownloadService::notFound);

    // atomic guard: returns 0 once the order has hit its download cap
    if (orderRepository.incrementDownloadCount(order.getId()) == 0) {
      logger.warn("Order {} hit its download cap", order.getId());
      throw notFound();
    }

    logger.info("Serving piece {} for order {}", piece.getId(), order.getId());
    Resource file;
    try {
      file = pieceFileService.getPieceFile(piece);
    } catch (IllegalStateException e) {
      logger.warn("Piece {} file unavailable for order {}", piece.getId(), order.getId(), e);
      throw notFound();
    }
    return new PieceDownload(file, piece.getTitle());
  }

  private Order getLiveOrder(String token) {
    Order order = orderRepository.findByDownloadToken(token).orElseThrow(DownloadService::notFound);

    if (order.getTokenExpiresAt().isBefore(Instant.now())) {
      logger.warn("Expired download token used for order {}", order.getId());
      throw notFound();
    }

    return order;
  }

  // generic 404 so the endpoint never reveals whether a token exists, is
  // expired, or references a piece outside the order
  private static ResponseStatusException notFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND);
  }

  public record PieceDownload(Resource resource, String title) {}
}
