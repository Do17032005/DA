// Global variables
let cart = [];
let wishlist = [];

// Document ready
$(document).ready(function() {
    // Initialize tooltips
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // Initialize popovers
    var popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
    popoverTriggerList.map(function (popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl);
    });

    // Load cart count
    loadCartCount();
    
    // Load wishlist count
    loadWishlistCount();
});

// Cart functions
function addToCart(productId, quantity = 1, size = null, color = null) {
    $.ajax({
        url: '/cart/api/add',
        type: 'POST',
        data: {
            productId: productId,
            quantity: quantity,
            size: size,
            color: color
        },
        success: function(response) {
            if (response.success) {
                showNotification('Đã thêm vào giỏ hàng!', 'success');
                loadCartCount();
                
                // Show mini cart preview
                showMiniCart();
            } else {
                showNotification(response.message || 'Có lỗi xảy ra!', 'error');
            }
        },
        error: function(xhr) {
            if (xhr.status === 401) {
                showNotification('Vui lòng đăng nhập để thêm vào giỏ hàng', 'warning');
                setTimeout(function() {
                    window.location.href = '/user/login?redirect=' + encodeURIComponent(window.location.pathname);
                }, 1500);
            } else {
                showNotification('Có lỗi xảy ra, vui lòng thử lại!', 'error');
            }
        }
    });
}

function updateCartItem(cartItemId, quantity) {
    $.ajax({
        url: '/cart/update/' + cartItemId,
        type: 'POST',
        data: { quantity: quantity },
        success: function(response) {
            if (response.success) {
                location.reload();
            } else {
                showNotification(response.message || 'Có lỗi xảy ra!', 'error');
            }
        },
        error: function() {
            showNotification('Có lỗi xảy ra, vui lòng thử lại!', 'error');
        }
    });
}

function removeCartItem(cartItemId) {
    if (confirm('Bạn có chắc muốn xóa sản phẩm này?')) {
        $.ajax({
            url: '/cart/remove/' + cartItemId,
            type: 'POST',
            success: function(response) {
                if (response.success) {
                    showNotification('Đã xóa sản phẩm khỏi giỏ hàng', 'success');
                    location.reload();
                }
            },
            error: function() {
                showNotification('Có lỗi xảy ra, vui lòng thử lại!', 'error');
            }
        });
    }
}

function loadCartCount() {
    $.get('/cart/count', function(response) {
        if (response.count !== undefined) {
            $('.fa-shopping-cart').parent().find('.badge').text(response.count);
        }
    });
}

function showMiniCart() {
    // Can implement a mini cart dropdown here
    console.log('Mini cart preview');
}

// Wishlist functions
function toggleWishlist(productId, button) {
    $.ajax({
        url: '/wishlist/toggle',
        type: 'POST',
        data: { productId: productId },
        success: function(response) {
            if (response.success) {
                if (response.added) {
                    $(button).find('i').removeClass('far').addClass('fas').css('color', '#dc3545');
                    showNotification('Đã thêm vào danh sách yêu thích!', 'success');
                } else {
                    $(button).find('i').removeClass('fas').addClass('far').css('color', '');
                    showNotification('Đã xóa khỏi danh sách yêu thích!', 'info');
                }
                loadWishlistCount();
            } else {
                showNotification(response.message || 'Có lỗi xảy ra!', 'error');
            }
        },
        error: function(xhr) {
            if (xhr.status === 401) {
                showNotification('Vui lòng đăng nhập!', 'warning');
                setTimeout(function() {
                    window.location.href = '/user/login?redirect=' + encodeURIComponent(window.location.pathname);
                }, 1500);
            } else {
                showNotification('Có lỗi xảy ra, vui lòng thử lại!', 'error');
            }
        }
    });
}

function removeFromWishlist(wishlistId) {
    if (confirm('Bạn có chắc muốn xóa sản phẩm này?')) {
        $.ajax({
            url: '/wishlist/remove/' + wishlistId,
            type: 'POST',
            success: function(response) {
                if (response.success) {
                    showNotification('Đã xóa khỏi danh sách yêu thích!', 'success');
                    location.reload();
                }
            },
            error: function() {
                showNotification('Có lỗi xảy ra, vui lòng thử lại!', 'error');
            }
        });
    }
}

function loadWishlistCount() {
    $.get('/wishlist/count', function(response) {
        if (response.count !== undefined) {
            $('.fa-heart').parent().find('.badge').text(response.count);
        }
    });
}

// Notification function
function showNotification(message, type = 'info') {
    const bgClass = {
        'success': 'bg-success',
        'error': 'bg-danger',
        'warning': 'bg-warning',
        'info': 'bg-info'
    }[type] || 'bg-info';

    const icon = {
        'success': 'fa-check-circle',
        'error': 'fa-exclamation-circle',
        'warning': 'fa-exclamation-triangle',
        'info': 'fa-info-circle'
    }[type] || 'fa-info-circle';

    const toast = `
        <div class="toast align-items-center text-white ${bgClass} border-0" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    <i class="fas ${icon} me-2"></i>${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        </div>
    `;

    // Create toast container if not exists
    if (!$('#toastContainer').length) {
        $('body').append('<div id="toastContainer" class="toast-container position-fixed top-0 end-0 p-3" style="z-index: 11111;"></div>');
    }

    const $toast = $(toast);
    $('#toastContainer').append($toast);
    
    const bsToast = new bootstrap.Toast($toast[0], {
        autohide: true,
        delay: 3000
    });
    bsToast.show();

    // Remove toast after hidden
    $toast.on('hidden.bs.toast', function() {
        $(this).remove();
    });
}

// Product filters
function applyFilters() {
    const filters = {
        category: $('input[name="category"]:checked').map(function() { return this.value; }).get(),
        priceMin: $('#priceMin').val(),
        priceMax: $('#priceMax').val(),
        size: $('input[name="size"]:checked').map(function() { return this.value; }).get(),
        color: $('input[name="color"]:checked').map(function() { return this.value; }).get(),
        sort: $('#sortSelect').val(),
        gender: $('input[name="gender"]:checked').val()
    };

    // Get current keyword from URL
    const urlParams = new URLSearchParams(window.location.search);
    const keyword = urlParams.get('keyword');

    // Build query string
    const params = new URLSearchParams();
    if (keyword) params.append('keyword', keyword);
    if (filters.gender) params.append('gender', filters.gender);
    filters.category.forEach(c => params.append('category', c));
    filters.size.forEach(s => params.append('size', s));
    filters.color.forEach(c => params.append('color', c));
    if (filters.priceMin) params.append('priceMin', filters.priceMin);
    if (filters.priceMax) params.append('priceMax', filters.priceMax);
    if (filters.sort) params.append('sort', filters.sort);

    window.location.href = '/products?' + params.toString();
}

function clearFilters() {
    window.location.href = '/products';
}

// Quick view product
function quickView(productId) {
    $.get('/products/' + productId + '/quick', function(product) {
        // Show product in modal
        $('#quickViewModal').modal('show');
        // Populate modal with product data
        // ... implement as needed
    });
}

// Compare products
let compareList = JSON.parse(localStorage.getItem('compareList') || '[]');

function addToCompare(productId) {
    if (compareList.length >= 4) {
        showNotification('Chỉ có thể so sánh tối đa 4 sản phẩm!', 'warning');
        return;
    }
    
    if (!compareList.includes(productId)) {
        compareList.push(productId);
        localStorage.setItem('compareList', JSON.stringify(compareList));
        showNotification('Đã thêm vào danh sách so sánh!', 'success');
        updateCompareCount();
    }
}

function removeFromCompare(productId) {
    compareList = compareList.filter(id => id !== productId);
    localStorage.setItem('compareList', JSON.stringify(compareList));
    updateCompareCount();
}

function updateCompareCount() {
    $('#compareCount').text(compareList.length);
}

// Image gallery
function changeMainImage(src) {
    $('#mainProductImage').attr('src', src);
}

// Quantity controls
$(document).on('click', '.qty-decrease', function() {
    const $input = $(this).siblings('input[type="number"]');
    const currentVal = parseInt($input.val());
    if (currentVal > 1) {
        $input.val(currentVal - 1).trigger('change');
    }
});

$(document).on('click', '.qty-increase', function() {
    const $input = $(this).siblings('input[type="number"]');
    const currentVal = parseInt($input.val());
    const max = parseInt($input.attr('max')) || 999;
    if (currentVal < max) {
        $input.val(currentVal + 1).trigger('change');
    }
});

// Form validation
function validateEmail(email) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
}

function validatePhone(phone) {
    const re = /^0[0-9]{9}$/;
    return re.test(phone);
}

// Apply voucher
function applyVoucher() {
    const code = $('#voucherCode').val();
    if (!code) {
        showNotification('Vui lòng nhập mã giảm giá!', 'warning');
        return;
    }

    $.ajax({
        url: '/cart/apply-voucher',
        type: 'POST',
        data: { code: code },
        success: function(response) {
            if (response.success) {
                showNotification('Áp dụng mã giảm giá thành công!', 'success');
                location.reload();
            } else {
                showNotification(response.message || 'Mã giảm giá không hợp lệ!', 'error');
            }
        },
        error: function() {
            showNotification('Có lỗi xảy ra, vui lòng thử lại!', 'error');
        }
    });
}

function removeVoucher() {
    $.ajax({
        url: '/cart/remove-voucher',
        type: 'POST',
        success: function(response) {
            if (response.success) {
                showNotification('Đã xóa mã giảm giá!', 'info');
                location.reload();
            }
        }
    });
}

// Price range slider
if ($('#priceRange').length) {
    const priceRange = document.getElementById('priceRange');
    noUiSlider.create(priceRange, {
        start: [0, 5000000],
        connect: true,
        step: 100000,
        range: {
            'min': 0,
            'max': 5000000
        },
        format: {
            to: function(value) {
                return Math.round(value);
            },
            from: function(value) {
                return Math.round(value);
            }
        }
    });

    priceRange.noUiSlider.on('update', function(values, handle) {
        $('#priceMin').val(values[0]);
        $('#priceMax').val(values[1]);
        $('#priceRangeLabel').text(
            formatNumber(values[0]) + 'đ - ' + formatNumber(values[1]) + 'đ'
        );
    });
}

// Format number
function formatNumber(num) {
    return new Intl.NumberFormat('vi-VN').format(num);
}

// Lazy load images
if ('IntersectionObserver' in window) {
    const imageObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const img = entry.target;
                img.src = img.dataset.src;
                img.classList.remove('lazy');
                imageObserver.unobserve(img);
            }
        });
    });

    document.querySelectorAll('img.lazy').forEach(img => imageObserver.observe(img));
}

// Smooth scroll
$('a[href^="#"]').on('click', function(e) {
    const target = $(this.getAttribute('href'));
    if (target.length) {
        e.preventDefault();
        $('html, body').animate({
            scrollTop: target.offset().top - 100
        }, 500);
    }
});

// Newsletter subscription
$('#newsletterForm').on('submit', function(e) {
    e.preventDefault();
    const email = $(this).find('input[type="email"]').val();
    
    if (!validateEmail(email)) {
        showNotification('Email không hợp lệ!', 'warning');
        return;
    }

    $.post('/newsletter/subscribe', { email: email }, function(response) {
        if (response.success) {
            showNotification('Đăng ký nhận tin thành công!', 'success');
            $('#newsletterForm')[0].reset();
        } else {
            showNotification(response.message || 'Đăng ký không thành công!', 'error');
        }
    }).fail(function() {
        showNotification('Có lỗi xảy ra, vui lòng thử lại!', 'error');
    });
});

// Export functions to global scope
window.addToCart = addToCart;
window.updateCartItem = updateCartItem;
window.removeCartItem = removeCartItem;
window.toggleWishlist = toggleWishlist;
window.removeFromWishlist = removeFromWishlist;
window.applyFilters = applyFilters;
window.clearFilters = clearFilters;
window.quickView = quickView;
window.addToCompare = addToCompare;
window.removeFromCompare = removeFromCompare;
window.changeMainImage = changeMainImage;
window.applyVoucher = applyVoucher;
window.removeVoucher = removeVoucher;
window.showNotification = showNotification;
