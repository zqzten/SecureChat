const rsa = require("./security/rsa");
const aes = require("./security/aes");

module.exports = {
    sessionKey: null,
    sessionIV: null,
    APIError: function (code, message) {
        this.code = code || "internal:unknown_error";
        this.message = message || "";
    },
    restify: () => {
        const pathPrefix = "/api/";
        return async (ctx, next) => {
            if (ctx.request.path.startsWith(pathPrefix)) {
                ctx.rest = data => {
                    ctx.response.body = data;
                };
                try {
                    await next();
                } catch (e) {
                    ctx.response.status = 400;
                    ctx.response.body = {
                        code: e.code || "internal:unknown_error",
                        message: e.message || ""
                    };
                } finally {
                    // encrypt response
                    ctx.response.body = aes.encrypt(JSON.stringify(ctx.response.body), this.sessionKey, this.sessionIV);
                }
            } else {
                await next();
            }
        };
    },
    decrypt: () => {
        return async (ctx, next) => {
            const key = ctx.request.body.key;
            const iv = ctx.request.body.iv;
            const body = ctx.request.body.body;

            // get session key and iv
            this.sessionKey = Buffer.from(rsa.decrypt(key), "base64");
            this.sessionIV = Buffer.from(rsa.decrypt(iv), "base64");

            // decrypt request body
            ctx.request.body = JSON.parse(aes.decrypt(body, this.sessionKey, this.sessionIV));

            await next();
        }
    }
};
