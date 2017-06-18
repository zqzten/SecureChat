const model = require("./model");
const User = model.users;
const Buddy = model.buddies;

module.exports = {
    createUser: async user => await User.create(user),
    getUser: async email => await User.findOne({ where: { email: email } }),
    getFriends: async email => {
        const buddies = await Buddy.findAll({ where: { $or: [{ user1: email }, { user2: email }], pending: false } });
        let friends = [];
        for (const buddy of buddies) {
            friends.push(buddy.user1 === email ? buddy.user2 : buddy.user1);
        }
        return friends;
    },
    getPendingFriends: async email => {
        const buddies = await Buddy.findAll({ where: { user2: email, pending: true } });
        let pendingFriends = [];
        for (const buddy of buddies) {
            pendingFriends.push(buddy.user1);
        }
        return pendingFriends;
    },
    getBuddy: async (email1, email2) => await Buddy.findOne({ where: { $or: [{ user1: email1, user2: email2 }, { user1: email2, user2: email1 }] } }),
    createPendingBuddy: async (email1, email2) => await Buddy.create({ user1: email1, user2: email2, pending: true }),
    acceptPendingBuddy: async (email1, email2) => {
        let buddy = await Buddy.findOne({ where: { user1: email1, user2: email2 } });
        buddy.pending = false;
        await buddy.save();
    },
    deletePendingBuddy: async (email1, email2) => {
        let buddy = await Buddy.findOne({ where: { user1: email1, user2: email2 } });
        await buddy.destroy();
    }
};
