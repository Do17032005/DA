# Kịch bản demo dự án: E-Commerce + AI Recommendation Trending

## 1) Mục tiêu buổi demo

- Chứng minh hệ thống gợi ý hoạt động thật trên dữ liệu hành vi người dùng.
- Giải thích rõ 2 phần AI:
  - Collaborative Filtering (User-Based + Item-Based)
  - Time-Decay Trending (fallback cho cold start)
- Cho thấy kết quả thay đổi sau khi người dùng tương tác.

---

## 2) Chuẩn bị trước demo (5-10 phút)

1. Chạy ứng dụng Spring Boot.
2. Đảm bảo database đã import file `database/database.sql`.
3. Mở sẵn 2 tài khoản:
   - User A: user mới (ít/không có lịch sử)
   - User B: user đã có lịch sử
4. Mở sẵn 2 API trên trình duyệt/Postman:
   - `GET /api/recommendations/user/{userId}?limit=6`
   - `GET /api/recommendations/debug/time-decay?limit=10&windowDays=30&lambda=0.08`
5. Chuẩn bị 1 trang sản phẩm để thao tác `view/add_to_cart/wishlist`.

---

## 3) Timeline demo 10 phút (chi tiết theo phút)

## Phút 0-1: Giới thiệu nhanh

Lời thoại gợi ý:

"Đây là hệ thống bán hàng có AI Recommendation. Khi user mới, hệ thống dùng Time-Decay Trending để vẫn gợi ý được ngay. Khi có hành vi đủ nhiều, hệ thống chuyển sang cá nhân hóa bằng Collaborative Filtering."

## Phút 1-3: Demo user mới (Cold Start)

Thao tác:

1. Đăng nhập User A (mới).
2. Gọi `GET /api/recommendations/user/{userAId}?limit=6`.
3. Mở `GET /api/recommendations/debug/time-decay?...`.

Điểm cần nói:

- User mới chưa có lịch sử nên fallback qua trending.
- Trending có Time-Decay: hành vi gần đây có trọng số cao hơn hành vi cũ.

## Phút 3-6: Tạo hành vi real-time

Thao tác:

1. Vào trang chi tiết 2-3 sản phẩm (tạo event `view`).
2. Thêm 1 sản phẩm vào giỏ (`add_to_cart`).
3. Thêm 1 sản phẩm vào wishlist (`wishlist`).
4. (Nếu có) hoàn tất đơn hàng nhỏ để tạo `purchase`.

Điểm cần nói:

- Frontend đã gửi interaction về endpoint `/api/recommendations/interaction`.
- Dữ liệu vừa tạo sẽ ảnh hưởng trực tiếp lần gợi ý kế tiếp.

## Phút 6-8: Gọi lại recommendation

Thao tác:

1. Gọi lại `GET /api/recommendations/user/{userAId}?limit=6`.
2. So sánh danh sách trước và sau tương tác.

Điểm cần nói:

- Danh sách bắt đầu nghiêng theo nhóm sản phẩm user vừa tương tác.
- Đây là dấu hiệu hệ thống đã chuyển từ fallback sang cá nhân hóa.

## Phút 8-9: Demo user cũ

Thao tác:

1. Đăng nhập User B (đã có lịch sử).
2. Gọi `GET /api/recommendations/user/{userBId}?limit=6`.

Điểm cần nói:

- User cũ có lịch sử dày hơn nên kết quả CF rõ ràng hơn user mới.

## Phút 9-10: Kết luận

Lời thoại gợi ý:

"Hệ thống giải quyết cả 2 bài toán: user mới thì có Time-Decay Trending để không rỗng dữ liệu, user cũ thì dùng Collaborative Filtering để cá nhân hóa sâu hơn."

---

## 4) Kịch bản backup khi live demo gặp lỗi

## Trường hợp A: API chậm

- Giảm tham số `limit` từ 10 xuống 6.
- Chỉ demo 1 endpoint chính: `/api/recommendations/user/{userId}`.

## Trường hợp B: Dữ liệu chưa thay đổi rõ

- Chạy nhanh vài thao tác `view/add_to_cart` thêm.
- Dùng endpoint debug time-decay để giải thích score.

## Trường hợp C: Mất mạng / DB lỗi

- Dùng ảnh chụp sẵn trước/sau tương tác.
- Trình bày logic pipeline thay vì thao tác live toàn bộ.

---

## 5) Checklist demo-ready (tick trước giờ trình bày)

- [ ] App chạy ổn định tại localhost.
- [ ] `database.sql` đã chạy thành công.
- [ ] Endpoint `interaction` ghi nhận dữ liệu.
- [ ] Endpoint `user recommendation` trả danh sách không rỗng.
- [ ] Endpoint `debug/time-decay` trả score.
- [ ] Có sẵn User A (mới) và User B (cũ).
- [ ] Có script backup nếu live fail.

---

## 6) Câu trả lời ngắn khi giảng viên hỏi

**Q: Vì sao cần Time-Decay?**

- Vì dữ liệu mới phải có ảnh hưởng mạnh hơn dữ liệu cũ để bắt trend nhanh.

**Q: Vì sao không chỉ dùng Collaborative Filtering?**

- User mới chưa có lịch sử nên cần fallback; Time-Decay giúp giải quyết cold start.

**Q: Hệ thống học từ đâu?**

- Từ hành vi thực: `view`, `add_to_cart`, `wishlist`, `purchase`.

**Q: Làm sao giải thích sản phẩm được gợi ý?**

- Dùng endpoint debug time-decay để xem điểm số theo công thức và theo thời gian.

---

## 7) Endpoint dùng trong demo

- `GET /api/recommendations/user/{userId}?limit=6`
- `GET /api/recommendations/user/{userId}/item-based?limit=6`
- `GET /api/recommendations/user/{userId}/user-based?limit=6`
- `GET /api/recommendations/homepage?userId={userId}&limit=6`
- `POST /api/recommendations/interaction`
- `GET /api/recommendations/debug/time-decay?limit=10&windowDays=30&lambda=0.08`

---

## 8) Payload mẫu để test interaction

```json
{
  "userId": 1,
  "productId": 101,
  "interactionType": "add_to_cart",
  "value": 1
}
```

Bạn có thể đổi `interactionType` thành `view`, `wishlist`, `purchase`.

---

## 9) Thông điệp chốt buổi demo

"Hệ thống recommendation của nhóm không chỉ gợi ý theo dữ liệu cũ, mà còn phản ứng theo hành vi mới theo thời gian thực. Nhờ kết hợp Collaborative Filtering và Time-Decay Trending, hệ thống vừa cá nhân hóa tốt cho user cũ vừa xử lý tốt cold start cho user mới."
