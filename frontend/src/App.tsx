import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Shop from "./pages/shop/Shop";
import Home from "./pages/home/Home";
import Cart from "./pages/cart/Cart";
import Showcase from "./pages/showcase/Piece";
import Checkout from "./pages/checkout/Checkout";
import ErrorPage from "./pages/ErrorPage";
import { PieceService } from "./services/PieceService";
import { useEffect, useState } from "react";
import { PieceDTO } from "./dtos/dtos";
import Layout from "./components/Layout";
import ContactSection from "./components/ContactSection";
import About from "./pages/about/About";
import Success from "./pages/payment/Success";
import Cancel from "./pages/payment/Cancel";
import Downloads from "./pages/payment/Downloads";
import Login from "./pages/login/Login";
import Signup from "./pages/login/Signup";

function App() {
  const fetchPieces = async () => {
    const pieces = await PieceService.getAllPieces();
    setPieces(pieces);
  };

  const [pieces, setPieces] = useState<PieceDTO[]>([]);

  useEffect(() => {
    fetchPieces();
  }, []);

  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/shop" element={<Shop />} />
          <Route path="/cart" element={<Cart />} />
          <Route path="/checkout" element={<Checkout />} />
          <Route path="/contact-me" element={<ContactSection />} />
          <Route path="/about" element={<About />} />
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/success" element={<Success />} />
          <Route path="/cancel" element={<Cancel />} />
          <Route path="/downloads/:token" element={<Downloads />} />
          <Route path="*" element={<ErrorPage />} />
          {pieces.map((piece) => (
            <Route
              key={piece.pieceId}
              path={`${piece.title.replace(/ /g, "-")}`}
              element={<Showcase piece={piece} />}
            ></Route>
          ))}
        </Routes>
      </Layout>
    </Router>
  );
}

export default App;
