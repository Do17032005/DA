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

    // Track product detail view for recommendation system
    autoTrackProductView();

    // Track purchased products once on order success page
    autoTrackOrderPurchase();

    // Keep recommendation section synced after new purchases
    initRecommendationRealtime();
});

function getLoggedInUserId() {
    if (window.appContext && window.appContext.userId) {
        return window.appContext.userId;
    }
    return null;
}

function recordInteraction(productId, interactionType, value = null) {
    const userId = getLoggedInUserId();
    if (!userId || !productId || !interactionType) {
        return;
    }

    const payload = {
        userId: userId,
        productId: Number(productId),
        interactionType: interactionType
    };

    if (value !== null && value !== undefined) {
        payload.value = value;
    }

    $.ajax({
        url: '/api/recommendations/interaction',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(payload),
        error: function() {
            // Keep silent - tracking should never block user flow
        }
    });
}

function getCurrentProductId() {
    const dataProductId = $('body').data('product-id') || $('[data-product-id]').first().data('product-id');
    if (dataProductId) {
        return Number(dataProductId);
    }

    const match = window.location.pathname.match(/^\/products\/(\d+)(?:\/.*)?$/);
    if (match && match[1]) {
        return Number(match[1]);
    }

    return null;
}

function autoTrackProductView() {
    const productId = getCurrentProductId();
    if (!productId) {
        return;
    }

    recordInteraction(productId, 'view');
}

function autoTrackOrderPurchase() {
    const $trackingRoot = $('#purchaseTrackingData');
    if (!$trackingRoot.length) {
        return;
    }

    const orderId = Number($trackingRoot.data('order-id'));
    if (!orderId) {
        return;
    }

    const notifiedKey = 'recommendation_refresh_notified_order_' + orderId;
    if (localStorage.getItem(notifiedKey) === '1') {
        return;
    }

    const productIds = [];
    $trackingRoot.find('.purchase-track-item').each(function() {
        const productId = Number($(this).data('product-id'));

        if (!productId) {
            return;
        }

        if (!productIds.includes(productId)) {
            productIds.push(productId);
        }
    });

    if (!productIds.length) {
        return;
    }

    savePendingRecommendationRefresh(orderId, productIds);
    notifyRecommendationRefresh(orderId, productIds);
    localStorage.setItem(notifiedKey, '1');
}

function initRecommendationRealtime() {
    const userId = getLoggedInUserId();
    const $recommendationSection = $('#homepageRecommendationSection');

    if (!userId || !$recommendationSection.length) {
        return;
    }

    consumePendingRecommendationRefresh();

    if (!window.EventSource) {
        return;
    }

    let source = null;
    let reconnectAttempts = 0;

    const connect = function() {
        if (source) {
            source.close();
        }

        source = new EventSource('/api/recommendations/me/live');

        source.addEventListener('recommendation-refresh', function(event) {
            reconnectAttempts = 0;

            let payload = {};
            try {
                payload = JSON.parse(event.data || '{}');
            } catch (e) {
                payload = {};
            }

            if (payload.type !== 'RECOMMENDATION_REFRESH') {
                return;
            }

            refreshHomepageRecommendations({
                refresh: true,
                recentProductIds: Array.isArray(payload.recentProductIds) ? payload.recentProductIds : []
            });
        });

        source.onerror = function() {
            if (source) {
                source.close();
                source = null;
            }

            reconnectAttempts += 1;
            const nextDelay = Math.min(10000, 1000 * reconnectAttempts);
            setTimeout(connect, nextDelay);
        };
    };

    connect();
}

function savePendingRecommendationRefresh(orderId, productIds) {
    const payload = {
        orderId: Number(orderId),
        recentProductIds: productIds,
        createdAt: Date.now()
    };

    localStorage.setItem('pending_recommendation_refresh', JSON.stringify(payload));
}

function consumePendingRecommendationRefresh() {
    const pendingRaw = localStorage.getItem('pending_recommendation_refresh');
    if (!pendingRaw) {
        return;
    }

    try {
        const payload = JSON.parse(pendingRaw);
        const createdAt = Number(payload.createdAt || 0);
        const isExpired = !createdAt || (Date.now() - createdAt) > 5 * 60 * 1000;

        if (isExpired) {
            localStorage.removeItem('pending_recommendation_refresh');
            return;
        }

        const recentProductIds = Array.isArray(payload.recentProductIds) ? payload.recentProductIds : [];
        const refreshPromise = refreshHomepageRecommendations({
            refresh: true,
            recentProductIds: recentProductIds
        });

        if (refreshPromise && typeof refreshPromise.then === 'function') {
            refreshPromise.then(function(success) {
                if (success) {
                    localStorage.removeItem('pending_recommendation_refresh');
                }
            });
        }
    } catch (e) {
        localStorage.removeItem('pending_recommendation_refresh');
    }
}

function notifyRecommendationRefresh(orderId, productIds) {
    const userId = getLoggedInUserId();
    if (!userId) {
        return;
    }

    $.ajax({
        url: '/api/recommendations/me/refresh',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            orderId: Number(orderId),
            recentProductIds: productIds
        }),
        error: function() {
            // Keep silent, local pending refresh already acts as a fallback.
        }
    });
}

function refreshHomepageRecommendations(options = {}) {
    const userId = getLoggedInUserId();
    const $section = $('#homepageRecommendationSection');
    const $grid = $('#homepageRecommendationGrid');

    if (!userId || !$section.length || !$grid.length) {
        return Promise.resolve(false);
    }

    const limit = Number($section.data('limit')) || 8;
    const params = new URLSearchParams();
    params.set('limit', String(limit));

    if (options.refresh) {
        params.set('refresh', 'true');
    }

    if (Array.isArray(options.recentProductIds) && options.recentProductIds.length) {
        params.set('recentProductIds', options.recentProductIds.join(','));
    }

    return fetch('/api/recommendations/me?' + params.toString(), {
        method: 'GET',
        credentials: 'same-origin'
    })
        .then(function(response) {
            if (!response.ok) {
                throw new Error('Unable to refresh recommendations');
            }
            return response.json();
        })
        .then(function(data) {
            const products = Array.isArray(data.recommendations) ? data.recommendations : [];
            renderHomepageRecommendations(products);

            const now = new Date();
            const $status = $('#homepageRecommendationStatus');
            if ($status.length) {
                $status.text('Đã cập nhật lúc ' + now.toLocaleTimeString('vi-VN'));
            }

            return true;
        })
        .catch(function() {
            // Silent fail to avoid breaking shopping flow
            return false;
        });
}

function renderHomepageRecommendations(products) {
    const $grid = $('#homepageRecommendationGrid');
    if (!$grid.length) {
        return;
    }

    if (!Array.isArray(products) || !products.length) {
        $grid.html('<div class="col-12"><p class="text-muted mb-0">Chưa có gợi ý phù hợp ở thời điểm này.</p></div>');
        return;
    }

    const cardsHtml = products.map(function(product) {
        return buildRecommendationCard(product);
    }).join('');

    $grid.html(cardsHtml);
}

function buildRecommendationCard(product) {
    const productId = Number(product.productId || 0);
    const productName = escapeHtml(product.productName || 'Sản phẩm');
    const imageUrl = escapeHtml(product.imageUrl || '/images/default-product.jpg');
    const productLink = '/products/' + productId;
    const addToCartCall = 'addToCart(' + productId + ')';
    const toggleWishlistCall = 'toggleWishlist(' + productId + ', this)';

    const discountPrice = toNumberOrNull(product.discountPrice);
    const price = toNumberOrNull(product.price) || 0;
    const hasDiscount = discountPrice !== null && discountPrice < price;

    const saleBadge = hasDiscount
        ? '<span class="badge bg-danger badge-sale">-' + calculateSalePercent(discountPrice, price) + '%</span>'
        : '';

    return '' +
        '<div class="col-md-3 col-sm-6">' +
            '<div class="card product-card h-100 position-relative">' +
                saleBadge +
                '<button class="btn btn-light btn-sm rounded-circle wishlist-btn" data-in-wishlist="false" onclick="' + toggleWishlistCall + '">' +
                    '<i class="far fa-heart"></i>' +
                '</button>' +
                '<a href="' + productLink + '">' +
                    '<img src="' + imageUrl + '" alt="' + productName + '" class="card-img-top" style="height: 280px; object-fit: cover;">' +
                '</a>' +
                '<div class="card-body">' +
                    '<h6 class="card-title">' +
                        '<a href="' + productLink + '" class="text-decoration-none text-dark">' + productName + '</a>' +
                    '</h6>' +
                    buildRecommendationPriceHtml(price, discountPrice, hasDiscount) +
                    '<button class="btn btn-primary btn-sm w-100" onclick="' + addToCartCall + '">' +
                        '<i class="fas fa-shopping-cart me-1"></i>Thêm vào giỏ' +
                    '</button>' +
                '</div>' +
            '</div>' +
        '</div>';
}

function buildRecommendationPriceHtml(price, discountPrice, hasDiscount) {
    if (hasDiscount) {
        return '' +
            '<div class="mb-2">' +
                '<span class="h5 text-danger mb-0">' + formatNumber(discountPrice) + 'đ</span>' +
                '<span class="text-muted text-decoration-line-through small ms-1">' + formatNumber(price) + 'đ</span>' +
            '</div>';
    }

    return '' +
        '<div class="mb-2">' +
            '<span class="h5 text-danger mb-0">' + formatNumber(price) + 'đ</span>' +
        '</div>';
}

function calculateSalePercent(discountPrice, price) {
    if (!price || price <= 0 || discountPrice === null || discountPrice >= price) {
        return 0;
    }
    return Math.round((1 - (discountPrice / price)) * 100);
}

function toNumberOrNull(value) {
    if (value === null || value === undefined || value === '') {
        return null;
    }

    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
}

function escapeHtml(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

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
                recordInteraction(productId, 'add_to_cart');
                
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
    const wasInWishlist = button ? $(button).attr('data-in-wishlist') === 'true' : false;
    const desiredAction = wasInWishlist ? 'remove' : 'add';

    $.ajax({
        url: '/wishlist/toggle',
        type: 'POST',
        data: {
            productId: productId,
            action: desiredAction
        },
        success: function(response) {
            if (response.success) {
                const serverAction = response.action || (desiredAction === 'add' ? 'added' : 'removed');
                const inWishlist = serverAction === 'added';
                updateWishlistButtonState(button, inWishlist);
                if (inWishlist) {
                    recordInteraction(productId, 'wishlist');
                }
                showNotification(
                    inWishlist ? 'Đã thêm vào danh sách yêu thích!' : 'Đã gỡ khỏi danh sách yêu thích!',
                    inWishlist ? 'success' : 'info'
                );
                loadWishlistCount();
            } else if (response.loggedIn === false) {
                showNotification('Vui lòng đăng nhập để sử dụng danh sách yêu thích', 'warning');
                setTimeout(function() {
                    window.location.href = '/user/login?redirect=' + encodeURIComponent(window.location.pathname);
                }, 1500);
            } else {
                updateWishlistButtonState(button, wasInWishlist);
                showNotification(response.message || 'Có lỗi xảy ra!', 'error');
            }
        },
        error: function(xhr) {
            updateWishlistButtonState(button, wasInWishlist);
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

function updateWishlistButtonState(button, inWishlist) {
    if (!button) {
        return;
    }

    const $button = $(button);
    $button.toggleClass('active', inWishlist);
    $button.attr('data-in-wishlist', inWishlist);

    const $icon = $button.find('i.fa-heart');
    if ($icon.length) {
        $icon.toggleClass('fas', inWishlist);
        $icon.toggleClass('far', !inWishlist);
        $icon.toggleClass('text-danger', inWishlist);
        if (!inWishlist) {
            $icon.removeClass('text-danger');
        }
    }
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
window.recordInteraction = recordInteraction;
