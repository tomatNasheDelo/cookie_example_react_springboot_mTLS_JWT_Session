#!/usr/bin/env bash
set -euo pipefail

# Local development CA and certificates. Requires OpenSSL and JDK keytool.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/certs"
PASS="${CERT_STORE_PASSWORD:-changeit}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$OUT"
rm -f "$OUT"/*

openssl genrsa -out "$TMP/ca.key" 3072
openssl req -x509 -new -key "$TMP/ca.key" -sha256 -days 3650 \
  -subj '/CN=Cookie Lab Development CA' \
  -addext 'basicConstraints=critical,CA:TRUE' \
  -addext 'keyUsage=critical,keyCertSign,cRLSign' \
  -out "$OUT/ca.crt"

issue_cert() {
  local name="$1" san="$2" eku="$3"
  openssl genrsa -out "$TMP/$name.key" 2048
  openssl req -new -key "$TMP/$name.key" -subj "/CN=$name" -out "$TMP/$name.csr"
  cat > "$TMP/$name.ext" <<EOF
basicConstraints=critical,CA:FALSE
keyUsage=critical,digitalSignature,keyEncipherment
extendedKeyUsage=$eku
subjectAltName=$san
EOF
  openssl x509 -req -in "$TMP/$name.csr" -CA "$OUT/ca.crt" -CAkey "$TMP/ca.key" \
    -CAcreateserial -out "$TMP/$name.crt" -days 825 -sha256 -extfile "$TMP/$name.ext"
  openssl pkcs12 -export -in "$TMP/$name.crt" -inkey "$TMP/$name.key" \
    -certfile "$OUT/ca.crt" -name "$name" -out "$OUT/$name.p12" -passout "pass:$PASS"
}

# Server cert SAN validates both local hostname and Docker DNS service name.
issue_cert 'mtls-resource' 'DNS:mtls-resource,DNS:localhost,IP:127.0.0.1' 'serverAuth,clientAuth'
# Backend client certificate is presented to the resource service; it is also valid for HTTPS server auth for local tools.
issue_cert 'backend-client' 'DNS:backend-client, DNS:localhost' 'clientAuth,serverAuth'

keytool -importcert -noprompt -trustcacerts -alias cookie-lab-dev-ca -file "$OUT/ca.crt" \
  -keystore "$OUT/truststore.p12" -storetype PKCS12 -storepass "$PASS"
chmod 600 "$OUT"/*.p12
printf 'Generated local mTLS keystores and CA under %s (password: %s). Do not use these in production.\n' "$OUT" "$PASS"
