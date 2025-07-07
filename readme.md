'''
  ➜  certs git:(master) ✗ curl  -v --cert client-keystore.p12 --cert-type P12 --pass password --cacert rootCA.crt --url https://localhost:9443/oauth2/token --header "Content-Type: application/x-www-form-urlencoded" --data-urlencode "grant_type=client_credentials" --data-urlencode "client_id=mtls-demo-client"
*   Trying 127.0.0.1:9443...
* TCP_NODELAY set
* Connected to localhost (127.0.0.1) port 9443 (#0)
* ALPN, offering h2
* ALPN, offering http/1.1
* successfully set certificate verify locations:
*   CAfile: rootCA.crt
    CApath: /etc/ssl/certs
* TLSv1.3 (OUT), TLS handshake, Client hello (1):
* TLSv1.3 (IN), TLS handshake, Server hello (2):
* TLSv1.3 (IN), TLS handshake, Encrypted Extensions (8):
* TLSv1.3 (IN), TLS handshake, Request CERT (13):
* TLSv1.3 (IN), TLS handshake, Certificate (11):
* TLSv1.3 (IN), TLS handshake, CERT verify (15):
* TLSv1.3 (IN), TLS handshake, Finished (20):
* TLSv1.3 (OUT), TLS change cipher, Change cipher spec (1):
* TLSv1.3 (OUT), TLS handshake, Certificate (11):
* TLSv1.3 (OUT), TLS handshake, CERT verify (15):
* TLSv1.3 (OUT), TLS handshake, Finished (20):
* SSL connection using TLSv1.3 / TLS_AES_256_GCM_SHA384
* ALPN, server did not agree to a protocol
* Server certificate:
*  subject: C=US; O=Spring; OU=Samples; CN=demo-authorizationserver-sample
*  start date: Jul  3 10:59:17 2025 GMT
*  expire date: Oct  6 10:59:17 2027 GMT
*  subjectAltName: host "localhost" matched cert's "localhost"
*  issuer: C=US; O=Spring; OU=Samples; CN=spring-samples-ca
*  SSL certificate verify ok.
> POST /oauth2/token HTTP/1.1
> Host: localhost:9443
> User-Agent: curl/7.68.0
> Accept: */*
> Content-Type: application/x-www-form-urlencoded
> Content-Length: 56
>
* upload completely sent off: 56 out of 56 bytes
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
* Mark bundle as not supporting multiuse
  < HTTP/1.1 200
  < X-Content-Type-Options: nosniff
  < X-XSS-Protection: 0
  < Cache-Control: no-cache, no-store, max-age=0, must-revalidate
  < Pragma: no-cache
  < Expires: 0
  < Strict-Transport-Security: max-age=31536000 ; includeSubDomains
  < X-Frame-Options: DENY
  < Content-Type: application/json;charset=UTF-8
  < Transfer-Encoding: chunked
  < Date: Thu, 03 Jul 2025 12:21:51 GMT
  <
* Connection #0 to host localhost left intact
  {"access_token":"eyJraWQiOiJmNGRiZTNhNC04N2JhLTRmNmEtYTg3OC1iMjY4MmMzYjQ1ZjYiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJtdGxzLWRlbW8tY2xpZW50IiwiYXVkIjoibXRscy1kZW1vLWNsaWVudCIsIm5iZiI6MTc1MTU0NTMxMSwiaXNzIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6OTQ0MyIsImNuZiI6eyJ4NXQjUzI1NiI6ImZlN2lyR3lJRm9qV3ZhQ2hwM0JaSElPZ1VUbkwxTGp4TjVOYlpuUlJ5VkUifSwiZXhwIjoxNzUxNTQ1NjExLCJpYXQiOjE3NTE1NDUzMTEsImp0aSI6IjExNzUwN2E3LWM0ZGUtNGNmZC1hOGE0LWUxOGZkMDVkZjBiZSJ9.KgvvzpCNEK0S2W2uNuxTpNPuzaibkrIvXsDXG3r04eIcwaOKMOMf4WLHytgND_IUBrDoOB8t0Ty4feC-i-tgHQyUTdfqmn3vdV-YKmyH7IGKOU-0ua-ZFYOWmGgCD4CJHGJ3ckQDSRLB1HCxXp1hJkHFt62UwV_EjiduGJ3WlhEo4I8jNeVhGUoJ4ziLkzrN02ySafZ3RN-TbiSYKv513hU8VmLBFfJgFz1Lqs6q_YcxAyDYw7g5SnxOs-OMH5NEOoJU43CXxDtIJrDkcI2JZylBk2q_dn3jkGfqjD6JrSQUmcTeyRJJdYZRWX52YgY3uy9fLZqiDMMLXtU-o84n2Q","token_type":"Bearer","expires_in":300}#
'''


openssl genrsa -out rootCA.key 4096

openssl req -x509 -new -nodes -key rootCA.key -sha256 -days 3650 -out rootCA.crt \
-subj "/C=US/O=Spring/OU=Samples/CN=spring-samples-ca"

server-openssl.cnf
[ req ]
default_bits       = 2048
prompt             = no
default_md         = sha256
distinguished_name = dn
req_extensions     = req_ext

[ dn ]
C  = US
O  = Spring
OU = Samples
CN = demo-authorizationserver-sample

[ req_ext ]
subjectAltName = @alt_names

[ alt_names ]
DNS.1 = demo-authorizationserver-sample
DNS.2 = localhost
IP.1  = 127.0.0.1


2.2 Generate Key and CSR
bash
Copy
Edit
openssl req -new -nodes -out server.csr -newkey rsa:2048 -keyout server.key \
-config server-openssl.cnf
2.3 Sign Server Certificate with CA
bash
Copy
Edit
openssl x509 -req -in server.csr -CA rootCA.crt -CAkey rootCA.key -CAcreateserial \
-out server.crt -days 825 -sha256 -extfile server-openssl.cnf -extensions req_ext



client-openssl.cnf:

ini
Copy
Edit
[ req ]
default_bits       = 2048
prompt             = no
default_md         = sha256
distinguished_name = dn
req_extensions     = req_ext

[ dn ]
C  = US
O  = Spring
OU = Samples
CN = demo-client-sample

[ req_ext ]
subjectAltName = @alt_names

[ alt_names ]
DNS.1 = demo-client-sample





# Generate key + CSR
openssl req -new -nodes -out client.csr -newkey rsa:2048 -keyout client.key \
-config client-openssl.cnf

# Sign client cert
openssl x509 -req -in client.csr -CA rootCA.crt -CAkey rootCA.key -CAcreateserial \
-out client.crt -days 825 -sha256 -extfile client-openssl.cnf -extensions req_ext



Optional: Convert Client Certs to PKCS#12 for curl
openssl pkcs12 -export -out keystore.p12 -inkey client.key -in client.crt \
-certfile rootCA.crt -name demo-client-sample -passout pass:password


openssl pkcs12 -export \
-out server-keystore.p12 \
-inkey server.key \
-in server.crt \
-certfile rootCA.crt \
-name demo-server-sample \
-passout pass:changeit











openssl genrsa -out resource-server.key 2048




--Get token of resource server using curl CLIENT SECRET BASIC AUTH

curl -u messaging-client:secret \
-X POST https://localhost:9443/oauth2/token \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "grant_type=client_credentials" \
--cacert rootCA.crt 

{"access_token":"","token_type":"Bearer","expires_in":299}#


--Get token for resource server using MTLS

curl -v --cert client-keystore.p12:password \
--cert-type P12 \
--cacert rootCA.crt \
-X POST https://localhost:9443/oauth2/token \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "grant_type=client_credentials" \
-d "client_id=mtls-demo-client" \
-d "scope=message.read message.write"