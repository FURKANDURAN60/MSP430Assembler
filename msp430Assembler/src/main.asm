; ========================
; Ana program
; ========================
.mlib "../macrolib.lib" ; Macro kutuphanesini dahil et

.ref print_msg       ; Başka modülden gelecek

BUF_SIZE  .equ  16
VAL       .set  0x1A2B

.text
.org 0xF800

START:
    MOV #BUF_SIZE, R4     ; Immediate (constant)
    MOV #VAL, R5          ; Immediate (set ile tanımlı)
    MOV #'A', R6          ; Char literal
    MOV &data_value, R7   ; Symbolic indirgeme
    ADD 4(R4), R5         ; Indexed adresleme
    MOV @R6, R7           ; Indirect
    MOV @R7+, R8          ; Auto-increment
    
    delay #5000           ; Macro cagrisi: 5000 donguluk bekleme
    
    CALL #print_msg     ; Başka modüle çağrı
    JMP NEXT              ; Jump format
    .float 3.14           ; 4 byte float veri
    .string "HELLO"       ; String + null byte

NEXT:
    NOP
    RETI

.data
.org 0x2000

data_value:  .word 0xABCD, 0x1234
binary_val:  .word 0b10101010
octal_val:   .word 123Q
char_val:    .byte 'Z'
hex_h_val:   .byte 5Ah
float_data:  .float -1.5 