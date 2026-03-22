# 🛍️ CLOTHES SHOP - Fashion E-Commerce Website

**Dự án Website Thời Trang - ĐH Phenikaa**  
Nhóm thực hiện: Nguyễn Tiến Doanh, Hoàng Văn Độ

---

## 📋 Tổng Quan Dự Án

Clothes Shop là website bán hàng thời trang trực tuyến với đầy đủ tính năng:

- 🛒 Giỏ hàng & thanh toán
- 👤 Đăng ký/đăng nhập
- 🔍 Tìm kiếm sản phẩm
- 📱 Responsive mobile-first design
- ✨ UI/UX hiện đại với Bootstrap 5

---

## 🚀 Công Nghệ Sử Dụng

### Frontend

- **HTML5** - Cấu trúc semantic
- **CSS3** - Custom styles + CSS Variables
- **Bootstrap 5.3.3** - Responsive framework
- **JavaScript (Vanilla)** - Interactivity
- **Bootstrap Icons 1.11.3** - Icon system
- **Montserrat Font** - Typography

### Backend & AI

- **Spring Boot 3.2.0** - Java framework
- **Spring Security & JWT** - Authentication & Authorization
- **MySQL** - Relational Database
- **Maven** - Build tool
- **AI Recommendation** - Collaborative Filtering & Time-Decay Trending

---

## 📁 Cấu Trúc Thư Mục

```
DA/
├── src/main/
│   ├── java/com/clothes/
│   │   └── clothesApplication.java
│   └── resources/
│       ├── application.properties
│       ├── static/
│       │   ├── css/style.css (2100+ dòng)
│       │   ├── js/main.js (850+ dòng)
│       │   └── images/
│       │       ├── logo.svg
│       │       └── favicon.svg
│       └── templates/ (18 trang HTML)
│           ├── index.html (Homepage)
│           ├── products.html (Danh sách sản phẩm)
│           ├── product-detail.html (Chi tiết)
│           ├── cart.html (Giỏ hàng)
│           ├── checkout.html (Thanh toán)
│           ├── login.html (Đăng nhập) ✨ Enhanced
│           ├── register.html (Đăng ký) ✨ Enhanced
│           ├── profile.html (Hồ sơ)
│           ├── orders.html (Đơn hàng)
│           ├── order-success.html (Xác nhận)
│           ├── address.html (Địa chỉ)
│           ├── membership.html (Thành viên)
│           ├── vouchers.html (Ưu đãi)
│           ├── stores.html (Cửa hàng) 🆕
│           ├── contact.html (Liên hệ) 🆕
│           ├── about.html (Giới thiệu) 🆕
│           ├── 404.html (Lỗi 404) 🆕
│           └── policy-return.html (Chính sách) 🆕
├── pom.xml
├── README.md
└── TESTING_CHECKLIST.md
```

---

## ✨ Tính Năng Chính

### 🔐 Authentication ✨ Phase 4 Enhanced

#### Login (login.html)

- Real-time validation
- Error messages inline
- Loading states
- Success feedback
- Google login (simulated)
- Toggle password visibility

#### Register (register.html)

- Multi-field validation (fullname, email, phone, username, password)
- Password strength indicator
- Duplicate check (username/email)
- Real-time error messages
- Loading states

### 🛍️ E-Commerce Features

- Product listing với filters, pagination, scrollbar
- Search functionality
- Shopping cart quản lý qua Database
- Checkout process & Order management
- User profile & Address management
- **Voucher/Khuyến mãi system**

### 🤖 AI Recommendation System

- **Time-Decay Trending:** Gợi ý sản phẩm định hướng theo xu hướng cho người dùng mới (Cold Start).
- **Collaborative Filtering:** Gợi ý cá nhân hóa dựa trên độ tương đồng Cosine của vector hành vi người dùng.
- **Interaction Tracking:** Thu thập và đánh giá trọng số tương tác (View, Wishlist, Add to Cart, Purchase).

### 🏪 Other Pages

- Store locator với city filter
- Contact form với FAQ
- About page với company info
- 404 error page
- Return policy page

---

## 🎨 Design System

### Màu Sắc

```css
--primary-red: #da291c;
--text-primary: #1a1a1a;
--bg-white: #ffffff;
```

### Typography

- **Font:** Montserrat (300-800 weights)
- **Base:** 14px / 1.5 line-height

### Responsive Breakpoints

- Mobile: < 576px
- Tablet: 576px - 992px
- Desktop: ≥ 992px

---

## 📊 Tiến Độ Dự Án (Theo Sprints)

### ✅ Sprint 1-3: UI/UX & Frontend Core (Hoàn thành 100%)
- Phân tích thiết kế, xây dựng HTML/CSS/JS tĩnh.
- Form validation, responsive mobile-first.

### ✅ Sprint 4-5: Backend API & AI Integration (Hoàn thành 100%)
- Restful API với Spring Boot + Spring Security JWT.
- Tích hợp MySQL Database, thiết kế sơ đồ thực thể ERD.
- Module thuật toán AI Recommendation.
- Data Seeding & Logic cho Đơn hàng, Giỏ hàng, Voucher.

### 🔄 Sprint 6: Kiểm thử & Báo cáo (Đang tiến hành)
- Kiểm thử tích hợp toàn bộ hệ thống (E2E).
- So sánh kết quả hiển thị UX có/không có AI Recommendation.
- Hoàn thiện tài liệu, sơ đồ Use Case, và báo cáo.

---

## 🛠️ Quick Start

```bash
# Run with Maven
mvn spring-boot:run

# Access
http://localhost:8080/index.html
```

---

## 📝 Documentation

- [TESTING_CHECKLIST.md](TESTING_CHECKLIST.md) - Complete testing guide
- Inline code comments
- JSDoc for JavaScript functions

---

## 👥 Team

**Nguyễn Tiến Doanh, Hoàng Văn Độ**  
Đại học Phenikaa - 2025/2026

---

**Version:** 2.0.0 (Tích hợp AI & Backend Complete)  
**Status:** 🔄 Testing & Documentation  
**Last Updated:** March 2026
