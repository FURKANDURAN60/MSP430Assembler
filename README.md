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

## Kurulum ve Başlatma

Projeyi yerel makinenizde derlemek ve çalıştırmak için aşağıdaki adımları izleyin.

### 1. Gereksinimler

- **Java Development Kit (JDK):** Sisteminizde JDK 23 veya daha yeni bir sürümün kurulu olması gerekmektedir.
- Herhangi bir IDE üzerinden de işlemleri gerçekleştirebilirsiniz.(Intellij önerilir.)
- /lib klasörü içerisindeki rsyntaxtextarea-3.3.2 ve json jar dosyaları kütüphane olarak projeye tanıtılmalıdır.

### 3. Çalıştırma

Derleme başarılı olduktan sonra uygulamayı çalıştırabilirsiniz:

#### Grafik Arayüz (GUI) ile Çalıştırma

Kullanıcı dostu arayüzü başlatmak için aşağıdaki komutu kullanın:


## Uygulama Kullanımı (GUI)

1.  **Uygulamayı Başlatın:
2.  **Dosya Açın:** Menü çubuğundan `File -> Open` seçeneğine tıklayarak bir veya daha fazla `.asm` uzantılı assembly dosyasını seçin.
3.  **Derleme ve Linkleme:** `Assemble -> Assemble and Link All` menü seçeneğine tıklayın. Bu işlem:
    -   Açık olan tüm `.asm` dosyalarını tek tek assemble eder.
    -   Her biri için `obj/` klasöründe bir `.obj` dosyası oluşturur.
    -   Tüm `.obj` dosyalarını birleştirerek linkleme işlemini yapar.
    -   Nihai çıktıyı proje ana dizininde `linked.txt` (TI-TXT formatında) ve `linked.map` (sembol haritası) olarak oluşturur.
4.  **Sonuçları Görüntüleyin:**
    -   **Source Paneli:** Açtığınız kaynak kodları gösterir.
    -   **Symbol Table Paneli:** Linkleme sonrası oluşan son sembol tablosunu gösterir.
    -   **Memory View Paneli:** Linklenmiş programın bellek haritasını hexadecimal olarak gösterir.

## Proje Çıktıları

Derleme ve linkleme sonrası aşağıdaki dosyalar ve klasörler oluşturulur:

-   `out/`: Derlenmiş Java `.class` dosyalarını içerir.
-   `obj/`: Her assembly kaynak dosyası için oluşturulan ara nesne dosyalarını (`.obj`) içerir.
-   `linked.txt`: Tüm programın linklenmiş halini içeren, TI-TXT formatındaki son çıktıdır. Bu dosya bir MSP430 yükleyicisi tarafından kullanılabilir.
-   `linked.map`: Programdaki sectionların adresleri ve ne kadar yer kapladıkları hakkında kullanıcıya hitap eden çıktı dosyasıdır.

## İletişim ve Destek

b210109048@subu.edu.tr
Furkan DURAN
