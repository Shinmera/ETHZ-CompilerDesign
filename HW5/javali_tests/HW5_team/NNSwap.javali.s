vtablearr_Record:
.long vtable_Object
vtablearr_Main:
.long vtable_Object
vtable_Object:
.long 0
vtable_Record:
.long vtable_Object
vtablearr_int:
.long vtable_Object
vtablearr_boolean:
.long vtable_Object
vtable_Main:
.long vtable_Object
.long Main_swap
.long Main_main
vtablearr_Object:
.long vtable_Object
vtablearr_void:
.long vtable_Object
.cstring
STR_NL:
.asciz "\n"
STR_D:
.asciz "%d"
STR_F:
.asciz "%.5f"
SCANF_STR_F:
.asciz "%f"
.data
.text
# Javali$CheckCast function___________________________________________
Javali$CheckCast:
enter $8, $0
and $-16, %esp
sub $16, %esp
movl 8(%ebp), %ecx
movl 12(%ebp), %eax
cmpl $0, %eax
je label1
movl 0(%eax), %eax
label0:
cmpl %eax, %ecx
je label1
cmpl $0, %eax
je label2
movl 0(%eax), %eax
jmp label0
label2:
movl $1, 0(%esp)
call _exit
label1:
leave
ret
# Javali$CheckNull function___________________________________________
Javali$CheckNull:
enter $8, $0
and $-16, %esp
sub $16, %esp
cmpl $0, 8(%ebp)
jne label3
movl $4, 0(%esp)
call _exit
label3:
leave
ret
# Javali$CheckNonZero function________________________________________
Javali$CheckNonZero:
enter $8, $0
and $-16, %esp
sub $16, %esp
cmpl $0, 8(%ebp)
jne label4
movl $7, 0(%esp)
call _exit
label4:
leave
ret
# Javali$CheckArraySize function______________________________________
Javali$CheckArraySize:
enter $8, $0
and $-16, %esp
sub $16, %esp
cmpl $0, 8(%ebp)
jge label5
movl $5, 0(%esp)
call _exit
label5:
leave
ret
# Javali$CheckArrayBounds function____________________________________
Javali$CheckArrayBounds:
enter $8, $0
and $-16, %esp
sub $16, %esp
movl 12(%ebp), %ecx
movl 8(%ebp), %eax
cmpl $0, %ecx
jl label6
cmpl 4(%eax), %ecx
jge label6
leave
ret
label6:
movl $3, 0(%esp)
call _exit
# Javali$Alloc function_______________________________________________
Javali$Alloc:
enter $8, $0
and $-16, %esp
sub $16, %esp
movl 8(%ebp), %eax
movl %eax, 0(%esp)
movl $1, 4(%esp)
call _calloc
leave
ret
# Javali$PrintNewLine function________________________________________
Javali$PrintNewLine:
enter $8, $0
and $-16, %esp
sub $16, %esp
movl $STR_NL, 0(%esp)
call _printf
leave
ret
# Javali$PrintInteger function________________________________________
Javali$PrintInteger:
enter $8, $0
and $-16, %esp
sub $16, %esp
movl 8(%ebp), %eax
movl %eax, 4(%esp)
movl $STR_D, 0(%esp)
call _printf
leave
ret
# Javali$ReadInteger function_________________________________________
Javali$ReadInteger:
enter $8, $0
and $-16, %esp
sub $16, %esp
leal 8(%esp), %eax
movl %eax, 4(%esp)
movl $STR_D, 0(%esp)
call _scanf
movl 8(%esp), %eax
leave
ret
# main() function_____________________________________________________
.globl _main
_main:
enter $8, $0
and $-16, %esp
  # Emitting new Main().main(...)
    # Emitting new Main().main(...)
    sub $12, %esp
      # Emitting new Main()
      push $4
      call Javali$Alloc
      add $4, %esp
      movl %eax, %edi
      movl $vtable_Main, 0(%edi)
    push %edi
    # Load "this" pointer
    movl 0(%esp), %edi
    sub $12, %esp
    push %edi
    call Javali$CheckNull
    add $16, %esp
    movl 0(%edi), %edi
    movl 8(%edi), %edi
    call *%edi
    add $16, %esp
    movl %eax, %edi
movl $0, %eax
leave
ret
# Class Main__________________________________________________________
  # 
  .text
# __Method Main.main__________________________________________________
  .globl Main_main
Main_main:
  # Variable     Offset
  # implicit=8 localSlot=12 sum=20
  enter $24, $0
  and $-16, %esp
  push %esi
  push %edi
  push %ebx
  jmp label7
# __Basic block 0_____________________________________________________
label7:
  # Exit to block 1
  jmp label8
# __Basic block 1_____________________________________________________
label8:
  # Return
  jmp label9
label9:
  movl $0, %eax
  pop %ebx
  pop %edi
  pop %esi
  leave
  ret
  .text
# __Method Main.swap__________________________________________________
  .globl Main_swap
Main_swap:
  # Variable     Offset
  # temp         -12
  # implicit=8 localSlot=16 sum=24
  enter $24, $0
  and $-16, %esp
  push %esi
  push %edi
  push %ebx
  movl $0, -12(%ebp)
  jmp label10
# __Basic block 0_____________________________________________________
label10:
    # Emitting temp = r1.a
      # Emitting r1.a
        # Emitting r1
        movl 12(%ebp), %edi
      push %edi
      call Javali$CheckNull
      add $4, %esp
      movl 4(%edi), %edi
    movl %edi, -12(%ebp)
    # Emitting r1.a = r2.a
      # Emitting r2.a
        # Emitting r2
        movl 8(%ebp), %edi
      push %edi
      call Javali$CheckNull
      add $4, %esp
      movl 4(%edi), %edi
      # Emitting r1
      movl 12(%ebp), %esi
    push %esi
    call Javali$CheckNull
    add $4, %esp
    movl %edi, 4(%esi)
    # Emitting r2.a = temp
      # Emitting temp
      movl -12(%ebp), %edi
      # Emitting r2
      movl 8(%ebp), %esi
    push %esi
    call Javali$CheckNull
    add $4, %esp
    movl %edi, 4(%esi)
  # Exit to block 1
  jmp label11
# __Basic block 1_____________________________________________________
label11:
  # Return
  jmp label12
label12:
  movl $0, %eax
  pop %ebx
  pop %edi
  pop %esi
  leave
  ret
# Class Record________________________________________________________
  # 
