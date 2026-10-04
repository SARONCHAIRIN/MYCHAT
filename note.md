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