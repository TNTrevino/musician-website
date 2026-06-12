import React, { ReactNode, useState } from "react";
import { CartContext } from "../components/CartContext";
import { CartItems, PaymentRequestDTO, PaymentResponseDTO } from "../dtos/dtos";
import { toast } from "react-toastify";

interface CartServiceProps {
  children: ReactNode;
}

const baseUrl = import.meta.env.VITE_BACKEND_URL;

export const CartProvider: React.FC<CartServiceProps> = ({ children }) => {
  const [cartItems, setCartItems] = useState<CartItems[]>([]);

  const addToCart = (piece: CartItems): void => {
    const isItemInCart = cartItems.find(
      (item) => item.pieceId == piece.pieceId,
    );

    if (isItemInCart) {
      setCartItems(
        cartItems.map((item) =>
          item.pieceId == piece.pieceId
            ? { ...item, quantity: item.quantity + 1 }
            : item,
        ),
      );
    } else {
      setCartItems([...cartItems, { ...piece, quantity: 1 }]);
    }
  };

  const removeFromCart = (piece: CartItems): void => {
    const isItemInCart = cartItems.find(
      (item) => item.pieceId == piece.pieceId,
    );

    if (isItemInCart?.quantity == 1) {
      setCartItems(cartItems.filter((item) => item.pieceId != piece.pieceId));
    } else {
      setCartItems(
        cartItems.map((item) =>
          item.pieceId == piece.pieceId
            ? { ...item, quantity: item.quantity - 1 }
            : item,
        ),
      );
    }
  };

  const removePieceFromCart = (piece: CartItems): void => {
    setCartItems(cartItems.filter((item) => item.pieceId != piece.pieceId));
  };

  const clearCart = (): void => {
    setCartItems([]);
  };

  const getCartSubtotal = (): number => {
    return cartItems.reduce(
      (total, item) => total + item.price * item.quantity,
      0,
    );
  };

  const getTotalItems = (): number => {
    return cartItems.reduce((total, item) => total + item.quantity, 0);
  };

  const prepareCartItemsForCheckout = () => {
    return cartItems.map((item) => ({
      id: item.pieceId,
      quantity: item.quantity,
    }));
  };

  const checkoutCart = async () => {
    const paymentRequest: PaymentRequestDTO = {
      products: prepareCartItemsForCheckout(),
      currency: "USD",
    };

    toast.info("Taking you to payment, please wait...");
    const response = await fetch(`${baseUrl}/payment/checkout`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(paymentRequest),
    });
    const paymentResponse: PaymentResponseDTO = await response.json();
    window.location.href = paymentResponse.checkoutUrl;
  };

  return (
    <CartContext.Provider
      value={{
        cartItems,
        addToCart,
        removeFromCart,
        clearCart,
        getCartSubtotal,
        getTotalItems,
        removePieceFromCart,
        checkoutCart,
      }}
    >
      {children}
    </CartContext.Provider>
  );
};
