# Yenilikler

[🇬🇧 English](CHANGELOG.en.md) · [🇷🇺 Русский](CHANGELOG.md)

Dinamik Ada, [Arusaky/Dynamic-Island-](https://github.com/Arusaky/Dynamic-Island-) projesinin Türkçe sürümüdür. Eski sürümlerin notları için İngilizce değişiklik günlüğüne bakın.

## [1.9.0] — 2026-11-21

- 🆕 **Orijinal 1.7.2'nin tüm yenilikleri alındı**: şarkı sözleri ve karaoke, Özet (saat, adımlar, hava durumu), kısayollar, ses/parlaklık denetimleri, yeni Liquid Glass ve bulanıklık gücü, kayıt kartı, bildirim geçmişi, yazı tipi seçimi, ayarlarda arama, erişim noktası kartı ve birçok düzeltme.
- 🇹🇷 Türkçe yeni dil sistemine taşındı ve 460'tan fazla yeni metin çevrildi.
- ⚫ Kapalıyken siyah, 🔕 Zil/Sessiz düğmesi, 🚫 çift bildirim yok ve ⏰ Xiaomi alarm düzeltmesi yeni sürümde korundu.
- Eski bulanıklık ayarı yerine orijinalin yeni sistem bulanıklığı kullanılıyor; ayarlarınız korunur.

## [1.8.4] — 2026-11-13

- 🚫 **Çift bildirim yok**: mesaj adada görününce uygulamanın kendi açılır kutusu artık çıkmıyor. Bildirim kapatılmaz: sesi ve titreşimi çalar, bildirim panelinde kalır (root gerekir; Bildirimler → Çift bildirim olmasın). Her bildirim türünün ilk bildiriminde kutu bir kez daha çıkabilir.
- 🔕 Xiaomi'de zil/sessiz plakası kapatıldı: HyperOS bunu kendi adasında zaten gösteriyor, ikisi üst üste biniyordu.

## [1.8.3] — 2026-11-13

- 🔕 **Sessiz düğmesi**: boş adaya dokununca zamanlayıcı yerine **Zil / Sessiz** düğmesi çıkıyor. Dokununca zil ↔ sessiz geçer; düğme o anki modu gösterir (Zil, Titreşim, Sessiz — kırmızı). İlk seferde «Rahatsız Etmeyin» erişimi istenir. Zamanlayıcıyı geri isterseniz: Davranış → Boş adaya dokunma.

## [1.8.2] — 2026-11-04

- ⚫ **Kapalıyken siyah düzeltildi**: gerçek bulanıklık açıkken küçük ada siyah yerine saydam görünüyordu. Sistem bulanıklık katmanı altındaki her şeyi siliyordu; camın gövdesi artık onun üstüne çiziliyor. Küçük ada yine siyah, açılan ada da cam rengini ve parlamalarını doğru gösteriyor.

## [1.8.1] — 2026-11-04

- 🌫️ **Bulanıklık gücü**: Görünüm → Renk ve cam altında yeni kaydırıcı. Varsayılan bulanıklık daha güçlü (%70): arkadaki ikonlar ve yazılar HyperOS kontrol merkezi gibi buzlu cama dönüşür, adadaki yazılar daha rahat okunur.

## [1.8.0] — 2026-11-04

- 🫧 **Gerçek Liquid Glass**: açılan ada artık arkasındaki ekranı gerçekten bulanıklaştırıyor — HyperOS 3 kontrol merkezi ve bildirim paneliyle aynı sistem bulanıklığı. Bulanıklık adanın açılma animasyonunu ve köşelerini birebir izler (Görünüm → Renk ve cam → **Gerçek bulanıklık**).
- ⚫ **Kapalıyken siyah**: küçük ada siyah kalır ve kamera deliğini gizler; açılınca yumuşak bir geçişle cama dönüşür. Kapatırsanız ada her zaman cam olur.
- ℹ️ Bulanıklık için telefonda **Ayarlar → Ekran → Gelişmiş materyal** açık olmalı. Kapalıysa ya da pil tasarrufu bulanıklığı kapatırsa ada eskisi gibi boyanmış camla görünür.

## [1.7.1] — 2026-11-04

- ⏰ **Alarm saati düzeltildi**: Xiaomi Saat sisteme alarmı 1 saat kayık bildirdiğinde ada yanlış saat gösteriyordu (07:40 yerine 06:40). Ada artık Saat uygulamasının yazdığı “Alarm … içinde” süresinden gerçek saati öğreniyor ve kaymayı kalıcı olarak düzeltiyor.

## [1.7.0] — 2026-11-04

- 🇹🇷 **Türkçe dil desteği**: uygulama ve ada tamamen Türkçe (Özellikler → Genel → Dil). Yeni kurulumda varsayılan dil Türkçe; “Sistem varsayılanı” telefon Türkçeyse Türkçe seçer.
- 🕐 **Türkçe tarih ve birimler**: “4 Kasım Çarşamba”, “7 sa 30 dk”, navigasyonda dk/sa.
- 🧭 **Türkçe ekranları tanıma**: Saat uygulamasındaki Zamanlayıcı, Kronometre, Başlat, Duraklat, Sıfırla; navigasyonda “Sola/Sağa dönün”, “Düz devam edin”; Türk ve Fransız bankalarının ödeme ekranları ve SMS'leri.
- 🏝️ **Kendi uygulamamız**: adı Dinamik Ada, paket adı `com.tr1action.island`, kendi imza anahtarı. Güncellemeler Tr1Action/Dynamic-Island- deposundan gelir. Orijinal uygulamayla çakışmaz, onu kaldırabilirsiniz.
