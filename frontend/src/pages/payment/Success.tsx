import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { PaymentService } from "../../services/PaymentService";
import { useEffect, useState } from "react";
import { toast } from "react-toastify";
import { OrderConfirmationDTO } from "../../dtos/dtos";
import DownloadList from "../../components/DownloadList";

const CONFIRM_RETRIES = 3;
const RETRY_DELAY_MS = 2000;

const Success = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [order, setOrder] = useState<OrderConfirmationDTO | null>(null);

  useEffect(() => {
    const sessionId = searchParams.get("session_id");
    if (!sessionId) {
      navigate("/cancel");
      return;
    }

    let isMounted = true;

    async function confirmPayment(attempt: number) {
      try {
        const result = await PaymentService.confirm(sessionId!);
        if (!isMounted) return;

        if (result.status === "SUCCESS") {
          toast.success("Your purchase was successful");
          setOrder(result);
        } else if (result.status === "PENDING" && attempt < CONFIRM_RETRIES) {
          setTimeout(() => confirmPayment(attempt + 1), RETRY_DELAY_MS);
        } else {
          toast.error("Payment was not successful");
          navigate("/cancel");
        }
      } catch (error) {
        console.error("Error confirming payment:", error);
        if (isMounted) {
          navigate("/cancel");
        }
      }
    }

    confirmPayment(0);

    return () => {
      isMounted = false;
    };
  }, []);

  return (
    <div className="flex flex-col">
      <div className="min-h-screen bg-black flex flex-row relative">
        <div className="flex flex-col items-center text-center gap-7 w-full min-h-screen justify-center py-20">
          <h1 className="text-8xl text-white">Thank you for your purchase!</h1>
          {order ? (
            <>
              <p className="text-3xl text-white m-3">
                Your downloads are below. We have also emailed the links to{" "}
                {order.buyerEmail}.
              </p>
              <DownloadList token={order.downloadToken} items={order.items} />
            </>
          ) : (
            <p className="text-3xl text-white m-3">
              Finalizing your order, one moment...
            </p>
          )}
          <div>
            <p className="text-2xl text-white m-3">
              If you have any questions, please email us at:
            </p>
            <p className="text-2xl text-white m-3">SebastianHavner@gmail.com</p>
          </div>
          <span className="text-textGray text-4xl">
            <p>
              We really appreciate it! Feel free to continue browsing our{" "}
              <Link to={"/"} className="text-sky-700 underline">
                website.
              </Link>{" "}
            </p>
          </span>
        </div>
      </div>
    </div>
  );
};

export default Success;
