#!/usr/bin/env bash
set -e

echo "Building all version groups..."
echo

failed=()
for f in \
    gradle.1_21_1.properties \
    gradle.1_21_4_5.properties \
    gradle.1_21_8_10.properties \
    gradle.1_21_11.properties
do
    ver=$(grep '^minecraft_version=' "$f" | cut -d= -f2)
    echo "=== Building for MC $ver ==="
    if ./gradlew build -Ptarget_minecraft_version="$ver"; then
        echo
    else
        echo "FAILED: MC $ver"
        failed+=("$ver")
        echo
    fi
done

if [ ${#failed[@]} -gt 0 ]; then
    echo "Build failed for: ${failed[*]}"
    exit 1
fi
echo "All versions built successfully!"
