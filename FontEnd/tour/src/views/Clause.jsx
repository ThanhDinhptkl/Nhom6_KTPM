import React from 'react';
import { View, Text, ScrollView, TouchableOpacity, Image, Linking } from 'react-native';
import { TailwindProvider, tw } from 'nativewind';

const App = () => {
  return (
    <TailwindProvider>
      <ScrollView style={tw`flex-1 bg-white`}>
        {/* Header */}
        <View style={tw`flex-row items-center justify-between p-4 border-b border-gray-200`}>
          <Image source={require('./assets/logo.png')} style={tw`w-32 h-8`} />
          <View style={tw`flex-row items-center`}>
            <TouchableOpacity style={tw`mr-4`}>
              <Text style={tw`text-gray-600`}>Trang chủ</Text>
            </TouchableOpacity>
            <TouchableOpacity style={tw`mr-4`}>
              <Text style={tw`text-gray-600`}>Danh sách Tour</Text>
            </TouchableOpacity>
            <TouchableOpacity style={tw`mr-4`}>
              <Text style={tw`text-gray-600`}>Khuyến mãi</Text>
            </TouchableOpacity>
            <TouchableOpacity style={tw`mr-4`}>
              <Text style={tw`text-gray-600`}>Liên hệ</Text>
            </TouchableOpacity>
            <View style={tw`flex-row items-center border border-gray-300 rounded-full px-4 py-2`}>
              <Image source={require('./assets/user.png')} style={tw`w-6 h-6 mr-2`} />
              <Text style={tw`text-gray-600`}>Xin chào, Phạm Hữu Thuận</Text>
            </View>
          </View>
        </View>

        {/* Điều khoản sử dụng */}
        <View style={tw`p-4`}>
          <Text style={tw`text-2xl font-bold mb-4`}>ĐIỀU KHOẢN SỬ DỤNG</Text>
          <Text style={tw`text-base text-gray-700 mb-2`}>1. Giới thiệu</Text>
          <Text style={tw`text-base text-gray-700 mb-4`}>
            Chào mừng bạn đến với [Tên Website]. Khi sử dụng dịch vụ của chúng tôi, bạn đồng ý tuân theo các điều khoản sau đây. Nếu bạn không đồng ý, vui lòng không sử dụng website.
          </Text>
          <Text style={tw`text-base text-gray-700 mb-2`}>2. Quyền và trách nhiệm của người dùng</Text>
          <Text style={tw`text-base text-gray-700 mb-4`}>
            • Cung cấp thông tin cá nhân chính xác khi đặt tour hoặc sử dụng dịch vụ.{"\n"}
            • Không sử dụng website vào mục đích vi phạm pháp luật, lừa đảo hoặc gây ảnh hưởng đến quyền lợi của người khác.{"\n"}
            • Không sao chép, chỉnh sửa hoặc phát tán nội dung của website nếu không có sự cho phép.{"\n"}
            • Chịu trách nhiệm về tài khoản của mình và bảo mật thông tin đăng nhập.
          </Text>
          <Text style={tw`text-base text-gray-700 mb-2`}>3. Đặt tour và thanh toán</Text>
          <Text style={tw`text-base text-gray-700 mb-4`}>
            • Khi đặt tour, khách hàng cần đọc kỹ thông tin về dịch vụ, chính sách thanh toán và hủy tour.{"\n"}
            • Chúng tôi cam kết cung cấp thông tin chính xác, tuy nhiên, có thể có thay đổi về lịch trình hoặc giá cả.{"\n"}
            • Thanh toán có thể được thực hiện qua chuyển khoản ngân hàng, thẻ tín dụng hoặc các phương thức khác được website hỗ trợ.
          </Text>
          <Text style={tw`text-base text-gray-700 mb-2`}>4. Chính sách hủy tour và hoàn tiền</Text>
          <Text style={tw`text-base text-gray-700 mb-4`}>
            • Nếu khách hàng hủy tour trước [X] ngày, có thể được hoàn [X]% số tiền.{"\n"}
            • Nếu hủy sát ngày khởi hành, có thể không được hoàn tiền.{"\n"}
            • Trường hợp tour bị hủy do lỗi từ công ty (thời tiết, dịch bệnh, lý do bất khả kháng), khách hàng sẽ được hoàn tiền hoặc hỗ trợ đổi sang tour khác.
          </Text>
          <Text style={tw`text-base text-gray-700 mb-2`}>5. Bảo mật thông tin</Text>
          <Text style={tw`text-base text-gray-700 mb-4`}>
            • Chúng tôi cam kết bảo vệ thông tin cá nhân của khách hàng theo Chính sách bảo mật.{"\n"}
            • Không chia sẻ thông tin cá nhân với bên thứ ba mà không có sự đồng ý của khách hàng, trừ khi có yêu cầu từ cơ quan chức năng.
          </Text>
          <Text style={tw`text-base text-gray-700 mb-2`}>6. Quyền thay đổi điều khoản</Text>
          <Text style={tw`text-base text-gray-700 mb-4`}>
            • Chúng tôi có quyền thay đổi hoặc cập nhật Điều khoản sử dụng bất cứ lúc nào mà không cần thông báo trước.{"\n"}
            • Người dùng có trách nhiệm kiểm tra và tuân theo các điều khoản mới nhất.
          </Text>
          <Text style={tw`text-base text-gray-700 mb-2`}>7. Giới hạn trách nhiệm</Text>
          <Text style={tw`text-base text-gray-700 mb-4`}>
            • Chúng tôi không chịu trách nhiệm với bất kỳ tổn thất nào do lỗi kỹ thuật, gián đoạn dịch vụ hoặc hành vi vi phạm của bên thứ ba.{"\n"}
            • Khách hàng tự chịu trách nhiệm về quyết định đặt tour và tuân thủ các điều kiện đi lại, visa, bảo hiểm du lịch.
          </Text>
        </View>

        {/* Footer */}
        <View style={tw`bg-gray-100 p-4`}>
          <View style={tw`flex-row justify-between mb-4`}>
            <View>
              <Text style={tw`text-lg font-semibold mb-2`}>Hotline tư vấn</Text>
              <TouchableOpacity onPress={() => Linking.openURL(`tel:0123456789`)}>
                <Text style={tw`text-gray-600`}>0123456789</Text>
              </TouchableOpacity>
              <View style={tw`flex-row mt-2`}>
                <TouchableOpacity style={tw`mr-2`}>
                  <Image source={require('./assets/facebook.png')} style={tw`w-6 h-6`} />
                </TouchableOpacity>
                <TouchableOpacity style={tw`mr-2`}>
                  <Image source={require('./assets/instagram.png')} style={tw`w-6 h-6`} />
                </TouchableOpacity>
                <TouchableOpacity style={tw`mr-2`}>
                  <Image source={require('./assets/tiktok.png')} style={tw`w-6 h-6`} />
                </TouchableOpacity>
              </View>
            </View>
            <View>
              <Text style={tw`text-lg font-semibold mb-2`}>Về chúng tôi</Text>
              <TouchableOpacity>
                <Text style={tw`text-gray-600`}>Giới thiệu</Text>
              </TouchableOpacity>
              <TouchableOpacity>
                <Text style={tw`text-gray-600`}>Liên hệ</Text>
              </TouchableOpacity>
              <TouchableOpacity>
                <Text style={tw`text-gray-600`}>Chính sách bảo mật</Text>
              </TouchableOpacity>
              <TouchableOpacity>
                <Text style={tw`text-gray-600`}>Điều khoản sử dụng</Text>
              </TouchableOpacity>
            </View>
            <View>
              <Text style={tw`text-lg font-semibold mb-2`}>Tour Du lịch</Text>
              <TouchableOpacity>
                <Text style={tw`text-gray-600`}>Tour Du lịch</Text>
              </TouchableOpacity>
              <TouchableOpacity>
                <Text style={tw`text-gray-600`}>Dịch vụ</Text>
              </TouchableOpacity>
              <TouchableOpacity>
                <Text style={tw`text-gray-600`}>Combo giá rẻ</Text>
              </TouchableOpacity>
            </View>
          </View>
          <View>
            <Text style={tw`text-lg font-semibold mb-2`}>Khác</Text>
            <TouchableOpacity>
              <Text style={tw`text-gray-600`}>Tuyển dụng</Text>
            </TouchableOpacity>
            <TouchableOpacity>
              <Text style={tw`text-gray-600`}>Hướng dẫn thanh toán</Text>
            </TouchableOpacity>
            <TouchableOpacity>
              <Text style={tw`text-gray-600`}>Chăm sóc khách hàng</Text>
            </TouchableOpacity>
            <TouchableOpacity>
              <Text style={tw`text-gray-600`}>Đăng ký đối tác</Text>
            </TouchableOpacity>
          </View>
        </View>
        <View style={tw`border-t border-gray-300 pt-4`}>
          <Text style={tw`text-center text-gray-600`}>TOUR DU LỊCH</Text>
          <View style={tw`flex-row justify-center mt-2`}>
            <View style={tw`mr-4`}>
              <Text style={tw`text-gray-600`}>Hồ Chí Minh</Text>
            </View>
            <View>
              <Text style={tw`text-gray-600`}>Hà Nội</Text>
            </View>
          </View>
          <View style={tw`flex-row justify-center mt-2`}>
            <View style={tw`mr-4`}>
              <Text style={tw`text-xs text-gray-600`}>12 Nguyễn Văn Bảo, phường 1, Quận Gò Vấp</Text>
            </View>
            <View>
              <Text style={tw`text-xs text-gray-600`}>48 Giảng Võ, Cát Linh, Đống Đa</Text>
            </View>
          </View>
          <Text style={tw`text-center text-xs text-gray-600 mt-4`}>© 2025 Tourdulich. All rights reserved</Text>
          <Text style={tw`text-center text-xs text-gray-600`}>Đội ngũ phụ trách: Thanh Định - Hữu Thuận - Trường Tuấn - Hoàng Anh - Thanh Thịnh</Text>
        </View>
      </ScrollView>
    </TailwindProvider>
  );
};

export default App;
