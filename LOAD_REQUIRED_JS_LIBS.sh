#!/bin/bash

mkdir -p src/main/resources/static/js/jose
curl -s https://cdnjs.cloudflare.com/ajax/libs/jose/6.0.11/index.umd.min.js -o src/main/resources/static/js/jose/index.umd.min.js
if [ $? -ne 0 ]; then
    echo "Failed to download jose/index.umd.min.js"
    exit 1
fi
echo "jose.js downloaded successfully to src/main/resources/static/js/jose/index.umd.min.js"

curl -s https://raw.githubusercontent.com/panva/jose/refs/heads/main/LICENSE.md -o src/main/resources/static/js/jose/LICENSE.txt
if [ $? -ne 0 ]; then
    echo "Failed to download jose/LICENSE.md"
    exit 1
fi
echo "License file downloaded successfully to src/main/resources/static/js/jose/LICENSE.txt"

mkdir -p src/main/resources/static/js/sweetalert2
curl -s https://cdn.jsdelivr.net/npm/sweetalert2@11 -o src/main/resources/static/js/sweetalert2/sweetalert2.all.min.js
if [ $? -ne 0 ]; then
    echo "Failed to download sweetalert2/sweetalert2.all.min.js"
    exit 1
fi
echo "sweetalert2.js downloaded successfully to src/main/resources/static/js/sweetalert2/sweetalert2.all.min.js"

curl -s https://raw.githubusercontent.com/sweetalert2/sweetalert2/refs/heads/main/LICENSE -o src/main/resources/static/js/sweetalert2/LICENSE.txt
if [ $? -ne 0 ]; then
    echo "Failed to download sweetalert2/LICENSE.md"
    exit 1
fi
echo "License file downloaded successfully to src/main/resources/static/js/sweetalert2/LICENSE.txt"
