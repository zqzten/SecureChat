const NodeRSA = require('node-rsa');
const bcrypt = require('bcryptjs');

const APIError = require('../rest').APIError;
const aes = require('../security/aes');
const util = require('../util');

module.exports = {
    'POST /api/register': async (ctx, next) => {
        const email = ctx.request.body.email;
        const password = ctx.request.body.password;

        // check if the request is valid
        if (!(email && password)) throw new APIError('register:request_invalid');

        // check if email existed
        const oldUser = await util.getUser(email);
        if (oldUser) throw new APIError('register:email_existed');

        // gen RSA keys for user
        const key = new NodeRSA({ b: 2048 });
        const publicKey = key.exportKey('public');
        const privateKey = key.exportKey('private');

        // create user
        const user = {
            email: email,
            password: await bcrypt.hash(password, await bcrypt.genSalt()),
            publicKey: publicKey,
            privateKey: aes.encrypt(privateKey, password)
        };
        await util.createUser(user);

        ctx.rest({});

        await next();
    },
    'POST /api/login': async (ctx, next) => {
        const email = ctx.request.body.email;
        const password = ctx.request.body.password;

        // check if the request is valid
        if (!(email && password)) throw new APIError('login:request_invalid');

        // validate email
        const user = await util.getUser(email);
        if (!user) throw new APIError('login:user_not_exist');

        // validate password
        if (!await bcrypt.compare(password, user.password)) throw new APIError('login:password_mismatch');

        ctx.rest({
            privateKey: aes.decrypt(user.privateKey, password)
        });

        await next();
    }
};
