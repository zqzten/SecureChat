const crypto = require('crypto');

module.exports = {
    encrypt: (plain, key, iv) => {
        const cipher = iv ? crypto.createCipheriv('aes-128-cbc', key, iv) : crypto.createCipher('aes-128-cbc', key);
        let encrypted = cipher.update(plain, 'utf8', 'base64');
        encrypted += cipher.final('base64');
        return encrypted;
    },
    decrypt: (encrypted, key, iv) => {
        const decipher = iv ? crypto.createDecipheriv('aes-128-cbc', key, iv) : crypto.createDecipher('aes-128-cbc', key);
        let decrypted = decipher.update(encrypted, 'base64', 'utf8');
        decrypted += decipher.final('utf8');
        return decrypted;
    }
};
