    # Mixify: Beginner's Guide to Project Files (Hinglish) 🚀

Namaste! Agar aap ek non-coder hain lekin Mixify project mein chhote-mote badlav (changes) karna chahte hain, toh ye guide aapke liye hai. Isme humne bataya hai ki kaunsi file kya karti hai aur aap kahan safely edit kar sakte hain.

---

## 1. App ki Pehchan (Name & Version) ℹ️

| File Path | Ye kya karta hai | Kab edit karna hai | Safe Edits (Kya badal sakte hain) | Risk ⚠️ |
| :--- | :--- | :--- | :--- | :--- |
| [AndroidManifest.xml](file:///C:/Users/prod/Downloads/Mixify-main/app/src/main/AndroidManifest.xml) | Isme app ki permissions aur main settings hoti hain. | Jab app ki permissions (Camera, Storage etc.) dekhni hon. | `android:label` mein app ka display name dikhta hai. | Galat tag delete karne se app install nahi hogi. |
| [build.gradle.kts](file:///C:/Users/prod/Downloads/Mixify-main/app/build.gradle.kts) | App ka Unique ID (`applicationId`) aur version number yahan hota hai. | Jab app ka version badhana ho ya app ka ID change karna ho. | `versionCode` (number) aur `versionName` ("1.0.0") change kar sakte hain. | Dependencies chhedne se app build hona band ho jayegi. |
| [app_name.xml](file:///C:/Users/prod/Downloads/Mixify-main/app/src/main/res/values/app_name.xml) | App ka basic name yahan store hota hai. | Jab launcher par dikhne wala naam change karna ho. | `<string name="app_name">Mixify</string>` mein "Mixify" ko badal dein. | Kuch build settings ise override kar sakti hain. |

---
    
## 2. Text aur Strings (Sabse Safe Zone) ✍️~~~~

App ke andar dikhne wale buttons aur messages ko yahan se change karein.

| File Path | Ye kya karta hai | Safe Edit Example | Risk ⚠️ |
| :--- | :--- | :--- | :--- |
| [mixify_strings.xml](file:///C:/Users/prod/Downloads/Mixify-main/app/src/main/res/values/mixify_strings.xml) | App ka sara main English text yahan hai. | `<string name="cancel">Cancel</string>` ko `<string name="cancel">Radd karein</string>` kar sakte hain. | Sirf quotation marks `""` ke beech ka text badlein. `name="..."` ko mat chhedein. |

---

## 3. Design: Icons aur Colors 🎨
    
App ka look and feel yahan se control hota hai.

| Folder/File | Ye kya karta hai | Kaise badle | Risk ⚠️ |
| :--- | :--- | :--- | :--- |
| [drawables/](file:///C:/Users/prod/Downloads/Mixify-main/app/src/main/res/drawable/) | Isme icons aur buttons ki images (SVG/PNG) hoti hain. | Purani file ko replace karein new file se (naam same hona chahiye). | File extension (.xml ya .png) sahi honi chahiye. |
| [mipmap-*/](file:///C:/Users/prod/Downloads/Mixify-main/app/src/main/res/mipmap-hdpi/) | App ka logo/icon yahan hota hai. | Naye logo ko in folders mein "ic_launcher" naam se save karein. | Sabhi folders (hdpi, xhdpi etc.) mein update karna hoga. |
| [colors.xml](file:///C:/Users/prod/Downloads/Mixify-main/app/src/main/res/values/colors.xml) | Fixed colors (jaise Widget color) yahan hain. | HEX code (`#000000`) badal kar color change karein. | Galat HEX code se color nahi dikhega. |
| [Theme Folder](file:///C:/Users/prod/Downloads/Mixify-main/app/src/main/kotlin/com/mixify/music/ui/theme/) | Material 3 themes ka sara logic yahan hai. | Fonts aur Color scheme ke behavior ko modify karne ke liye. | Bahut saara coding logic hai, dhyan se! |

---

## 4. Screens ka Code (UI Folders) 📱

Agar aapko kisi screen ka layout ya design badalna hai:

- **Home Screen**: [HomeScreen.kt](file:///C:/Users/prod/Downloads/Mixify-main/app/src/main/kotlin/com/mixify/music/ui/screens/HomeScreen.kt) (Main UI yahi hai)
- **Search**: [search/](file:///C:/Users/prod/Downloads/Mixify-main/app/src/main/kotlin/com/mixify/music/ui/screens/search/) folder (Khone wala interface)
- **Library**: [library/](file:///C:/Users/prod/Downloads/Mixify-main/app/src/main/kotlin/com/mixify/music/ui/screens/library/) folder (Aapke gaane)
- **Settings**: [settings/](file:///C:/Users/prod/Downloads/Mixify-main/app/src/main/kotlin/com/mixify/music/ui/screens/settings/) folder (App ki setting screen)
- **Player**: [player/](file:///C:/Users/prod/Downloads/Mixify-main/app/src/main/kotlin/com/mixify/music/ui/player/) folder (Bottom player bar aur Full screen player)

**Navigation (Rasta) Kaise Kaam Karta Hai?**
[NavigationBuilder.kt](file:///C:/Users/prod/Downloads/Mixify-main/app/src/main/kotlin/com/mixify/music/ui/screens/NavigationBuilder.kt) ye decide karta hai ki ek screen se doosri screen par kaise jaana hai. Ye ek "Map" ki tarah hai.

---

## 5. ⚠️ KHATARNAK ZONE (Bilkul mat chhedo!) 🚫

Ye files sirf developers ke liye hain. Inme badlav se app turant crash ho sakti hai:

1. **[db/](file:///C:/Users/prod/Downloads/Mixify-main/app/src/main/kotlin/com/mixify/music/db/)**: Database logic. Yahan galti matlab saara saved data gaya!
2. **[playback/](file:///C:/Users/prod/Downloads/Mixify-main/app/src/main/kotlin/com/mixify/music/playback/)**: Music engine. Yahan galti matlab gaane nahi bajenge.
3. **[di/](file:///C:/Users/prod/Downloads/Mixify-main/app/src/main/kotlin/com/mixify/music/di/)**: Dependency Injection. Ye files app ke parts ko jorti hain.
4. **[api/](file:///C:/Users/prod/Downloads/Mixify-main/app/src/main/kotlin/com/mixify/music/api/)**: YouTube Music se communication karne wala hissa.

---

## 🌟 Sunehri Tip

> [!TIP]
> **Backup zaroor lein!** Kisi bhi file ko edit karne se pehle, uski ek copy Desktop par bana lein. Agar app crash ho jaye, toh bas original file wapas copy-paste kar dein aur sab theek ho jayega.

Happy Modding! 🎧
