; =============================================
; utils.asm - Yardımcı fonksiyonlar modülü
; =============================================

.def print_msg    ; Bu fonksiyonu diğer modüllerin kullanabilmesi için dışa aktar

.text             ; Kod bölümü
print_msg:
    ; Bu fonksiyonun gerçek bir MSP430'da çalışacak
    ; bir çıktı mekanizması olmadığını varsayıyoruz.
    ; Amacımız sadece sembollerin doğru linklenip linklenmediğini
    ; ve CALL komutunun doğru adrese dallandığını görmektir.
    ; Parametre olarak R12'deki adresi kullanır.
    MOV  R12, R11     ; Adresi R11'e al (sadece bir işlem olsun diye)
    RETI              ; Çağrıldığı yere dön 