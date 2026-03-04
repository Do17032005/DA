(function (window) {
    function extractServerMessage(html) {
        if (!html) return null;
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');

        const errorNode = doc.querySelector('.alert.alert-danger span');
        if (errorNode && errorNode.textContent) {
            return errorNode.textContent.trim();
        }

        return null;
    }

    async function login(username, password, rememberMe) {
        const result = await window.Api.postForm('/login', {
            username: username,
            password: password,
            rememberMe: rememberMe ? 'true' : null
        });

        const finalUrl = (result.url || '').toLowerCase();

        if (finalUrl.includes('/login') || finalUrl.includes('/user/login')) {
            return {
                success: false,
                error: extractServerMessage(result.text) || 'Tên đăng nhập/email hoặc mật khẩu không đúng!'
            };
        }

        return { success: true };
    }

    async function register(userData) {
        const result = await window.Api.postForm('/user/register', {
            username: userData.username,
            email: userData.email,
            password: userData.password,
            confirmPassword: userData.confirmPassword,
            fullName: userData.fullName,
            phone: userData.phone
        });

        const finalUrl = (result.url || '').toLowerCase();

        if (finalUrl.includes('/user/register')) {
            return {
                success: false,
                error: extractServerMessage(result.text) || 'Đăng ký thất bại. Vui lòng kiểm tra thông tin!'
            };
        }

        return { success: true };
    }

    window.Auth = {
        login: login,
        register: register
    };
})(window);
