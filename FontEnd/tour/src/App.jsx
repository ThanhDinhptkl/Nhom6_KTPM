import React from "react";
import Footer from "./views/Footer";
import Header from "./views/Header";
import MainContent from "./views/MainContent";

function App() {
  return (
    <div className="flex flex-col min-h-screen">
      <Header />
      <main className="flex-1">
        <MainContent />
      </main>
      <Footer />
    </div>
  );
}

export default App;
