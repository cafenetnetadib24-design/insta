# راهنمای ارسال پروژه به گیت‌هاب و بیلد خودکار (GitHub Actions)

این پروژه به طور کامل برای ارسال به گیت‌هاب و ساخت خودکار فایل‌های **APK** و **AAB** (نسخه ریلیز) آماده شده است.

---

## 🚀 مراحل ارسال پروژه به گیت‌هاب (GitHub)

1. یک مخزن جدید (Repository) در [GitHub](https://github.com/new) بسازید.
2. در ترمینال سیستم خود، دستورات زیر را برای ارسال کد وارد کنید:

```bash
git init
git add .
git commit -m "Initial commit - Instagram Video Downloader"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPOSITORY_NAME.git
git push -u origin main
```

*(به جای `YOUR_USERNAME` و `YOUR_REPOSITORY_NAME` اطلاعات مخزن خودتان را قرار دهید)*

---

## 📦 نحوه کار اکشن ساخت نسخه ریلیز (GitHub Action)

به محض اینکه کد را به شاخه `main` یا `master` ارسال کنید، GitHub Actions به طور خودکار شروع به کار کرده و فایل‌های زیر را تولید می‌کند:

1. **`app-release.apk`**: برای نصب مستقیم روی گوشی‌های اندروید
2. **`app-release.aab`**: فایل App Bundle جهت انتشار در گوگل پلی (Google Play Store)

---

## 📥 نحوه دریافت فایل‌های خروجی (APK و AAB)

1. به صفحه پروژه خود در گیت‌هاب بروید.
2. تب **Actions** را از بالای صفحه انتخاب کنید.
3. روی آخرین بیلد اجرا شده کلیک کنید.
4. در پایین صفحه در بخش **Artifacts**، دو فایل زیر را دانلود کنید:
   - `Android-Release-APK`
   - `Android-Release-AAB`

---

## 🔐 (اختیاری) تنظیم کلید امضا اختصاصی (Signing Keystore)

در حالت پیش‌فرض اگر کلیدی تنظیم نکنید، پروژه با کلید ایمن پیش‌فرض امضا و ساخته می‌شود تا بیلد بدون خطا تمام شود.

اگر می‌خواهید از کلید اختصاصی خودتان برای امضا استفاده کنید:
1. فایل کلید خود را به صورت Base64 تبدیل کنید:
   ```bash
   base64 -w 0 my-release-key.jks > key.txt
   ```
2. در گیت‌هاب به مسیر **Settings > Secrets and variables > Actions** بروید.
3. متغیرهای زیر را اضافه کنید:
   - `SIGNING_KEY`: محتوای Base64 کلید
   - `STORE_PASSWORD`: رمز عبور KeyStore
   - `KEY_ALIAS`: آلیس کلید
   - `KEY_PASSWORD`: رمز عبور کلید
