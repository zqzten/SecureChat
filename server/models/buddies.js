const db = require('../db');

module.exports = db.defineModel('buddies', {
    user1: db.STRING,
    user2: db.STRING,
    pending: db.BOOLEAN
});
