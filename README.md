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

### Backend (Demo)

- **Spring Boot** - Java framework
- **LocalStorage** - Client-side data storage (demo)
- **Maven** - Build tool

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

- Product listing với filters
- Search functionality
- Shopping cart
- Checkout process
- Order history
- User profile management

### 🏪 New Pages (Phase 3)

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

## 📊 Tiến Độ Dự Án

### ✅ Phase 1-3: Hoàn thành (100%)

- Fixed Swiper.js, logo, search, navigation
- Standardized headers
- Created missing pages

### ✅ Phase 4: Hoàn thành (100%)

- [x] Form validation (login + register)
- [x] Loading states & error handling
- [x] Mobile responsive optimization
- [x] Testing checklist

### 📋 Next: Testing & Deployment

- Cross-browser testing
- Mobile testing
- Performance optimization

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

**Version:** 1.0.0 (Phase 4 Complete)  
**Status:** ✅ Ready for Testing  
**Last Updated:** February 1, 2026
