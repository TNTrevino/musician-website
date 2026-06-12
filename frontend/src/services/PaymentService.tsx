import { OrderConfirmationDTO } from "../dtos/dtos";

const baseUrl = import.meta.env.VITE_BACKEND_URL;

export const PaymentService = {
  async confirm(sessionId: string): Promise<OrderConfirmationDTO> {
    try {
      const response = await fetch(
        `${baseUrl}/payment/confirm?session_id=${encodeURIComponent(sessionId)}`,
      );

      const responseData: OrderConfirmationDTO = await response.json();
      return responseData;
    } catch (error) {
      throw new Error(`Something went wrong confirming the payment: ${error}`);
    }
  },
};
