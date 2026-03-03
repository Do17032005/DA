# AI Recommendation Guide & Implementation Plan (chi tiết, dễ dùng)

## 1) Mục tiêu tài liệu

Tài liệu này giúp bạn:

1. Hiểu rõ thuật toán recommendation đang dùng trong dự án.
2. Biết hệ thống chạy thế nào từ frontend -> backend -> database.
3. Có checklist rõ ràng để hoàn thiện AI recommendation trước demo.
4. Có script thao tác demo cho user mới (cold start) và user đã có hành vi.

---

## 2) Recommendation trong dự án bạn là gì?

Dự án hiện dùng **Collaborative Filtering (CF)** + **Time-Decay Trending fallback**.

### 2.1 Collaborative Filtering (CF)

CF nghĩa là: gợi ý sản phẩm dựa trên hành vi người dùng, không cần hiểu sâu nội dung sản phẩm.

Bạn có 2 nhánh:

- **User-Based CF**: tìm user giống nhau, lấy sản phẩm họ thích để gợi ý.
- **Item-Based CF**: tìm sản phẩm thường đi cùng nhau, lấy sản phẩm tương tự để gợi ý.

### 2.2 Time-Decay Trending (fallback)

Khi user mới, ít dữ liệu, CF chưa đủ mạnh.
Khi đó hệ thống dùng trending có giảm trọng số theo thời gian:

$$
Score = \sum w(type) \cdot e^{-\lambda \cdot ageDays}
$$

Trong đó:

- `w(purchase)=10`
- `w(add_to_cart)=3`
- `w(wishlist)=2`
- `w(view)=1`
- `ageDays`: số ngày đã trôi qua từ interaction
- `lambda`: hệ số giảm theo thời gian (đang dùng khởi tạo `0.08`)

Ý nghĩa: tương tác mới gần đây có sức nặng hơn tương tác cũ.

---

## 3) Luồng dữ liệu end-to-end (rất quan trọng)

## Bước A: Frontend ghi nhận hành vi

File: `src/main/resources/static/js/main.js`

- Gọi `recordInteraction(productId, interactionType, value)` khi user:
  - xem sản phẩm (`view`)
  - thêm giỏ (`add_to_cart`)
  - thêm wishlist (`wishlist`)

## Bước B: API nhận interaction

File: `src/main/java/com/clothes/controller/RecommendationController.java`

- Endpoint: `POST /api/recommendations/interaction`
- Nhận payload gồm `userId`, `productId`, `interactionType`, `value`.

## Bước C: Service lưu interaction + làm mới cache

File: `src/main/java/com/clothes/service/HybridRecommendationService.java`

- `recordInteraction(...)` lưu vào `user_interactions`.
- Tăng `view_count` / `purchase_count` cho product.
- Xóa cache recommendation user đó để lần gọi sau có dữ liệu mới.

## Bước D: Khi gọi recommend

- Nếu đủ dữ liệu -> ưu tiên User-Based + Item-Based CF.
- Nếu thiếu dữ liệu -> dùng Time-Decay Trending fallback.

---

## 4) Cấu trúc thành phần chính cần nắm

### 4.1 Model/DAO/Service liên quan

- Model:
  - `UserInteraction`
  - `ProductSimilarity`
  - `UserSimilarity`
  - `Recommendation`

- DAO:
  - `UserInteractionDAO`
  - `ProductDAO` (có `findTrending`, `findTrendingTimeDecay`)
  - `RecommendationDAO`
  - `ProductSimilarityDAO`, `UserSimilarityDAO`

- Service:
  - `ItemBasedCFService`
  - `UserBasedCFService`
  - `HybridRecommendationService`
  - `RecommendationScheduledService`

### 4.2 Database bảng chính

- `user_interactions`
- `product_similarity`
- `user_similarity`
- `recommendations_cache`
- `user_ratings`

---

## 5) Plan hoàn thiện AI Recommendation (theo pha)

## Pha 1 - Ổn định dữ liệu vào (must-have)

1. Bật tracking interaction ở frontend (đã làm).
2. Kiểm tra payload gửi đúng `interactionType` enum backend.
3. Đảm bảo user đã login mới gửi interaction.
4. Đảm bảo lỗi tracking không làm hỏng flow mua hàng.

**Done criteria:**

- Có bản ghi mới trong `user_interactions` khi user xem/thêm giỏ/wishlist.

## Pha 2 - Củng cố Time-Decay Trending

1. Kiểm tra `findTrendingTimeDecay(...)` trả dữ liệu đúng thứ tự mong muốn.
2. Thêm index DB để query nhanh:
   - `(product_id, created_at)`
   - `(interaction_type, created_at)`
   - `(user_id, created_at)`
3. Cho phép bật/tắt Time-Decay bằng config (khuyến nghị).

**Done criteria:**

- API recommendation trả về nhanh, không timeout, top sản phẩm thay đổi theo dữ liệu mới.

## Pha 3 - Nâng chất lượng CF

1. Kiểm tra job tính similarity chạy theo lịch (`RecommendationScheduledService`).
2. Xác nhận có dữ liệu trong `product_similarity` và `user_similarity`.
3. Tinh chỉnh weight hybrid:
   - user-based
     n - item-based
   - trending/time-decay

**Done criteria:**

- User có lịch sử nhận kết quả cá nhân hóa rõ hơn user mới.

## Pha 4 - Demo ready + quan sát

1. Chuẩn bị seed data theo mốc 1/7/20 ngày.
2. Chuẩn bị 2 tài khoản demo:
   - user mới (cold start)
   - user có lịch sử
3. So sánh kết quả trước/sau tương tác trực tiếp trong buổi demo.

**Done criteria:**

- Demo 3-5 phút chạy mượt, có thông điệp kỹ thuật rõ.

---

## 6) Checklist vận hành trước demo (copy dùng ngay)

- [ ] Backend chạy ổn, không lỗi API `/api/recommendations/*`
- [ ] `POST /api/recommendations/interaction` trả success
- [ ] `user_interactions` có dữ liệu mới trong ngày
- [ ] `product_similarity` có bản ghi
- [ ] `recommendations_cache` có bản ghi sinh mới
- [ ] Homepage/user recommendations trả danh sách không rỗng
- [ ] Test user mới: thấy fallback time-decay
- [ ] Test user cũ: thấy cá nhân hóa theo CF

---

## 7) Script demo nói trong 2-3 phút

### Case 1: User mới

1. Đăng nhập tài khoản mới.
2. Gọi API recommend user -> hệ thống dùng time-decay trending.
3. Giải thích: chưa có lịch sử nên fallback thông minh.

### Case 2: User mới tạo hành vi

1. Mở 2-3 sản phẩm cùng nhóm.
2. Add to cart 1 sản phẩm, wishlist 1 sản phẩm.
3. Gọi lại API recommend -> kết quả bắt đầu nghiêng theo hành vi vừa ghi.

### Case 3: User cũ

1. Dùng tài khoản có lịch sử.
2. Gọi API recommend -> hiển thị personalized rõ hơn.

---

## 8) KPI nên theo dõi

- Latency API recommendation (mục tiêu demo: < 300ms nếu dữ liệu vừa).
- Tỷ lệ recommendation không rỗng.
- Số lượng interaction ghi nhận thành công/phút.
- Mức thay đổi top sản phẩm sau khi thêm interaction mới.

---

## 9) Rủi ro thường gặp + cách xử lý nhanh

1. **Không thấy dữ liệu interaction tăng**
   - Kiểm tra user login có `userId`.
   - Kiểm tra request network `POST /api/recommendations/interaction`.

2. **Recommend trả giống nhau cho mọi user**
   - Kiểm tra bảng similarity có dữ liệu chưa.
   - Kiểm tra scheduler đã chạy chưa hoặc chạy tay trước demo.

3. **Query trending chậm**
   - Bổ sung index như checklist.
   - Giảm cửa sổ `windowDays` từ 30 xuống 14 khi demo.

---

## 10) Việc nên làm ngay hôm nay

1. Chuẩn bị migration + seed data demo.
2. Chạy test nhanh 2 tài khoản (new/old user).
3. Chụp trước 2-3 ảnh màn hình kết quả để dự phòng khi mạng chậm.
4. Tập nói đúng thông điệp: "CF để cá nhân hóa, Time-Decay để xử lý cold start và bắt trend mới".

---

## 11) Nâng cấp sau demo (nếu còn thời gian)

- Tách config Time-Decay sang `application.properties`.
- A/B test trọng số recommendation.
- Tạo endpoint debug giải thích score từng sản phẩm.
- Thêm event `purchase` real-time từ luồng đặt hàng thành công.

---

**Kết luận ngắn:**

- Bạn đã có nền tảng AI recommendation khả thi cho demo.
- Trọng tâm hoàn thiện là: dữ liệu interaction đúng + fallback time-decay rõ + kịch bản demo mạch lạc.
