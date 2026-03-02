(function (window) {
    async function postForm(url, data) {
        const body = new URLSearchParams();

        Object.keys(data || {}).forEach(function (key) {
            const value = data[key];
            if (value !== undefined && value !== null) {
                body.append(key, String(value));
            }
        });

        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
            },
            body: body.toString(),
            credentials: 'same-origin',
            redirect: 'follow'
        });

        return {
            ok: response.ok,
            status: response.status,
            url: response.url,
            redirected: response.redirected,
            text: await response.text()
        };
    }

    window.Api = {
        postForm: postForm
    };
})(window);
