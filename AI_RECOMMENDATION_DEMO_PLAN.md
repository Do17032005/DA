# Kế hoạch xây dựng AI Recommendation + kịch bản Trending (cho demo gấp)

## 1) Mục tiêu demo

Trong vài ngày tới, mục tiêu thực tế là:

- Chạy ổn định hệ gợi ý dựa trên Collaborative Filtering hiện có.
- Nâng cấp Trending sang Time-Decay để thể hiện yếu tố AI rõ hơn.
- Có kịch bản demo mạch lạc: dữ liệu vào -> tính điểm -> API trả kết quả -> hiển thị frontend.
- Có fallback an toàn khi thiếu dữ liệu (cold start).

---

## 2) Phạm vi triển khai (ưu tiên MVP)

### Bắt buộc cho demo (MVP)

1. Time-Decay Trending trong truy vấn dữ liệu.
2. Tích hợp Time-Decay vào luồng recommendation fallback.
3. API có thể trả loại recommendation rõ ràng (`item_based_cf`, `user_based_cf`, `time_decay_trending`).
4. Seed dữ liệu interaction đủ để kết quả nhìn thấy khác biệt.
5. 1 kịch bản demo cho user mới và 1 kịch bản cho user đã có hành vi.

### Nên có nếu còn thời gian

1. Feature flag bật/tắt Time-Decay.
2. Log score để giải thích khi thuyết trình.
3. Endpoint debug nội bộ để so sánh trending cũ vs time-decay.

---

## 3) Công thức AI Trending đề xuất

Dùng điểm theo thời gian:

$$
Score(product) = \sum_{i \in interactions} w(type_i) \cdot e^{-\lambda \cdot ageDays_i}
$$

Trong đó:

- `w(PURCHASE)=10`
- `w(ADD_TO_CART)=3`
- `w(WISHLIST)=2`
- `w(VIEW)=1`
- `ageDays`: số ngày từ lúc interaction xảy ra đến hiện tại
- `lambda` khởi tạo: `0.08`
- cửa sổ dữ liệu ban đầu: `30 ngày`

Ý nghĩa khi demo:

- Tương tác gần đây có trọng số cao hơn tương tác cũ.
- Sản phẩm đang lên xu hướng sẽ được đẩy lên nhanh, không bị kẹt bởi dữ liệu tích lũy lâu năm.

---

## 4) Timeline đề xuất (4 ngày trước demo)

## Ngày 1 - Chốt kỹ thuật + DB

- [ ] Chốt công thức và các hệ số (`weights`, `lambda`, `windowDays`).
- [ ] Thêm index cho bảng `user_interactions` để query nhanh:
  - `(product_id, created_at)`
  - `(interaction_type, created_at)`
  - `(user_id, created_at)`
- [ ] Tạo script seed interactions (view/cart/purchase) có timestamp khác nhau (1 ngày, 7 ngày, 20 ngày).

Kết quả cần đạt:

- Có dữ liệu đủ để chứng minh time-decay hoạt động.

## Ngày 2 - DAO + Service

- [ ] Thêm method mới trong `ProductDAO` để lấy `findTrendingTimeDecay(...)`.
- [ ] Tích hợp vào `HybridRecommendationService` làm fallback trending.
- [ ] Giữ method cũ `findTrending(...)` để rollback nếu cần.

Kết quả cần đạt:

- API recommendation gọi ra có thể dùng Time-Decay thực sự.

## Ngày 3 - API + Frontend demo

- [ ] Bổ sung response metadata: `recommendationSource`, `generatedAt`.
- [ ] Đảm bảo frontend hiển thị khối “Gợi ý cho bạn” + “Đang xu hướng”.
- [ ] Thêm script ghi interaction khi user xem chi tiết sản phẩm / thêm giỏ / mua.

Kết quả cần đạt:

- Luồng đầu-cuối hoạt động: thao tác user -> lưu interaction -> điểm thay đổi -> danh sách gợi ý đổi.

## Ngày 4 - Test + rehearsal demo

- [ ] Test chức năng 3 tình huống (user mới, user cũ, user ít dữ liệu).
- [ ] Test hiệu năng API recommendation (ít nhất 50-100 request liên tiếp).
- [ ] Chạy rehearsal đúng script trình bày 10-15 phút.
- [ ] Chuẩn bị phương án fallback khi lỗi (bật trending cũ).

Kết quả cần đạt:

- Demo ổn định, có số liệu minh họa rõ ràng.

---

## 5) Kịch bản Trending cho AI Recommendation (để demo)

## Kịch bản A: User mới (Cold Start)

Mục tiêu: chứng minh hệ thống vẫn gợi ý tốt khi chưa có lịch sử cá nhân.

Bước demo:

1. Đăng nhập bằng tài khoản mới chưa có interaction.
2. Gọi API recommendation.
3. Hệ thống trả `time_decay_trending`.
4. Trình bày vì sao sản phẩm top xuất hiện (nhiều interaction gần đây).

Thông điệp:

- Không cần đợi lâu vẫn có gợi ý hợp lý cho user mới.

## Kịch bản B: User đã có hành vi rõ (CF ưu tiên)

Mục tiêu: chứng minh personalized recommendation.

Bước demo:

1. Dùng user đã có lịch sử xem/mua một nhóm sản phẩm (ví dụ thời trang nam).
2. Gọi API recommendation.
3. Hệ thống trả ưu tiên `item_based_cf`/`user_based_cf`.
4. So sánh với user mới để thấy kết quả khác nhau.

Thông điệp:

- Khi có dữ liệu, CF cho kết quả cá nhân hóa hơn trending chung.

## Kịch bản C: Xu hướng mới nổi (Time-Decay thắng Trending cũ)

Mục tiêu: chứng minh yếu tố “AI theo thời gian”.

Bước demo:

1. Seed thêm 15-20 interaction mới trong 1-2 ngày gần đây cho một sản phẩm chưa từng top trước đó.
2. Gọi API với chế độ time-decay.
3. Quan sát sản phẩm này tăng hạng nhanh.
4. (Nếu có endpoint so sánh) đối chiếu với trending cũ để thấy khác biệt.

Thông điệp:

- Time-Decay bắt nhịp xu hướng mới nhanh hơn mô hình cộng dồn truyền thống.

---

## 6) Checklist kỹ thuật theo file

- `src/main/java/com/clothes/dao/ProductDAO.java`
  - Thêm query Time-Decay trending.
- `src/main/java/com/clothes/service/HybridRecommendationService.java`
  - Dùng Time-Decay làm fallback/trending source.
- `src/main/java/com/clothes/controller/RecommendationController.java`
  - Trả metadata nguồn recommendation.
- `src/main/resources/application.properties`
  - Thêm config: `recommendation.time-decay.lambda`, `recommendation.time-decay.window-days`, `recommendation.time-decay.enabled`.
- `database/` (sql migration mới)
  - Thêm index + seed data demo.
- `src/main/resources/static/js/main.js`
  - Đảm bảo gửi interaction event (view/cart/purchase) qua API.

---

## 7) KPI theo dõi nhanh trước demo

- Latency API recommendation (mục tiêu: < 300ms ở dữ liệu demo).
- Tỷ lệ response có recommendation không rỗng (> 95%).
- Số interaction ghi nhận thành công.
- Top sản phẩm thay đổi khi dữ liệu mới được thêm (chứng minh time-decay có tác dụng).

---

## 8) Rủi ro và phương án dự phòng

Rủi ro chính:

1. Dữ liệu interaction quá ít, kết quả không khác biệt.
2. Query time-decay chậm khi thiếu index.
3. Frontend chưa bắn interaction đầy đủ.

Phương án dự phòng:

1. Chuẩn bị sẵn script seed dữ liệu trước giờ demo.
2. Bật feature flag về trending cũ nếu hiệu năng không đạt.
3. Nếu API lỗi, demo bằng endpoint đã cache/replay response.

---

## 9) Kịch bản nói khi thuyết trình (ngắn gọn)

- “Hệ thống recommendation của nhóm dùng Collaborative Filtering để cá nhân hóa.”
- “Để giải quyết bài toán user mới, nhóm dùng Time-Decay Trending làm fallback.”
- “Điểm AI không chỉ dựa vào tổng lượt xem/mua, mà ưu tiên tương tác gần đây theo hàm decay.”
- “Vì vậy sản phẩm đang nổi lên sẽ được phát hiện sớm, phù hợp xu hướng thực tế.”

---

## 10) Việc cần làm ngay hôm nay

1. Chốt tham số đầu tiên: `lambda=0.08`, `windowDays=30`.
2. Viết SQL index + seed interaction demo.
3. Cài query `findTrendingTimeDecay(...)` trong DAO.
4. Chạy thử 2 kịch bản: user mới và user đã có lịch sử.

Nếu cần, có thể tách kế hoạch này thành board công việc theo giờ (morning/afternoon/evening) để bám sát deadline demo.
