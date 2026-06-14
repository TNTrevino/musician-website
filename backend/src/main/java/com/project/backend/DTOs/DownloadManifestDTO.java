package com.project.backend.DTOs;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DownloadManifestDTO {
  private List<DownloadItemDTO> items;
  private Instant expiresAt;
  private int remainingDownloads;
}
