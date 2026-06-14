import { IconTrash } from "@tabler/icons-react";
import { toast } from "react-toastify";
import { useCartContext } from "./CartContext";
import EmptyCart from "./EmptyCart";
import { Link } from "react-router-dom";

const CartSection = () => {
  const {
    cartItems,
    removeFromCart,
    clearCart,
    getCartSubtotal,
    getTotalItems,
    checkoutCart,
  } = useCartContext();

  const fallbackSrc = "/albums/blank.png";

  return (
    <div className="min-h-screen flex flex-col mx-4 md:mx-12 lg:mx-24 xl:mx-52 gap-6 md:gap-8 text-white border-t border-textGray/40 mb-12 md:mb-24 mt-20 md:mt-32 lg:mt-40 py-12 md:py-16">
      {cartItems.length != 0 ? (
        <>
          <h1 className="text-4xl md:text-6xl lg:text-7xl mb-4 md:mb-8 font-light">Shopping Cart</h1>
          <div className="space-y-6 md:space-y-8">
            {cartItems.map((item, index) => (
              <div
                className={`flex flex-col sm:flex-row justify-between gap-3 md:gap-4 pb-4 ${index === cartItems.length - 1
                  ? ""
                  : "border-b border-textGray/20"
                  }`}
              >
                <Link to={"/" + item.title.replace(/ /g, "-").toLowerCase()} className="flex-shrink-0">
                  <img
                    loading="lazy"
                    src={
                      "/albums/" +
                      item.title.replace(/ /g, "-").toLowerCase() +
                      ".png"
                    }
                    onError={(e) => {
                      const target = e.target as HTMLImageElement;
                      target.onerror = null;
                      target.src = fallbackSrc;
                    }}
                    alt={item.title}
                    className="w-full sm:w-32 md:w-44 h-auto object-cover rounded"
                  />
                </Link>

                <div className="flex flex-col justify-between flex-grow">
                  <div className="w-full">
                    <Link
                      to={"/" + item.title.replace(/ /g, "-").toLowerCase()}
                    >
                      <h2 className="text-2xl md:text-3xl mb-2">{item.title}</h2>
                    </Link>

                    <Link
                      to={"/" + item.title.replace(/ /g, "-").toLowerCase()}
                    >
                      <p className="text-gray-400 text-sm line-clamp-2 sm:truncate sm:w-full md:w-96">
                        {item.description}
                      </p>
                    </Link>
                  </div>

                  <div className="flex flex-row items-center gap-3 md:gap-4 mt-3 sm:mt-0">
                    <button
                      onClick={() => {
                        removeFromCart(item);
                      }}
                      className="text-primary cursor-pointer hover:text-white p-3 active:scale-95 transition-transform rounded-lg min-w-[44px] min-h-[44px] flex items-center justify-center"
                      aria-label="Remove item from cart"
                    >
                      <IconTrash size={20} />
                    </button>
                  </div>
                </div>

                <div className="flex flex-row sm:flex-col text-left sm:text-right justify-between sm:justify-between items-center sm:items-end sm:w-auto">
                  <p className="text-sm sm:text-base underline">
                    Item {index + 1}
                  </p>
                  <p className="text-lg md:text-xl font-semibold">
                    ${item.price.toFixed(2)}
                  </p>
                </div>
              </div>
            ))}
          </div>

          <div className="mt-6 md:mt-8 pt-6 md:pt-8 border-t border-textGray/40">
            <div className="flex justify-start items-center mb-4 md:mb-6 gap-3 md:gap-4">
              <span className="text-base md:text-lg">
                Subtotal ({getTotalItems()} items):
              </span>
              <span className="text-base md:text-lg font-semibold">${getCartSubtotal()}</span>
            </div>

            <div className="flex flex-col justify-end items-stretch sm:items-center mb-6 gap-3 md:gap-4">
              <a
                onClick={checkoutCart}
                className="button cursor-pointer text-white py-3 md:py-2 bg-primary w-full sm:w-auto"
              >
                Proceed to Checkout
              </a>
              <a
                onClick={() => {
                  clearCart();
                  toast.success("Cart has been cleared");
                }}
                className="button cursor-pointer text-white py-3 md:py-2 bg-primary w-full sm:w-auto"
              >
                Clear Cart
              </a>
            </div>
          </div>
        </>
      ) : (
        <EmptyCart />
      )}
    </div>
  );
};

export default CartSection;
