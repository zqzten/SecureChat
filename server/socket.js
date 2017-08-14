const bcrypt = require('bcryptjs');

const util = require('./util');
const rsa = require('./security/rsa');
const aes = require('./security/aes');

let emailIDMap = new Map();
let idUserMap = new Map();

function encryptForUserSession(plain, id) {
    const user = idUserMap.get(id);
    return aes.encrypt(plain, user.key, user.iv);
}

function decryptForUserSession(encrypted, id) {
    const user = idUserMap.get(id);
    return aes.decrypt(encrypted, user.key, user.iv);
}

module.exports = io => {
    io.on('connection', socket => {
        /* user online */
        socket.on('online', async (email, password, key, iv) => {
            // get session key and iv
            const sessionKey = Buffer.from(rsa.decrypt(key), 'base64');
            const sessionIV = Buffer.from(rsa.decrypt(iv), 'base64');

            // decrypt email and password
            email = aes.decrypt(email, sessionKey, sessionIV);
            password = aes.decrypt(password, sessionKey, sessionIV);

            // validate email and password
            const user = await util.getUser(email);
            if (!user) return;
            if (!bcrypt.compareSync(password, user.password)) return;

            // add user to online list
            idUserMap.set(socket.id, { email: email, key: sessionKey, iv: sessionIV });
            emailIDMap.set(email, socket.id);

            // notify the user's friends and check if they are online
            let friendList = [];
            const friendEmails = await util.getFriends(email);
            for (const friendEmail of friendEmails) {
                const friend = await util.getUser(friendEmail);
                const online = emailIDMap.has(friendEmail);
                friendList.push({ email: friendEmail, publicKeyPEM: friend.publicKey, online: online });
                if (online) {
                    const id = emailIDMap.get(friendEmail);
                    io.to(id).emit('friend online', encryptForUserSession(email, id));
                }
            }

            // send friend list to user
            const pendingFriendList = await util.getPendingFriends(email);
            const friends = { friendList: friendList, pendingFriendList: pendingFriendList };
            io.to(socket.id).emit('friends', encryptForUserSession(JSON.stringify(friends), socket.id));
        });

        /* user offline */
        socket.on('offline', async () => {
            // get user email
            const email = idUserMap.get(socket.id).email;

            // notify the user's friends
            const friends = await util.getFriends(email);
            for (const friend of friends) {
                if (emailIDMap.has(friend)) {
                    const id = emailIDMap.get(friend);
                    io.to(id).emit('friend offline', encryptForUserSession(email, id));
                }
            }

            // remove user from online list
            emailIDMap.delete(email);
            idUserMap.delete(socket.id);
        });

        /* user send friend request */
        socket.on('friend request', async (friendEmail, ack) => {
            // decrypt friend email
            friendEmail = decryptForUserSession(friendEmail, socket.id);

            // validate friend email
            const friend = await util.getUser(friendEmail);
            if (!friend) {
                ack(encryptForUserSession('no such user', socket.id));
                return;
            }

            // get user email
            const email = idUserMap.get(socket.id).email;

            // check if (pending) friends already
            const buddy = await util.getBuddy(email, friendEmail);
            if (buddy) {
                ack(encryptForUserSession('friends already', socket.id));
                return;
            }

            // create pending buddy
            await util.createPendingBuddy(email, friendEmail);

            // forward request
            if (emailIDMap.has(friendEmail)) {
                const id = emailIDMap.get(friendEmail);
                io.to(id).emit('new friend request', encryptForUserSession(email, id));
            }
        });

        /* user accept friend response */
        socket.on('accept friend request', async (friendEmail, ack) => {
            // decrypt friend email
            friendEmail = decryptForUserSession(friendEmail, socket.id);

            // get user email
            const email = idUserMap.get(socket.id).email;

            // accept pending buddy
            await util.acceptPendingBuddy(friendEmail, email);

            // notify the buddy
            const friend = await util.getUser(friendEmail);
            if (emailIDMap.has(friendEmail)) {
                ack(encryptForUserSession(friend.publicKey, socket.id), encryptForUserSession('online', socket.id));
                const user = await util.getUser(email);
                const id = emailIDMap.get(friendEmail);
                io.to(id).emit('friend request accepted', encryptForUserSession(email, id), encryptForUserSession(user.publicKey, id));
            } else {
                ack(encryptForUserSession(friend.publicKey, socket.id), encryptForUserSession('offline', socket.id));
            }
        });

        /* user deny friend response */
        socket.on('deny friend request', async friendEmail => {
            // decrypt friend email
            friendEmail = decryptForUserSession(friendEmail, socket.id);

            // get user email
            const email = idUserMap.get(socket.id).email;

            // delete pending buddy
            await util.deletePendingBuddy(friendEmail, email);
        });

        /* user send initial private text msg */
        socket.on('initial text msg', (email, encryptedKey, encryptedIV, encryptedContent, encryptedSignature) => {
            // decrypt email
            email = decryptForUserSession(email, socket.id);

            // forward initial msg
            io.to(emailIDMap.get(email)).emit('initial text msg received', encryptForUserSession(idUserMap.get(socket.id).email, emailIDMap.get(email)), encryptedKey, encryptedIV, encryptedContent, encryptedSignature);
        });

        /* user send private text msg */
        socket.on('text msg', (email, encryptedContent, encryptedSignature) => {
            // decrypt email
            email = decryptForUserSession(email, socket.id);

            // forward msg
            io.to(emailIDMap.get(email)).emit('text msg received', encryptForUserSession(idUserMap.get(socket.id).email, emailIDMap.get(email)), encryptedContent, encryptedSignature);
        });

        /* user send initial private file msg */
        socket.on('initial file msg', (email, encryptedKey, encryptedIV, encryptedFileName, encryptedContent, encryptedSignature) => {
            // decrypt email
            email = decryptForUserSession(email, socket.id);

            // forward initial msg
            io.to(emailIDMap.get(email)).emit('initial file msg received', encryptForUserSession(idUserMap.get(socket.id).email, emailIDMap.get(email)), encryptedKey, encryptedIV, encryptedFileName, encryptedContent, encryptedSignature);
        });

        /* user send private file msg */
        socket.on('file msg', (email, encryptedFileName, encryptedContent, encryptedSignature) => {
            // decrypt email
            email = decryptForUserSession(email, socket.id);

            // forward msg
            io.to(emailIDMap.get(email)).emit('file msg received', encryptForUserSession(idUserMap.get(socket.id).email, emailIDMap.get(email)), encryptedFileName, encryptedContent, encryptedSignature);
        });
    });
};
