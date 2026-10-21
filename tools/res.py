"""Writes the small hand-made resources: theme, colours, launcher icon and the camera's file paths.

python tools/res.py
"""
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app/src/main/res"

# A terracotta pot on the sill with a sprout of three leaves.
POT = """    <path android:fillColor="#33000000" android:pathData="M41,85.5h26a13,2.2 0,0 1,-26 0z" />
    <path android:fillColor="#BF6640" android:pathData="M40.5,64h27l-3,21h-21z" />
    <path android:fillColor="#A9552F" android:pathData="M58,64h9.5l-3,21h-8.4z" />
    <path android:fillColor="#D07A52" android:pathData="M38,57.5h32v7h-32z" />
    <path android:fillColor="#B85E39" android:pathData="M62,57.5h8v7h-8z" />
    <path android:fillColor="#4A3426" android:pathData="M40,57.8a14,2.6 0,0 1,28 0a14,2.6 0,0 1,-28 0z" />
    <path android:fillColor="#00000000" android:strokeColor="#4C7A35" android:strokeWidth="2.4" android:strokeLineCap="round"
        android:pathData="M54,57.5C54,50 53,45 54,38" />
    <path android:fillColor="#5E8F40" android:pathData="M53.6,48.5C48,49 41.5,46 39,39.5C45.5,38.5 51.5,41.5 53.6,48.5z" />
    <path android:fillColor="#6FA34A" android:pathData="M54.2,44C59,42.5 64.5,38 66,31.5C59.5,31.5 54.8,36.5 54.2,44z" />
    <path android:fillColor="#7DB257" android:pathData="M54,38.5C52,35 52.5,31 55,28C57,31.5 56.5,35.5 54,38.5z" />"""

FILES = {
    "values/themes.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Sill" parent="Theme.AppCompat.Light.NoActionBar">
        <item name="android:windowBackground">@color/window</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="colorPrimary">@color/clay</item>
        <item name="colorAccent">@color/clay</item>
    </style>
</resources>
""",
    "values/colors.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="window">#FFF1E9DA</color>
    <color name="clay">#FFB65A36</color>
</resources>
""",
    "mipmap-anydpi-v26/ic_launcher.xml": """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_monochrome" />
</adaptive-icon>
""",
    "drawable/ic_launcher_background.xml": """<?xml version="1.0" encoding="utf-8"?>
<!-- A limewashed wall, a window with a morning sky and the sill under it. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#F1E9DA" android:pathData="M0,0h108v108h-108z" />
    <path android:fillColor="#F7F3EA" android:pathData="M20,8h68v58h-68z" />
    <path android:fillColor="#9CC9E6" android:pathData="M24,12h28v20h-28z M56,12h28v20h-28z M24,36h28v28h-28z M56,36h28v28h-28z" />
    <path android:fillColor="#9DBB8C" android:pathData="M24,52q14,-6 28,-2v14h-28z M56,50q14,-5 28,0v14h-28z" />
    <path android:fillColor="#EDE6D8" android:pathData="M0,64h108v9h-108z" />
    <path android:fillColor="#D9CFBD" android:pathData="M0,73h108v5h-108z" />
</vector>
""",
    "drawable/ic_launcher_foreground.xml": f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
{POT}
</vector>
""",
    "drawable/ic_monochrome.xml": """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FFFFFF" android:pathData="M38,57.5h32v7h-2.5l-3,20.5h-21l-3,-20.5h-2.5z" />
    <path android:fillColor="#00000000" android:strokeColor="#FFFFFF" android:strokeWidth="2.4" android:strokeLineCap="round"
        android:pathData="M54,56C54,50 53,45 54,38" />
    <path android:fillColor="#FFFFFF" android:pathData="M53.6,48.5C48,49 41.5,46 39,39.5C45.5,38.5 51.5,41.5 53.6,48.5z M54.2,44C59,42.5 64.5,38 66,31.5C59.5,31.5 54.8,36.5 54.2,44z M54,38.5C52,35 52.5,31 55,28C57,31.5 56.5,35.5 54,38.5z" />
</vector>
""",
    "xml/file_paths.xml": """<?xml version="1.0" encoding="utf-8"?>
<!-- The phone's camera writes a watering's photo here. -->
<paths>
    <files-path name="photos" path="photos/" />
</paths>
""",
}


def main():
    for name, text in FILES.items():
        f = RES / name
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text(text, encoding="utf-8", newline="\n")


if __name__ == "__main__":
    main()
