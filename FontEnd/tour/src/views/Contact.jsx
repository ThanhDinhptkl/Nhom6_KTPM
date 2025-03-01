import React from 'react';
import { View, Text, Image, ScrollView, Linking } from 'react-native';
import tw from 'tailwind-rn';

const ContactScreen = () => {
  return (
    <View style={tw('flex-1 bg-white')}>
      <View style={tw('bg-blue-600 p-4')}>
        <Text style={tw('text-white text-xl font-bold')}>TOUR DU LỊCH</Text>
      </View>

      <ScrollView style={tw('p-4')}>
        <Text style={tw('text-lg font-bold')}>LIÊN HỆ CHÚNG TÔI</Text>
        <Text style={tw('mt-2')}>Bạn có thắc mắc cần tư vấn về dịch vụ? Hãy liên hệ chúng tôi để được hỗ trợ nhanh chóng và tận tình!</Text>

        <Text style={tw('mt-4 font-bold')}>1. Thông tin liên hệ</Text>
        <Text style={tw('mt-2')}>📞 Hotline: 0123456789</Text>
        <Text style={tw('mt-2')}>✉️ Email: [Địa chỉ email]</Text>
        <Text style={tw('mt-2')}>🌐 Website: [Địa chỉ website]</Text>
        <Text style={tw('mt-2')}>🕒 Giờ làm việc: Thứ 2 - Chủ Nhật, từ 08:00 - 18:00</Text>

        <Text style={tw('mt-4 font-bold')}>2. Liên hệ trực tuyến</Text>
        <Text style={tw('mt-2')}>📱 Zalo/Viber/WhatsApp: [SĐT điện thoại]</Text>
        <Text style={tw('mt-2')} onPress={() => Linking.openURL('https://www.facebook.com')}>📘 Facebook: [Link Fanpage]</Text>
        <Text style={tw('mt-2')} onPress={() => Linking.openURL('https://www.instagram.com')}>📸 Instagram: [Link Instagram]</Text>
      </ScrollView>

      <View style={tw('bg-blue-600 p-4')}>
        <Text style={tw('text-center text-white')}>Hotline tư vấn: 0123456789</Text>
      </View>
    </View>
  );
};

export default ContactScreen;