
<!-- run wev socket -->
cd /Users/chhairin/Downloads/chat-backend
python3 -m http.server 5500


grep -n "mychat-9d25a" lib/firebase_options.dart
47:    projectId: 'mychat-9d25a',
48:    authDomain: 'mychat-9d25a.firebaseapp.com',
49:    storageBucket: 'mychat-9d25a.firebasestorage.app',
57:    projectId: 'mychat-9d25a',
58:    storageBucket: 'mychat-9d25a.firebasestorage.app',
64:    projectId: 'mychat-9d25a',
65:    storageBucket: 'mychat-9d25a.firebasestorage.app',
72:    projectId: 'mychat-9d25a',
73:    storageBucket: 'mychat-9d25a.firebasestorage.app',
81:    projectId: 'mychat-9d25a',
82:    authDomain: 'mychat-9d25a.firebaseapp.com',
83:    storageBucket: 'mychat-9d25a.firebasestorage.app',


//==============================================================



cd /Users/chhairin/Downloads/chat-backend

export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

export DB_USERNAME=root
read -r -s 'DB_PASSWORD?MySQL password: '
printf '\n'
export DB_PASSWORD

export GOOGLE_APPLICATION_CREDENTIALS="/Users/chhairin/FireBase_mychat/mychat-9d25a-firebase-adminsdk-fbsvc-b56c028f1e.json"

export JWT_SECRET="$(openssl rand -base64 32)"

./mvnw spring-boot:run



//================================================================

test
//==========================


cd /Users/chhairin/Downloads/chat-backend

export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

export DB_USERNAME=root
read -r -s 'DB_PASSWORD?MySQL password: '
printf '\n'
export DB_PASSWORD

export GOOGLE_APPLICATION_CREDENTIALS="/Users/chhairin/FireBase_mychat/mychat-9d25a-firebase-adminsdk-fbsvc-b56c028f1e.json"

./mvnw clean compile
./mvnw test