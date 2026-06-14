package com.project.backend.services;

import com.project.backend.models.Piece;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class PieceFileService {
  private static final Logger logger = LoggerFactory.getLogger(PieceFileService.class);

  // lives outside the repo working tree, e.g. /var/lib/sebastian/files,
  // since deploys run git reset --hard in the app directory
  @Value("${PIECE_FILES_DIR:/var/lib/sebastian/files}")
  private String filesDir;

  public boolean hasFile(Piece piece) {
    return piece.getFileName() != null && !piece.getFileName().isBlank();
  }

  public Resource getPieceFile(Piece piece) {
    if (!hasFile(piece)) {
      logger.error("Piece {} has no file_name set", piece.getId());
      throw new IllegalStateException("Piece has no file");
    }

    Path base = Path.of(filesDir).toAbsolutePath().normalize();
    Path resolved = base.resolve(piece.getFileName()).normalize();

    // a file_name like "../../etc/passwd" must never escape the files dir
    if (!resolved.startsWith(base)) {
      logger.error("Piece {} file_name resolves outside the files dir", piece.getId());
      throw new IllegalStateException("Invalid piece file path");
    }

    if (!Files.isRegularFile(resolved)) {
      logger.error("Piece {} file does not exist: {}", piece.getId(), resolved);
      throw new IllegalStateException("Piece file not found");
    }

    return new FileSystemResource(resolved);
  }
}
