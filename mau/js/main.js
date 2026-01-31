/* ===================================
   CANIFA Website Clone - Main JavaScript
   =================================== */

document.addEventListener('DOMContentLoaded', function () {
    // Initialize all components
    initSubHeaderSlider();
    initHeroSlider();
    initVoucherSlider();
    initProductsSlider();
    initProductTabs();
    initStickyHeader();
    initBackToTop();
    initMobileMenu();
    initLoginModal();
});

/* ===================================
   SUB HEADER SLIDER
   =================================== */
function initSubHeaderSlider() {
    new Swiper('.sub-header-slider', {
        loop: true,
        autoplay: {
            delay: 3000,
            disableOnInteraction: false,
        },
        effect: 'fade',
        fadeEffect: {
            crossFade: true
        },
    });
}

/* ===================================
   HERO SLIDER
   =================================== */
function initHeroSlider() {
    new Swiper('.hero-slider', {
        loop: true,
        autoplay: {
            delay: 5000,
            disableOnInteraction: false,
        },
        effect: 'fade',
        fadeEffect: {
            crossFade: true
        },
        navigation: {
            nextEl: '.hero-slider .swiper-button-next',
            prevEl: '.hero-slider .swiper-button-prev',
        },
        pagination: {
            el: '.hero-slider .swiper-pagination',
            clickable: true,
        },
    });
}

/* ===================================
   VOUCHER SLIDER
   =================================== */
function initVoucherSlider() {
    new Swiper('.voucher-slider', {
        slidesPerView: 1,
        spaceBetween: 15,
        breakpoints: {
            576: {
                slidesPerView: 2,
            },
            768: {
                slidesPerView: 3,
            },
            1024: {
                slidesPerView: 4,
            },
        },
    });
}

/* ===================================
   PRODUCTS SLIDER
   =================================== */
function initProductsSlider() {
    new Swiper('.products-slider', {
        slidesPerView: 1,
        spaceBetween: 20,
        navigation: {
            nextEl: '.products-slider .swiper-button-next',
            prevEl: '.products-slider .swiper-button-prev',
        },
        breakpoints: {
            480: {
                slidesPerView: 2,
            },
            768: {
                slidesPerView: 3,
            },
            1024: {
                slidesPerView: 4,
            },
            1280: {
                slidesPerView: 5,
            },
        },
    });
}

/* ===================================
   PRODUCT TABS
   =================================== */
function initProductTabs() {
    const tabButtons = document.querySelectorAll('.tab-btn');

    tabButtons.forEach(button => {
        button.addEventListener('click', function () {
            // Remove active class from all buttons
            tabButtons.forEach(btn => btn.classList.remove('active'));

            // Add active class to clicked button
            this.classList.add('active');

            // Get the tab data
            const tab = this.dataset.tab;

            // Here you would typically load products for the selected category
            // For demo purposes, we just animate the change
            const productsWrapper = document.querySelector('.products-slider .swiper-wrapper');
            if (productsWrapper) {
                productsWrapper.style.opacity = '0';
                setTimeout(() => {
                    productsWrapper.style.opacity = '1';
                }, 300);
            }

            console.log('Selected tab:', tab);
        });
    });
}

/* ===================================
   STICKY HEADER
   =================================== */
function initStickyHeader() {
    const header = document.querySelector('.main-header');
    const nav = document.querySelector('.main-nav');
    let lastScroll = 0;

    window.addEventListener('scroll', function () {
        const currentScroll = window.pageYOffset;

        // Add shadow when scrolling
        if (currentScroll > 0) {
            header.style.boxShadow = '0 2px 10px rgba(0, 0, 0, 0.1)';
        } else {
            header.style.boxShadow = 'none';
        }

        lastScroll = currentScroll;
    });
}

/* ===================================
   BACK TO TOP BUTTON
   =================================== */
function initBackToTop() {
    const backToTopBtn = document.getElementById('backToTop');

    if (!backToTopBtn) return;

    // Show/hide button based on scroll position
    window.addEventListener('scroll', function () {
        if (window.pageYOffset > 500) {
            backToTopBtn.classList.add('visible');
        } else {
            backToTopBtn.classList.remove('visible');
        }
    });

    // Scroll to top on click
    backToTopBtn.addEventListener('click', function () {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });
}

/* ===================================
   MOBILE MENU
   =================================== */
function initMobileMenu() {
    // Add mobile menu toggle functionality
    const navMenu = document.querySelector('.nav-menu');

    // Make nav scrollable on mobile
    if (navMenu) {
        navMenu.addEventListener('scroll', function () {
            // Add scroll indicators if needed
        });
    }
}

/* ===================================
   SEARCH FUNCTIONALITY
   =================================== */
const searchInput = document.querySelector('.search-input');
if (searchInput) {
    searchInput.addEventListener('input', function (e) {
        const query = e.target.value.trim();

        // Debounce search
        clearTimeout(this.searchTimeout);
        this.searchTimeout = setTimeout(() => {
            if (query.length > 2) {
                // Perform search
                console.log('Searching for:', query);
            }
        }, 300);
    });

    searchInput.addEventListener('keypress', function (e) {
        if (e.key === 'Enter') {
            const query = this.value.trim();
            if (query) {
                console.log('Search submitted:', query);
                // Redirect to search results page
                // window.location.href = `/search?q=${encodeURIComponent(query)}`;
            }
        }
    });
}

/* ===================================
   WISHLIST FUNCTIONALITY
   =================================== */
document.querySelectorAll('.action-btn.wishlist').forEach(btn => {
    btn.addEventListener('click', function (e) {
        e.preventDefault();
        e.stopPropagation();

        // Toggle active state
        this.classList.toggle('active');

        if (this.classList.contains('active')) {
            this.style.background = '#DA291C';
            this.style.color = '#fff';
            console.log('Added to wishlist');
        } else {
            this.style.background = '#fff';
            this.style.color = '#666';
            console.log('Removed from wishlist');
        }
    });
});

/* ===================================
   COLOR SELECTION
   =================================== */
document.querySelectorAll('.product-colors').forEach(colorsContainer => {
    const dots = colorsContainer.querySelectorAll('.color-dot');

    dots.forEach(dot => {
        dot.addEventListener('click', function () {
            // Remove active from all dots in this container
            dots.forEach(d => d.classList.remove('active'));

            // Add active to clicked dot
            this.classList.add('active');

            // Here you would typically update the product image
            console.log('Selected color:', this.style.backgroundColor);
        });
    });
});

/* ===================================
   VOUCHER SAVE FUNCTIONALITY
   =================================== */
document.querySelectorAll('.voucher-btn').forEach(btn => {
    btn.addEventListener('click', function () {
        if (this.textContent === 'Lưu') {
            this.textContent = 'Đã lưu';
            this.style.background = '#27ae60';
            console.log('Voucher saved');
        } else {
            this.textContent = 'Lưu';
            this.style.background = '#DA291C';
            console.log('Voucher removed');
        }
    });
});

/* ===================================
   LAZY LOADING IMAGES
   =================================== */
if ('IntersectionObserver' in window) {
    const lazyImages = document.querySelectorAll('img[data-src]');

    const imageObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const img = entry.target;
                img.src = img.dataset.src;
                img.removeAttribute('data-src');
                observer.unobserve(img);
            }
        });
    });

    lazyImages.forEach(img => imageObserver.observe(img));
}

/* ===================================
   CART FUNCTIONALITY
   =================================== */
const cart = {
    items: [],

    add(product) {
        const existing = this.items.find(item => item.id === product.id);
        if (existing) {
            existing.quantity++;
        } else {
            this.items.push({ ...product, quantity: 1 });
        }
        this.updateUI();
    },

    remove(productId) {
        this.items = this.items.filter(item => item.id !== productId);
        this.updateUI();
    },

    updateUI() {
        const cartCount = document.querySelector('.cart-count');
        if (cartCount) {
            const totalItems = this.items.reduce((sum, item) => sum + item.quantity, 0);
            cartCount.textContent = totalItems;
        }
    }
};

/* ===================================
   MEGA MENU ACCESSIBILITY
   =================================== */
document.querySelectorAll('.nav-item.has-dropdown').forEach(item => {
    const link = item.querySelector('.nav-link');
    const menu = item.querySelector('.mega-menu');

    if (link && menu) {
        // Show menu on focus for keyboard navigation
        link.addEventListener('focus', () => {
            menu.style.opacity = '1';
            menu.style.visibility = 'visible';
        });

        // Hide menu when focus leaves the dropdown
        item.addEventListener('focusout', (e) => {
            if (!item.contains(e.relatedTarget)) {
                menu.style.opacity = '';
                menu.style.visibility = '';
            }
        });
    }
});

/* ===================================
   ANIMATION ON SCROLL
   =================================== */
if ('IntersectionObserver' in window) {
    const animatedElements = document.querySelectorAll('.banner-item, .collection-item, .blog-card');

    const animationObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
            }
        });
    }, {
        threshold: 0.1
    });

    animatedElements.forEach(el => {
        el.style.opacity = '0';
        el.style.transform = 'translateY(20px)';
        el.style.transition = 'opacity 0.5s ease, transform 0.5s ease';
        animationObserver.observe(el);
    });
}

console.log('CANIFA Website Clone - Initialized successfully!');

/* ===================================
   LOGIN MODAL FUNCTIONALITY
   =================================== */
function initLoginModal() {
    const loginBtn = document.getElementById('loginBtn');
    const loginModal = document.getElementById('loginModal');
    const closeLoginModal = document.getElementById('closeLoginModal');
    const otpModal = document.getElementById('otpModal');
    const closeOtpModal = document.getElementById('closeOtpModal');
    const phoneLoginForm = document.getElementById('phoneLoginForm');
    const phoneInput = document.getElementById('phoneInput');
    const otpPhone = document.getElementById('otpPhone');
    const googleLoginBtn = document.getElementById('googleLoginBtn');
    const verifyOtpBtn = document.getElementById('verifyOtpBtn');
    const resendOtp = document.getElementById('resendOtp');
    const otpInputs = document.querySelectorAll('.otp-input');

    // User state
    let currentUser = JSON.parse(localStorage.getItem('canifaUser')) || null;

    // Update UI based on login state
    function updateLoginState() {
        if (currentUser) {
            loginBtn.innerHTML = `
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                </svg>
                <span>${currentUser.name || 'Tài khoản'}</span>
            `;
            loginBtn.href = 'profile.html';
        }
    }

    // Check login state on init
    updateLoginState();

    // Open login modal
    if (loginBtn) {
        loginBtn.addEventListener('click', function (e) {
            if (!currentUser) {
                e.preventDefault();
                loginModal.classList.add('active');
                document.body.style.overflow = 'hidden';
            }
        });
    }

    // Close login modal
    if (closeLoginModal) {
        closeLoginModal.addEventListener('click', function () {
            loginModal.classList.remove('active');
            document.body.style.overflow = '';
        });
    }

    // Close on overlay click
    if (loginModal) {
        loginModal.addEventListener('click', function (e) {
            if (e.target === loginModal) {
                loginModal.classList.remove('active');
                document.body.style.overflow = '';
            }
        });
    }

    // Close OTP modal
    if (closeOtpModal) {
        closeOtpModal.addEventListener('click', function () {
            otpModal.classList.remove('active');
            document.body.style.overflow = '';
        });
    }

    // Close on overlay click (OTP)
    if (otpModal) {
        otpModal.addEventListener('click', function (e) {
            if (e.target === otpModal) {
                otpModal.classList.remove('active');
                document.body.style.overflow = '';
            }
        });
    }

    // Phone login form submit
    if (phoneLoginForm) {
        phoneLoginForm.addEventListener('submit', function (e) {
            e.preventDefault();
            const phone = phoneInput.value.trim();

            // Validate Vietnamese phone number
            const phoneRegex = /^(0[3|5|7|8|9])+([0-9]{8})$/;
            if (!phoneRegex.test(phone)) {
                showToast('Vui lòng nhập số điện thoại hợp lệ', 'error');
                return;
            }

            // Show OTP modal
            loginModal.classList.remove('active');
            otpModal.classList.add('active');
            otpPhone.textContent = phone.replace(/(\d{3})(\d{3})(\d{4})/, '$1 *** $3');

            // Focus first OTP input
            setTimeout(() => otpInputs[0].focus(), 300);

            // Simulate OTP sent
            showToast('Mã OTP đã được gửi đến số điện thoại của bạn', 'success');
        });
    }

    // OTP input handling
    otpInputs.forEach((input, index) => {
        input.addEventListener('input', function (e) {
            // Only allow numbers
            this.value = this.value.replace(/[^0-9]/g, '');

            // Auto focus next input
            if (this.value && index < otpInputs.length - 1) {
                otpInputs[index + 1].focus();
            }
        });

        input.addEventListener('keydown', function (e) {
            // Backspace - focus previous
            if (e.key === 'Backspace' && !this.value && index > 0) {
                otpInputs[index - 1].focus();
            }
        });

        // Paste handling
        input.addEventListener('paste', function (e) {
            e.preventDefault();
            const paste = (e.clipboardData || window.clipboardData).getData('text');
            const digits = paste.replace(/[^0-9]/g, '').split('');

            digits.forEach((digit, i) => {
                if (otpInputs[index + i]) {
                    otpInputs[index + i].value = digit;
                }
            });

            if (digits.length > 0) {
                const nextIndex = Math.min(index + digits.length, otpInputs.length - 1);
                otpInputs[nextIndex].focus();
            }
        });
    });

    // Verify OTP
    if (verifyOtpBtn) {
        verifyOtpBtn.addEventListener('click', function () {
            const otp = Array.from(otpInputs).map(input => input.value).join('');

            if (otp.length !== 6) {
                showToast('Vui lòng nhập đầy đủ mã OTP', 'error');
                return;
            }

            // Simulate OTP verification (in real app, verify with backend)
            // For demo, any 6-digit code works
            currentUser = {
                id: Date.now(),
                phone: phoneInput.value,
                name: 'Khách hàng',
                email: '',
                memberLevel: 'Green',
                points: 200
            };

            localStorage.setItem('canifaUser', JSON.stringify(currentUser));
            updateLoginState();

            otpModal.classList.remove('active');
            document.body.style.overflow = '';

            showToast('Đăng nhập thành công! Chào mừng bạn đến CANIFA', 'success');

            // Clear OTP inputs
            otpInputs.forEach(input => input.value = '');

            // Redirect to profile after short delay
            setTimeout(() => {
                window.location.href = 'profile.html';
            }, 1500);
        });
    }

    // Resend OTP
    if (resendOtp) {
        resendOtp.addEventListener('click', function (e) {
            e.preventDefault();
            showToast('Mã OTP mới đã được gửi', 'success');
        });
    }

    // Google Login
    if (googleLoginBtn) {
        googleLoginBtn.addEventListener('click', function () {
            // Simulate Google OAuth popup
            // In production, use Google Identity Services
            showToast('Đang kết nối với Google...', 'info');

            setTimeout(() => {
                // Simulate successful Google login
                currentUser = {
                    id: Date.now(),
                    phone: '',
                    name: 'Google User',
                    email: 'user@gmail.com',
                    avatar: 'https://lh3.googleusercontent.com/a/default-user',
                    memberLevel: 'Green',
                    points: 0,
                    loginMethod: 'google'
                };

                localStorage.setItem('canifaUser', JSON.stringify(currentUser));
                updateLoginState();

                loginModal.classList.remove('active');
                document.body.style.overflow = '';

                showToast('Đăng nhập bằng Google thành công!', 'success');

                // Redirect to profile
                setTimeout(() => {
                    window.location.href = 'profile.html';
                }, 1500);
            }, 1500);
        });
    }

    // ESC key to close modals
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            if (loginModal && loginModal.classList.contains('active')) {
                loginModal.classList.remove('active');
                document.body.style.overflow = '';
            }
            if (otpModal && otpModal.classList.contains('active')) {
                otpModal.classList.remove('active');
                document.body.style.overflow = '';
            }
        }
    });
}

/* ===================================
   TOAST NOTIFICATION
   =================================== */
function showToast(message, type = 'info') {
    // Remove existing toast
    const existingToast = document.querySelector('.toast-notification');
    if (existingToast) {
        existingToast.remove();
    }

    // Create toast
    const toast = document.createElement('div');
    toast.className = `toast-notification toast-${type}`;
    toast.innerHTML = `
        <span class="toast-icon">${type === 'success' ? '✓' : type === 'error' ? '✕' : 'ℹ'}</span>
        <span class="toast-message">${message}</span>
    `;

    // Add styles if not exist
    if (!document.getElementById('toast-styles')) {
        const style = document.createElement('style');
        style.id = 'toast-styles';
        style.textContent = `
            .toast-notification {
                position: fixed;
                bottom: 30px;
                left: 50%;
                transform: translateX(-50%) translateY(100px);
                padding: 15px 25px;
                border-radius: 10px;
                display: flex;
                align-items: center;
                gap: 12px;
                font-size: 14px;
                font-weight: 500;
                z-index: 100000;
                opacity: 0;
                transition: all 0.3s ease;
                box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
            }
            .toast-notification.show {
                opacity: 1;
                transform: translateX(-50%) translateY(0);
            }
            .toast-success {
                background: linear-gradient(135deg, #10b981, #059669);
                color: white;
            }
            .toast-error {
                background: linear-gradient(135deg, #ef4444, #dc2626);
                color: white;
            }
            .toast-info {
                background: linear-gradient(135deg, #3b82f6, #2563eb);
                color: white;
            }
            .toast-icon {
                font-size: 18px;
            }
        `;
        document.head.appendChild(style);
    }

    document.body.appendChild(toast);

    // Animate in
    requestAnimationFrame(() => {
        toast.classList.add('show');
    });

    // Remove after delay
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

/* ===================================
   LOGOUT FUNCTIONALITY
   =================================== */
function logout() {
    localStorage.removeItem('canifaUser');
    window.location.href = 'index.html';
}

