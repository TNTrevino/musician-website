package com.project.backend.DTOs;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderConfirmationDTO {
  public String status;
  public String buyerEmail;
  public String downloadToken;
  public List<DownloadItemDTO> items;
}
