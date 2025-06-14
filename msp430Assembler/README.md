# MSP430 Assembler ve Linker Projesi

Bu proje, MSP430 mikrokontrolcü ailesi için Java ile geliştirilmiş, çok dosyalı ve macro destekli bir assembler ve linker araç zinciridir.

## Özellikler

- **İki Aşamalı Assembler:** `PassOne` ve `PassTwo` ile klasik assembler mimarisi.
- **Linker:** Birden fazla nesne dosyasını (`.obj`) birleştirerek tek bir çalıştırılabilir çıktı üretir.
- **Macro Desteği:** Parametreli macro tanımlama, macro kütüphaneleri (`.mlib`) ve yerel etiket (`?`) desteği.
- **Modüler Tasarım:** `.ref` ve `.def` direktifleri ile modüller arası sembol paylaşımı.
- **Bölüm Desteği:** `.text`, `.data`, `.bss` gibi bellek bölümlerini yönetir.
- **Grafik Arayüz (GUI):** Kolay kullanım için bir masaüstü arayüzü içerir.
- **JSON Nesne Formatı:** İnsan tarafından okunabilir ve modern bir ara nesne dosyası formatı kullanır.

---

## Kurulum ve Çalıştırma

Projeyi çalıştırmak için iki ana yöntem bulunmaktadır: bir IDE (önerilen) kullanarak veya komut satırından.

### Yöntem 1: IDE ile Kurulum (IntelliJ IDEA Örneği)

Bu yöntem, geliştirme ve hata ayıklama için tavsiye edilir.

#### 1. Ön Gereksinimler

- **Java Development Kit (JDK):** Sisteminize JDK 17 veya daha yeni bir sürümün kurulu olduğundan emin olun.
- **JSON Kütüphanesi:**
  - [Buradan](https://github.com/stleary/JSON-java/releases) en son `json-java.jar` dosyasını indirin.
  - Proje ana dizininde `lib` adında bir klasör oluşturun ve indirdiğiniz `.jar` dosyasını bu klasörün içine kopyalayın.

#### 2. Projeyi Açma ve Ayarlama

1.  **Projeyi Açın:** IntelliJ IDEA'da `File -> Open...` seçeneğine gidin ve projenin ana klasörünü seçerek açın.
2.  **Kütüphaneyi Ekleyin:**
    -   `File -> Project Structure...` menüsünü açın.
    -   Sol taraftan `Modules`'ü seçin.
    -   Sağdaki sekmelerden `Dependencies`'e tıklayın.
    -   `+` simgesine tıklayın, `JARs or Directories...`'i seçin.
    -   Açılan pencerede projenizdeki `lib/json-java.jar` dosyasını bulun ve seçin.
    -   `OK`'e tıklayarak pencereleri kapatın.

#### 3. Uygulamayı Çalıştırma

**a) Grafik Arayüzü (GUI) Çalıştırma:**

1.  Sol taraftaki Proje Gezgini panelinde `src/assembler/AssemblerGUI.java` dosyasını bulun.
2.  Dosya içeriğindeki `main` metodunun yanındaki yeşil ▶️ (Play) simgesine tıklayın ve `Run 'AssemblerGUI.main()'` seçeneğini seçin.
3.  Uygulama arayüzü başlayacaktır.

**b) Komut Satırı Versiyonunu Çalıştırma (Argümanlı):**

1.  Proje Gezgini panelinde `src/assembler/Main.java` dosyasını bulun.
2.  `main` metodunun yanındaki yeşil ▶️ simgesine tıklayıp çalıştırın. Başlangıçta argüman eksikliği nedeniyle hata verecektir.
3.  Üst menüden `Run -> Edit Configurations...` seçeneğine gidin.
4.  Açılan pencerede solda `Main`'i seçin.
5.  **Program arguments** kutusuna derlemek istediğiniz assembly dosyasının yolunu yazın. Örneğin: `src/main.asm`
6.  `OK`'e tıklayıp kaydedin ve şimdi `Main` yapılandırmasını tekrar çalıştırın.

---

### Yöntem 2: Komut Satırı ile Kurulum

Bu yöntem, projeyi bir IDE olmadan hızlıca derlemek ve çalıştırmak için kullanılır.

#### 1. Gereksinimler

- **Java Development Kit (JDK):** JDK 17+ kurulu ve komut satırından erişilebilir olmalı.
- **JSON Kütüphanesi:** Yukarıdaki "Ön Gereksinimler" bölümünde açıklandığı gibi `lib/json-java.jar` dosyası yerinde olmalıdır.

#### 2. Derleme

Bir terminal açın ve projenin ana dizininde aşağıdaki komutu çalıştırın.

**Windows:**
```bash
# Çıktı için 'out' klasörünü oluştur
mkdir out
# Java kaynak kodlarını derle (UTF-8 karakter setiyle)
javac -encoding UTF-8 -cp "lib/json-java.jar;." -d out src/assembler/*.java
```

**Linux/macOS:**
```bash
# Çıktı için 'out' klasörünü oluştur
mkdir -p out
# Java kaynak kodlarını derle
javac -cp "lib/json-java.jar:." -d out src/assembler/*.java
```

#### 3. Çalıştırma

**a) Grafik Arayüz (GUI) ile:**

**Windows:**
`java -cp "lib/json-java.jar;out" assembler.AssemblerGUI`

**Linux/macOS:**
`java -cp "lib/json-java.jar:out" assembler.AssemblerGUI`

**b) Komut Satırından:**

**Windows:**
`java -cp "lib/json-java.jar;out" assembler.Main "path/to/your/file.asm"`

**Linux/macOS:**
`java -cp "lib/json-java.jar:out" assembler.Main "path/to/your/file.asm"`

---

## Uygulama Kullanımı (GUI)

1.  **Uygulamayı Başlatın.**
2.  **Dosya Açın:** `File -> Open` ile bir veya daha fazla `.asm` dosyası seçin.
3.  **Derle ve Linkle:** `Assemble -> Assemble and Link All` menüsüne tıklayın.
4.  **Sonuçları Görüntüleyin:** Panellerde kaynak kod, sembol tablosu ve bellek haritası güncellenecektir. Çıktı dosyaları ana dizinde oluşturulur.

## Proje Çıktıları

-   `out/`: Derlenmiş Java `.class` dosyaları.
-   `obj/`: Ara nesne dosyaları (`.obj`).
-   `linked.txt`: Son çalıştırılabilir TI-TXT formatındaki çıktı.
-   `linked.map`: Sembol haritası. 