.data
format: .ascii "%d"

.text
.global _main

_main:
// Allocate the stack for our variable and for calls
// then save the address to the variable to EAX
subl    $12, %esp
movl    $0, 8(%esp)
leal    8(%esp), %eax

// Call to scanf to read into EAX
movl    %eax, 4(%esp)
movl    $format, 0(%esp)
call    _scanf

// Load into register and increment
movl    8(%esp), %eax
incl    %eax

// Call to printf to write out EAX
movl    %eax, 4(%esp)
movl    $format, 0(%esp)
call    _printf

// Restore stack pointer and return
addl    $12, %esp
ret
