export interface UserDTO {
  firstName: string;
  lastName: string;
  emailAddress: string;
  role: string;
}

export interface CreateUpdateUserDTO {
  firstName: string;
  lastName: string;
  password: string;
  emailAddress: string;
  role: string;
}

export interface PieceDTO {
  pieceId: number;
  title: string;
  composer: string;
  price: number;
  yearComposed: number;
  hasElectronics: boolean;
  completed: boolean;
  numOfPlayers: number;
  difficultyGrade: number;
  timeLength: number; // this might be wrong
  description: string;
  productId: string;
}

export interface DownloadItemDTO {
  pieceId: number;
  title: string;
  composer: string;
}

export interface OrderConfirmationDTO {
  status: string;
  buyerEmail: string;
  downloadToken: string;
  items: DownloadItemDTO[];
}

export interface DownloadManifestDTO {
  items: DownloadItemDTO[];
  expiresAt: string;
  remainingDownloads: number;
}

export interface ContactDTO {
  firstName: string;
  lastName: string;
  from: string;
  subject: string;
  message: string;
}

export interface EmailResponseDTO {
  successful: boolean;
  reply: string;
}

export interface CartItems extends PieceDTO {
  quantity: number;
  productId: string;
}

export interface ProductDTO {
  quantity: number;
  // DB piece id, not a Stripe product id
  id: number;
}

export interface LoginDTO {
  username: string;
  password: string;
}

export interface LoginResponseDTO {
  jwtToken: string;
  message: string;
  successful: boolean;
}

export interface RegisterDTO {
  username: string;
  firstName: string;
  lastName: string;
  password: string;
  emailAddress: string;
}

export interface PaymentRequestDTO {
  products: ProductDTO[];
  currency: string;
}

export interface PaymentResponseDTO {
  status: string;
  message: string;
  sessionId: string;
  checkoutUrl: string;
}

