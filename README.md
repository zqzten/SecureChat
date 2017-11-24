# SecureChat

A **secure** IM implemented with Node.js server and Android client.

## Setup

### Prerequisites

* MySQL Community Server v5.6.35
* Node.js v8.0.0 with npm v5.0.0
* Android SDK API 25 with Support Repository

### Setup database

1. Start your MySQL server
1. Create an empty MySQL database (UTF-8 collation recommended)
1. Create a file named `config.js` under the server dir and input the following content

```javascript
const config = {
    dialect: "mysql",
    database: "securechat", // your newly created database name
    username: "root",       // name of your MySQL user
    password: "root",       // password of the user
    host: "localhost",      // where your MySQL server runs
    port: 3306              // the port your MySQL server listens
};

module.exports = config;
```

### Setup server

Run following command under the server dir

```shell
# install dependencies
npm install

# start server at localhost:3000
npm run start
```

### Setup client

1. Use the latest version of Android Studio to import the `SecureChat` project under the client dir
1. Build and run the app on a local emulator

**Notice:** If you want to run the app on real devices, you should edit the URLs in `SecureChatApplication.java` and `RESTUtil.java` manually to point to your local Node.js server.

## Usage

1. After the app launch, you'll see a **Login** page, you should first click **REGISTER** to register a new user if you don't have one. Then you can login with your already registered user.
1. If it's your first login, the app will ask for permission to **write external storage**. Be sure to accept it or you will not be able to send and receive files.
1. You'll get an empty **Chat** page after login, you can swipe rightward or click the **menu button** on the top-left corner to open the drawer.
1. You'll see your current user's email displayed in the **navigation header** of the drawer, and there're three **menu items** in the drawer: Chat, Friends, Logout. You can click them to switch to the corresponding page.
1. The **Chat** page will show a list of your current active chats. If one of your friends send a message to you, a **Chat** item will pop up in this page showing the friend's name, last message sent/received and an indicator of the number of unread messages. The **Chat** item will be removed if it's no longer active (your friend is offline). You can click a **Chat** item to go to the **Chat Detail** page.
1. The **Friends** page will show a list of your friends and pending friend requests. You can click the **ACCEPT** or **DENY** button to accept or deny a friend request. You can also check the online status of your friend by the color of the **Friend** item: white means online and gray means offline. You can click a white **Friend** item to go to the **Chat Detail** page to start chatting with your friend. You can click the **add friend button** on the top-right corner to send friend request to a user by indicating his/her email.
1. In the **Chat Detail** page, you can see your friend's email displayed on **action bar**, and you can see all messages with your friend here. You can input and send text messages using the **EditText** and **SEND** button at bottom. You can select and send a file using the **attachment** button beside the **SEND** button. File message will show up as its file name beside an **attachment** indicator. All the files you received will be saved to the **Download** dir of your device, you can use your favorite file explorer to view them then. The **Chat Detail** page will be forced to finish if it's no longer active (your friend is offline).

## Security

There's a key distribution system and 3 ways of information transmission in this app, different ways are used to ensure their security.

### Key Distribution System

* The server has a 2048-bit RSA key pair originally, every client knows the server's public key but only the server itself knows its private key.
* User has to provide the server a unique email and a password to create a new client account, then the server will generate a 2048-bit RSA key pair for the client and let it know its own private key.
* Every client's email and public key are stored in the database in plain text, but the password is hashed using bcrypt and the private key is encrypted using AES-128-CBC with the key and IV derived by the client's password using Node.js' default key derivation algorithm. With this implementation, only the client itself knows its password and only with that password it can get its private key.
* Every client can query the server to get any client's public key.

### Client-Server RESTful Service

The login and register part of this app uses classic RESTful service. It takes following steps:

1. Client generates an AES key and IV locally for server session and encrypts them with server's public key
1. Client uses AES-128-CBC to encrypt the original request body with the newly generated AES key and IV
1. Client sends the encrypted body with encrypted AES key and IV to server
1. Server uses its private key to decrypt the AES key and IV
1. Server uses the decrypted AES key and IV to decrypt the original request body
1. Server parses the request and sends response back to client encrypted using AES-128-CBC with the same AES key and IV it received
1. Client uses the AES key and IV of the session to decrypt the response body

### Client-Server Socket Transportation

Other services provided by the server (such as friend query, online notification, etc.) uses socket transportation. It takes following steps:

1. When a client is online, it uses socket to send an online notification to the server, along with the AES key and IV generated locally for the server session which are encrypted with the server's public key, and the client's email and password encrypted by AES-128-CBC with the AES key and IV of the session
1. When the server receives the client's first socket message (the "online" message), it decrypts the session AES key and IV and then use them to decrypt the email and password
1. Server validates the email and password and saves the corresponding AES key and IV of the client in memory if the validation is successful
1. In the following socket transportation, the server and the client use AES-128-CBC with their shared session AES key and IV to encrypt and decrypt all the messages
1. When a client is offline, the server clears the session AES key and IV from its memory

### Client-Client Socket Transportation

"Real" messages and files are transported with socket between clients (transferred by server). It's somewhat similar to the client-server socket transportation but applies signature to provide additional integrity and authentication for human-readable messages. It takes following steps:

1. When a client (sender) want to start a communication with another client (receiver), it generates an AES key and IV locally for the session and encrypts them with the receiver's public key
1. Sender calculate the SHA-256 hash of the plain message and signs the result with its private key
1. Sender encrypts both the message and the signature using AES-128-CBC with the AES key and IV of the session
1. Send uses socket to send a message forward request to server, along with the receiver's email encrypted by AES-128-CBC with the AES key and IV shared with the server before and all the above-mentioned encrypted things
1. Server decrypts the receiver's email using its AES key and IV shared with the sender and forward the other things along with the sender's email encrypted using AES-128-CBC with the AES key and IV shared with the receiver before
1. Receiver decrypts the sender's email using its AES key and IV shared with the server and get the sender's public key from its friend list
1. Receiver uses its private key to decrypt the AES key and IV of the session
1. Receiver uses the decrypted AES key and IV to decrypt the message and signature
1. Receiver decrypt (validate) the signature using the sender's public key
1. Receiver calculate the SHA-256 hash of the plain message and check its integrity by comparing the result with the decrypted signature
1. Receiver saves the corresponding AES key and IV of the sender in memory if all the above procedures are successful
1. In the following socket transportation, the sender and the receiver (may change roles) use each other's public key to sign the plain message and then use AES-128-CBC with their shared session AES key and IV to encrypt and decrypt all the messages and signatures
1. When a client is offline, another client clears the session AES key and IV from its memory
1. File transmission is very similar to the text transmission, the only difference is that file content is transported in bytes and signed along with the byte representation of the file name

## Notice

* DDL is not needed for database setup for the server will automatically create the tables for you on start if they do not exist.
* The Android client will not store anything permanently (except for the files you received). You will lost all your chat history when offline and also have to login again.
* If you want to know more about the implementation detail of security, you may take a look at `/controllers/api.js`, `rest.js`, `socket.js` under service dir and `/util/RESTUtil.java`, `ChatDetailActivity.java`, `SecureChatApplication.java` under client dir.
