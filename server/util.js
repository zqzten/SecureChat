const model = require("./model");
const User = model.users;
const Buddy = model.buddies;

module.exports = {
    createUser: async user => await User.create(user),
    getUser: async email => {
        let users = await User.findAll({
            where: { email: email }
        });
        return users[0];
    },
    getFriends: async email => {
        let buddies = await Buddy.findAll({
            where: { $or: [{ user1: email }, { user2: email }], pending: false }
        });
        let friends = [];
        for (let buddy of buddies) {
            friends.push(buddy.user1 === email ? buddy.user2 : buddy.user1);
        }
        return friends;
    },
    getPendingFriends: async email => {
        let buddies = await Buddy.findAll({
            where: { user2: email, pending: true }
        });
        let pendingFriends = [];
        for (let buddy of buddies) {
            pendingFriends.push(buddy.user1);
        }
        return pendingFriends;
    },
    getBuddy: async (email1, email2) => {
        let buddies = await Buddy.findAll({
            where: { $or: [{ user1: email1, user2: email2 }, { user1: email2, user2: email1 }] }
        });
        return buddies[0];
    },
    createPendingBuddy: async (email1, email2) => {
        await Buddy.create({
            user1: email1,
            user2: email2,
            pending: true
        });
    },
    acceptPendingBuddy: async (email1, email2) => {
        let buddies = await Buddy.findAll({
            where: { user1: email1, user2: email2 }
        });
        let buddy = buddies[0];
        buddy.pending = false;
        await buddy.save();
    },
    deletePendingBuddy: async (email1, email2) => {
        let buddies = await Buddy.findAll({
            where: { user1: email1, user2: email2 }
        });
        let buddy = buddies[0];
        await buddy.destroy();
    }
};
