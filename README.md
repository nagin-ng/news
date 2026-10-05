# GitHub Uploader (Android app)

Phone se **ZIP ya folder import karo, GitHub par upload karo**, aur GitHub Actions se APK build karwao.

## App kya karta hai

- ZIP file ya phone ka folder chunta hai (Android Studio project).
- Poora project **ek hi commit** me GitHub par push karta hai.
- Repo na ho to naya bana deta hai (private ya public).
- `build/`, `.gradle/`, `.idea/`, `local.properties`, keystore (`*.jks`) jaisi bekaar ya secret files **upload nahi karta**.
- ZIP ke andar ek hi top-level folder ho to usse hata deta hai, taaki `settings.gradle` repo ke root me aaye.
- Agar project me workflow nahi hai to **APK build workflow** khud add kar deta hai.
- Builds tab me GitHub Actions ka status dikhta hai, aur Releases page se APK seedha download hota hai.

## Pehli baar is app ka APK kaise banaye

App ka APK banane ke liye is project ko ek baar GitHub par daalna padega (baad me app khud ye kaam karega).

1. GitHub par naya repo banao.
2. Is ZIP ko extract karke saari files repo me upload karo (PC par `git push` sabse aasan hai):
   ```bash
   git init && git add . && git commit -m "GitHub Uploader"
   git branch -M main
   git remote add origin https://github.com/<username>/<repo>.git
   git push -u origin main
   ```
   Dhyan rakho ki `.github/workflows/android-build.yml` upload ho. Agar browser upload me ye hidden folder miss ho jaye to **Add file -> Create new file** me path `.github/workflows/android-build.yml` likhkar file ka content paste karo.
3. Repo me **Settings -> Actions -> General -> Workflow permissions -> Read and write permissions** select karo.
4. **Actions** tab me build chalega (3-6 min). Khatam hone par **Releases** page par `Build N` me `app-debug.apk` milega.
5. APK phone me install karo (Unknown sources allow karna padega).

## App kaise use kare

1. **Settings tab**: GitHub token banao (button se) aur paste karke "Token check karo" dabao.
   Classic token me `repo` aur `workflow` scope chahiye.
2. **Upload tab**: ZIP/Folder chuno, repo ka naam likho, **GitHub par upload karo** dabao.
3. Build ke baad **Releases** se APK download karo.

## Dhyan rakhne wali baatein

- Is app ka debug APK ek fixed key (`app/debug.keystore`) se sign hota hai, isliye naya version purane ke upar install ho jata hai.
- Jo project tum upload karoge uske APK ka signature har build me alag ho sakta hai (runner ki random debug key). Update se pehle purana app uninstall karna pad sakta hai. Isse bachne ke liye us project me bhi ek fixed `debug.keystore` rakh sakte ho.
- 25 MB se badi file skip hoti hai.
- Generic workflow JDK 17 aur Gradle 8.9 par chalta hai. Bahut purane projects ke liye workflow badalna pad sakta hai.
- Mirror option ON karne par repo ki wo files delete hongi jo is upload me nahi hain.

## Local build

```bash
./gradlew assembleDebug   # APK: app/build/outputs/apk/debug/app-debug.apk
```

Android Studio Ladybug (2024.2) ya naya, JDK 17.
