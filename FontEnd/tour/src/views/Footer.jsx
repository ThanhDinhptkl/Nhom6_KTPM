import React from "react";

function Footer() {
  return (
    <footer className="bg-blue-800 text-white p-4">
      <div className="container mx-auto flex flex-col md:flex-row justify-between items-center">
        <div className="mb-4 md:mb-0">
          <h3 className="font-bold mb-2">Liên hệ</h3>
          <p>Hotline tư vấn: 0123456789</p>
          <div className="flex space-x-4 mt-2">
            <a href="#" className="hover:text-gray-300">
              Facebook
            </a>
            <a href="#" className="hover:text-gray-300">
              Instagram
            </a>
            <a href="#" className="hover:text-gray-300">
              TikTok
            </a>
          </div>
        </div>
        <div className="mb-4 md:mb-0">
          <h3 className="font-bold mb-2">Về chúng tôi</h3>
          <ul className="space-y-1">
            <li>
              <a href="#" className="hover:text-gray-300">
                Giới thiệu
              </a>
            </li>
            <li>
              <a href="#" className="hover:text-gray-300">
                Liên hệ
              </a>
            </li>
            <li>
              <a href="#" className="hover:text-gray-300">
                Chính sách bảo mật
              </a>
            </li>
            <li>
              <a href="#" className="hover:text-gray-300">
                Điều khoản sử dụng
              </a>
            </li>
          </ul>
        </div>
        <div className="mb-4 md:mb-0">
          <h3 className="font-bold mb-2">Sản phẩm</h3>
          <ul className="space-y-1">
            <li>
              <a href="#" className="hover:text-gray-300">
                Tour Du Lịch
              </a>
            </li>
            <li>
              <a href="#" className="hover:text-gray-300">
                Combo giá tốt
              </a>
            </li>
          </ul>
        </div>
        <div>
          <h3 className="font-bold mb-2">Khác</h3>
          <ul className="space-y-1">
            <li>
              <a href="#" className="hover:text-gray-300">
                Tuyển dụng
              </a>
            </li>
            <li>
              <a href="#" className="hover:text-gray-300">
                Hướng dẫn thanh toán
              </a>
            </li>
            <li>
              <a href="#" className="hover:text-gray-300">
                Chăm sóc khách hàng
              </a>
            </li>
            <li>
              <a href="#" className="hover:text-gray-300">
                Đăng ký đối tác
              </a>
            </li>
          </ul>
        </div>
      </div>
      <div className="text-center mt-4">
        <p>&copy; 2025 Tourdulich. All rights reserved.</p>
        <p>
          Đội ngũ phụ trách: Thanh Dinh - Hữu Thuận - Tuấn Trương - Hoàng Anh
          Thanh Than
        </p>
      </div>
    </footer>
  );
}

export default Footer;
