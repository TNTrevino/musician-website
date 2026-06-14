package com.project.backend.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadItemDTO {
  private Long pieceId;
  private String title;
  private String composer;
}
