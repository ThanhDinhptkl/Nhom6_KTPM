import React from "react";

function Header() {
  return (
    <header className="bg-blue-600 text-white p-4 flex justify-between items-center">
      <div className="flex items-center">
        <img src="/logo.png" alt="Logo" className="h-8 mr-2" />
        <span>TOUR DU LỊCH</span>
      </div>
      <nav className="flex space-x-4">
        <a href="#" className="hover:text-gray-300">
          Trang chủ
        </a>
        <a href="#" className="hover:text-gray-300">
          Danh sách Tour
        </a>
        <a href="#" className="hover:text-gray-300">
          Khuyến mãi
        </a>
        <a href="#" className="hover:text-gray-300">
          Liên hệ
        </a>
      </nav>
      <div className="flex items-center">
        <img
          src="/avatar.png"
          alt="Avatar"
          className="h-8 w-8 rounded-full mr-2"
        />
        <span>Xin chào, Phạm Hữu Thuận</span>
      </div>
    </header>
  );
}

export default Header;
