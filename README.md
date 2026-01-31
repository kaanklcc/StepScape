StepScape 🚶‍♂️📊

StepScape, kullanıcıların günlük adım sayılarını takip edebildiği, Health Connect ve Google Fit üzerinden verileri çekip görselleştiren ve bu verileri Firebase ile buluta senkronize eden bir Android uygulamasıdır.
 Uygulamanın Amacı
Bu projedeki temel amacım, hem Health Connect entegrasyonunu öğrenmek hem de gerçek bir senaryo üzerinden temiz mimari ve modern Android geliştirme prensiplerini uygulamaktı.

## Uygulama sayesinde kullanıcılar:
- Günlük adım sayılarını görebilir
- Geçmiş adım verilerini grafik üzerinde inceleyebilir
- İnternet olmasa bile geçmiş verilere erişebilir
- Senkronize edilmemiş verileri otomatik olarak Firebase’e gönderebilir
- Google ile güvenli bir şekilde giriş yapabilir

## Kullanılan Teknolojiler
- Kotlin
- MVVM mimarisi
- Clean Architecture (presentation – domain – data katmanları)
- Repository pattern
- Hilt (Dependency Injection)
- Coroutines & Flow (asenkron işlemler ve state yönetimi)
- Room (lokal veritabanı)
- Firebase Authentication & Firestore
- MPAndroidChart (grafik gösterimi)
- XML + ViewBinding (UI)

Bu yapıyı özellikle kodun okunabilir, test edilebilir ve sürdürülebilir olması için tercih ettim.


## Veri Kaynakları
-Health Connect 
-Google Fit 

## Firebase ve Senkronizasyon Yapısı
-Firebase tarafında:
-Authentication (Email/Şifre ve Google ile giriş)
-Firestore (adım verilerinin tutulması)
-Uygulamada kullanıcı her ana ekrana geldiğinde Firestore senkronizasyonu tetiklenir.
-Eğer daha önce senkronize edilmemiş lokal veriler varsa, otomatik olarak buluta gönderilir.

 ## Lokal Veri Yönetimi (Room)
-Room kullanmamın sebebi, kullanıcının geçmiş adım verilerini internet olmadan da görebilmesini sağlamaktır.
-Bu sayede uygulama sürekli Health Connect’e bağlı kalmadan çalışabilmektedir.

## Asenkron Yapı ve State Yönetimi
-Coroutines ile uzun süren işlemler ana thread’i bloklamadan yapılır
-Flow kullanarak UI’ın veriye reaktif şekilde tepki vermesi sağlanır

## Grafik Gösterimi
- Adım verileri MPAndroidChart kullanılarak grafik üzerinde gösterilmektedir.

## Karşılaşılan Zorluklar
- Health Connect API davranışları
- Health Connect’in bazı Android sürümlerinde farklı davranması, izin yönetimi ve veri çekme süreçlerinde ekstra uyumluluk geliştirmeyi gerektirdi.
- Health Connect öğrenme süreci
- Health Connect’i ilk defa bu projede kullandım. Uygulamaya doğru şekilde bağlamak ve verileri anlık gösterebilmek düşündüğümden daha uzun bir araştırma süreci gerektirdi.
- Grafik verisi kısıtları
- Google Fit üzerinden aylık, 6 aylık ve yıllık filtrelenmiş verileri doğrudan alamadığım için bu verileri grafikte göstermek istediğim gibi mümkün olmadı.

##  Kurulum

1.  **Projeyi Klonlayın:**
    Terminalinizi açın ve aşağıdaki komutu çalıştırın:
    ```bash
    git clone https://github.com/kaanklcc/StepScape.git
    ```

2.  **Projeyi Android Studio'da Açın:**
    Android Studio'yu başlatın ve `StepScape` klasörünü seçerek projeyi açın. Gradle senkronizasyonunun (Sync) tamamlanmasını bekleyin.

## ⚙️ Yapılandırma (Önemli!)

Bu proje **Firebase** servislerini kullanmaktadır. Güvenlik nedeniyle `google-services.json` dosyası GitHub deposunda **bulunmamaktadır**. Uygulamayı çalıştırabilmek için kendi Firebase projenizi oluşturmanız gerekmektedir.

### Adım 1: Firebase Projesi Oluşturma
1.  [Firebase Console](https://console.firebase.google.com/)'a gidin.
2.  Yeni bir proje oluşturun (veya mevcut bir projeyi seçin).
3.  Proje genel bakış sayfasından **Android** simgesine tıklayarak yeni bir uygulama ekleyin.
4.  **Paket Adı** olarak şunu girin: `com.example.stepscape`
5.  Uygulamayı kaydedin.

### Adım 2: google-services.json Dosyası
1.  Firebase kurulum ekranından `google-services.json` dosyasını indirin.
2.  Bu dosyayı projenizin `app/` klasörünün içine yapıştırın.
    *   Dosya yolu şöyle olmalıdır: `StepScape/app/google-services.json`

### Adım 3: Google Sign-In, SHA-1/SHA-256 ve Web Client ID

Google ile giriş (Credential Manager) özelliğinin hatasız çalışması için **hem SHA-1 hem de SHA-256** parmak izlerinizi eklemeniz ve **Web Client ID**'yi yapılandırmanız gerekmektedir.

1.  **SHA-1 ve SHA-256 Ekleme:**
    *   Android Studio'da sağ taraftaki **Gradle** panelini açın.
    *   `Tasks > android > signingReport` görevini çalıştırın.
    *   Konsol çıktısında `SHA1` ve `SHA-256` değerlerini kopyalayın.
    *   Firebase Console'da **Proje Ayarları (Project Settings)** > **Genel (General)** sekmesine gidin.
    *   "Parmak İzi Ekle" diyerek hem SHA-1 hem de SHA-256 kodlarını projenize ekleyin.

2.  **Web Client ID (Önemli):**
    *   Firebase Console'da **Authentication > Sign-in method > Google** bölümüne gidin.
    *   Yapılandırma penceresinde "Web SDK configuration" başlığı altında **Web client ID** değerini kopyalayın.
    *   Projenizde `app/src/main/res/values/strings.xml` dosyasını açın.
    *   `default_web_client_id` alanına bu değeri yapıştırın:
        ```xml
        <string name="default_web_client_id">BURAYA_WEB_CLIENT_ID_YAPISTIRIN</string>
        ```

### Adım 4: Firestore Veritabanı
1.  Firebase Console'da **Firestore Database** sekmesine gidin.
2.  "Veritabanı oluştur" butonuna tıklayın.
3.  Test modunda veya üretim modunda başlatın (Test modu geliştirme için daha kolaydır).

## ▶️ Çalıştırma

Tüm yapılandırmaları tamamladıktan sonra:

1.  Android Studio'da üst menüden **Run > Run 'app'** seçeneğine tıklayın (veya yeşil oynatma butonuna basın).

