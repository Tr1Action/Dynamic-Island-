<div align="center">

# 🏝️ Dinamik Ada

**Her Android telefonun kamerasının etrafında iPhone tarzı Dynamic Island — Türkçe**

Yanıtlanabilen bildirimler · navigasyon · ödemeler · sözlü müzik · aramalar · zamanlayıcılar · indirmeler · şarj · el feneri · sistem olayları

### [⬇️ Son sürümü indir](https://github.com/Tr1Action/Dynamic-Island-/releases/latest)

</div>

Bu, [Akira9021/Dynamic-Island-](https://github.com/Akira9021/Dynamic-Island-) projesinin Türkçe sürümüdür: uygulama ve ada Türkçe, Saat, navigasyon ve banka ekranlarındaki Türkçe metinler tanınır. Paket adı `com.tr1action.island` olduğu için orijinal uygulamayla çakışmaz.

## 📥 Kurulum

1. [Son sürüm](https://github.com/Tr1Action/Dynamic-Island-/releases/latest) sayfasından **`DinamikAda.apk`** dosyasını indirip kurun.
2. Uygulamayı açın → Ana sayfa → **Erişilebilirliği aç** → “Dinamik Ada”yı etkinleştirin.
   > Android 13+'ta “Kısıtlanmış ayar” çıkarsa: **Uygulama bilgisi → ⋮ → Kısıtlanmış ayarlara izin ver**, sonra 2. adımı tekrarlayın.
3. Ana sayfa → **İzinler** → **Bildirim erişimi**ni açın (müzik, indirmeler ve zamanlayıcılar için).
4. Aynı yerde **Aramalar**'a dokunup telefon, arama kaydı ve kişilere izin verin.
5. **Görünüm → Konum ve boyut**: kırmızı kapsülü tam kameranın üstüne getirin.

> [!TIP]
> Xiaomi / HyperOS'ta ada kayboluyorsa: **İzinler → Adayı açık tut** — kısıtlamasız pil, otomatik başlatma ve son uygulamalarda kilitleme.

## 👆 Hareketler

| Hareket | Sonuç |
| --- | --- |
| Dokunma | Aç (değiştirilebilir) |
| Aşağı çekme | Aç |
| Yukarı çekme / dışına dokunma | Kapat |
| Sola / sağa itme | Sonraki / önceki şarkı |

## 🛠️ Geliştirme

Her push GitHub Actions'ta APK derler. Yeni sürüm için `app/build.gradle.kts` içinde `versionName` ve `versionCode`'u artırın ve `CHANGELOG.tr.md`, `CHANGELOG.en.md`, `CHANGELOG.md` dosyalarına `## [x.y.z]` bölümü ekleyin — varsayılan dala push edilince APK'lı sürüm kendiliğinden yayınlanır.

İmza anahtarı depoda tutulmaz: CI, APK'yı GitHub secrets'taki `KEYSTORE_BASE64` ve `KEYSTORE_PASSWORD` ile imzalar.

Türkçe metinler `app/src/main/assets/tr_strings.json` dosyasındadır (İngilizce metin → Türkçe karşılığı).

## 💜 Teşekkürler

Orijinal uygulama: [Akira9021](https://github.com/Akira9021/Dynamic-Island-). Fikir: [Dynamic Island for Windows](https://github.com/mihailkotovski/DynamicIsland). Şarkı sözleri: [LRCLIB](https://lrclib.net).
