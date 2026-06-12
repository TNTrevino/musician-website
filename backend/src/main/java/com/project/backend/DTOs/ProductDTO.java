package com.project.backend.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ProductDTO {
  // DB piece id, not a Stripe product id
  private Long id;
  private Long quantity;
}
