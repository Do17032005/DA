(function (window) {
    function extractServerMessage(html) {
        if (!html) return null;
        const match = html.match(/<span[^>]*th:text=\"\$\{error\}\"[^>]*>(.*?)<\/span>|<span[^>]*>(.*?)<\/span>/i);
        if (!match) return null;
        return (match[1] || match[2] || '').trim() || null;
    }

    async function login(username, password, rememberMe) {
        const result = await window.Api.postForm('/login', {
            username: username,
            password: password,
            rememberMe: rememberMe ? 'true' : null
        });

        const finalUrl = (result.url || '').toLowerCase();

        if (finalUrl.includes('/login')) {
            return {
                success: false,
                error: extractServerMessage(result.text) || 'Tên đăng nhập/email hoặc mật khẩu không đúng!'
            };
        }

        return { success: true };
    }

    async function register(userData) {
        const result = await window.Api.postForm('/register', {
            username: userData.username,
            email: userData.email,
            password: userData.password,
            confirmPassword: userData.confirmPassword,
            fullName: userData.fullName,
            phone: userData.phone
        });

        const finalUrl = (result.url || '').toLowerCase();

        if (finalUrl.includes('/register')) {
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
