package com.project.backend.controllers;

import com.project.backend.DTOs.DownloadManifestDTO;
import com.project.backend.services.DownloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/download")
public class DownloadController {
  private static final Logger logger = LoggerFactory.getLogger(DownloadController.class);

  @Autowired DownloadService downloadService;

  @GetMapping("/{token}")
  public ResponseEntity<DownloadManifestDTO> getManifest(@PathVariable String token) {
    logger.info("Download manifest requested");
    return ResponseEntity.ok(downloadService.getManifest(token));
  }

  @GetMapping("/{token}/{pieceId}")
  public ResponseEntity<Resource> downloadPiece(
      @PathVariable String token, @PathVariable Long pieceId) {
    DownloadService.PieceDownload download = downloadService.getPieceDownload(token, pieceId);

    String fileName = download.title().replaceAll("[^a-zA-Z0-9 _-]", "").trim() + ".pdf";

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
        .body(download.resource());
  }
}
