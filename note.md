<!-- export JWT_SECRET="$(openssl rand -base64 32)"
./mvnw spring-boot:run -->

./mvnw clean compile

export JWT_SECRET="NCTgPPvt8iTrnrm60Iv6jBHwWRySYAleaTXitWheaKs="
./mvnw spring-boot:run

<!-- run wev socket -->
cd /Users/chhairin/Downloads/chat-backend
python3 -m http.server 5500

token rin2
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIzIiwiaXNzIjoiY2hhdC1iYWNrZW5kIiwiaWF0IjoxNzkwOTI2ODExLCJleHAiOjE3OTA5Mjc3MTEsInRva2VuX3R5cGUiOiJhY2Nlc3MifQ.QPBCN1svZvKerdHuH_kT-NP99HTRB1eA0htaiU6amtI






cd /Users/chhairin/Downloads/chat-backend

export GOOGLE_APPLICATION_CREDENTIALS="/Users/chhairin/FireBase_mychat/mychat-9d25a-firebase-adminsdk-fbsvc-b56c028f1e.json"

export JWT_SECRET="NCTgPPvt8iTrnrm60Iv6jBHwWRySYAleaTXitWheaKs="

./mvnw clean compile

./mvnw spring-boot:run








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