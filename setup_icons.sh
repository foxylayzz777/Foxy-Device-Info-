#!/bin/bash
set -e

SRC_FILE="${1:-app/src/main/res/drawable/app_logo.png}"

if [ ! -f "$SRC_FILE" ]; then
    # Check if user named it logo.png or similar
    for candidate in app/src/main/res/drawable/logo.png app/src/main/res/drawable/ic_logo.png app/src/main/res/drawable/*.png; do
        if [ -f "$candidate" ]; then
            SRC_FILE="$candidate"
            break
        fi
    done
fi

if [ ! -f "$SRC_FILE" ]; then
    echo "Source image file not found. Please upload your logo to app/src/main/res/drawable/app_logo.png"
    exit 1
fi

echo "Processing exact original logo: $SRC_FILE"
cp "$SRC_FILE" "app/src/main/res/drawable/app_logo.png"

BASE="app/src/main/res"
for spec in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
  density="${spec%:*}"
  size="${spec#*:}"
  dir="$BASE/mipmap-$density"
  mkdir -p "$dir"
  
  # Remove template webp files to avoid AAPT2 conflicts
  rm -f "$dir/ic_launcher.webp" "$dir/ic_launcher_round.webp"
  
  # Generate exact square launcher icon
  convert "app/src/main/res/drawable/app_logo.png" -resize "${size}x${size}!" "PNG32:$dir/ic_launcher.png"
  
  # Generate exact circular masked round launcher icon
  radius=$((size / 2))
  convert "app/src/main/res/drawable/app_logo.png" -resize "${size}x${size}!" \
    \( -size "${size}x${size}" xc:none -fill white \
       -draw "circle $radius,$radius $radius,0" \) \
    -alpha set -compose DstIn -composite "PNG32:$dir/ic_launcher_round.png"

  echo "Generated $density (${size}x${size}px) icons."
done

# Update adaptive foreground layer with 66dp safe zone
cat << 'EOF' > app/src/main/res/drawable/ic_launcher_foreground.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:width="66dp"
        android:height="66dp"
        android:drawable="@drawable/app_logo"
        android:gravity="center" />
</layer-list>
EOF

# Update adaptive background with solid matching deep dark color
cat << 'EOF' > app/src/main/res/drawable/ic_launcher_background.xml
<?xml version="1.0" encoding="utf-8"?>
<color xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="#040814" />
EOF

echo "All Android launcher icons successfully configured from original logo!"
