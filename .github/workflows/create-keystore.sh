#!/bin/bash

# Create a dummy keystore for CI builds
keytool -genkey -v -keystore release_key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias release -storepass android -keypass android \
  -dname "CN=CI Build, OU=CI, O=Synapse, L=Unknown, ST=Unknown, C=US"

# Create keystore.properties file
cat > keystore.properties << EOF
storeFile=release_key.jks
storePassword=android
keyAlias=release
keyPassword=android
EOF