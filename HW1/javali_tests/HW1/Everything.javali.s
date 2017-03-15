  # Emitting class Main {...}
    # Emitting void main(...) {...}
    .cstring
    printfinteger: .asciz "%d"
    scanfinteger: .asciz "%d"
    printfnewline: .asciz "\n"
    .data
      # Emitting (...)
        # Emitting int r1
        varr1: .long 0
        # Emitting int r2
        varr2: .long 0
        # Emitting int r3
        varr3: .long 0
        # Emitting int r4
        varr4: .long 0
        # Emitting int i0
        vari0: .long 0
        # Emitting int i1
        vari1: .long 0
        # Emitting int i2
        vari2: .long 0
        # Emitting int i3
        vari3: .long 0
        # Emitting int i4
        vari4: .long 0
    .text
    .global _main
    _main:
    .align 16
      # Emitting (...)
        # Emitting i0 = 5
          # Emitting 5
          movl $5, %edi
        leal vari0, %esi
        movl %edi, (%esi)
        # Emitting i1 = read()
          # Emitting read()
          subl $4, %esp
          movl %eax, 0(%esp)
          subl $8, %esp
          leal 8(%esp), %edi
          movl %edi, 4(%esp)
          movl $scanfinteger, 0(%esp)
          call _scanf
          movl 8(%esp), %edi
          addl $8, %esp
          movl 0(%esp), %eax
          addl $4, %esp
        leal vari1, %esi
        movl %edi, (%esi)
        # Emitting i2 = read()
          # Emitting read()
          subl $4, %esp
          movl %eax, 0(%esp)
          subl $8, %esp
          leal 8(%esp), %edi
          movl %edi, 4(%esp)
          movl $scanfinteger, 0(%esp)
          call _scanf
          movl 8(%esp), %edi
          addl $8, %esp
          movl 0(%esp), %eax
          addl $4, %esp
        leal vari2, %esi
        movl %edi, (%esi)
        # Emitting i3 = -(7)
          # Emitting -(7)
            # Emitting 7
            movl $7, %edi
          negl %edi
        leal vari3, %esi
        movl %edi, (%esi)
        # Emitting r1 = (i2 / 2)
          # Emitting (i2 / 2)
            # Emitting 2
            movl $2, %edi
            # Emitting i2
            movl vari2, %esi
          subl $4, %esp
          movl %edx, 0(%esp)
          subl $4, %esp
          movl %eax, 0(%esp)
          movl $0, %edx
          movl %esi, %eax
          idivl %edi
          movl %eax, %esi
          movl 0(%esp), %eax
          addl $4, %esp
          movl 0(%esp), %edx
          addl $4, %esp
        leal varr1, %edi
        movl %esi, (%edi)
        # Emitting write(-((r1 % 2)))
        subl $4, %esp
        movl %eax, 0(%esp)
          # Emitting -((r1 % 2))
            # Emitting (r1 % 2)
              # Emitting 2
              movl $2, %esi
              # Emitting r1
              movl varr1, %edi
            subl $4, %esp
            movl %edx, 0(%esp)
            subl $4, %esp
            movl %eax, 0(%esp)
            movl $0, %edx
            movl %edi, %eax
            idivl %esi
            movl %edx, %edi
            movl 0(%esp), %eax
            addl $4, %esp
            movl 0(%esp), %edx
            addl $4, %esp
          negl %edi
        subl $8, %esp
        movl %edi, 4(%esp)
        movl $printfinteger, 0(%esp)
        call _printf
        addl $8, %esp
        movl 0(%esp), %eax
        addl $4, %esp
        # Emitting writeln()
        subl $4, %esp
        movl %eax, 0(%esp)
        subl $16, %esp
        movl $printfnewline, 0(%esp)
        call _printf
        addl $16, %esp
        movl 0(%esp), %eax
        addl $4, %esp
        # Emitting i4 = read()
          # Emitting read()
          subl $4, %esp
          movl %eax, 0(%esp)
          subl $8, %esp
          leal 8(%esp), %edi
          movl %edi, 4(%esp)
          movl $scanfinteger, 0(%esp)
          call _scanf
          movl 8(%esp), %edi
          addl $8, %esp
          movl 0(%esp), %eax
          addl $4, %esp
        leal vari4, %esi
        movl %edi, (%esi)
        # Emitting r2 = (r1 / i3)
          # Emitting (r1 / i3)
            # Emitting i3
            movl vari3, %edi
            # Emitting r1
            movl varr1, %esi
          subl $4, %esp
          movl %edx, 0(%esp)
          subl $4, %esp
          movl %eax, 0(%esp)
          movl $0, %edx
          movl %esi, %eax
          idivl %edi
          movl %eax, %esi
          movl 0(%esp), %eax
          addl $4, %esp
          movl 0(%esp), %edx
          addl $4, %esp
        leal varr2, %edi
        movl %esi, (%edi)
        # Emitting write((r2 * 12))
        subl $4, %esp
        movl %eax, 0(%esp)
          # Emitting (r2 * 12)
            # Emitting 12
            movl $12, %esi
            # Emitting r2
            movl varr2, %edi
          imull %esi, %edi
        subl $8, %esp
        movl %edi, 4(%esp)
        movl $printfinteger, 0(%esp)
        call _printf
        addl $8, %esp
        movl 0(%esp), %eax
        addl $4, %esp
        # Emitting write(i4)
        subl $4, %esp
        movl %eax, 0(%esp)
          # Emitting i4
          movl vari4, %edi
        subl $8, %esp
        movl %edi, 4(%esp)
        movl $printfinteger, 0(%esp)
        call _printf
        addl $8, %esp
        movl 0(%esp), %eax
        addl $4, %esp
        # Emitting writeln()
        subl $4, %esp
        movl %eax, 0(%esp)
        subl $16, %esp
        movl $printfnewline, 0(%esp)
        call _printf
        addl $16, %esp
        movl 0(%esp), %eax
        addl $4, %esp
        # Emitting r3 = (1 - (3 % (37 / ((5 * i3) + 2))))
          # Emitting (1 - (3 % (37 / ((5 * i3) + 2))))
            # Emitting (3 % (37 / ((5 * i3) + 2)))
              # Emitting (37 / ((5 * i3) + 2))
                # Emitting ((5 * i3) + 2)
                  # Emitting (5 * i3)
                    # Emitting i3
                    movl vari3, %edi
                    # Emitting 5
                    movl $5, %esi
                  imull %edi, %esi
                  # Emitting 2
                  movl $2, %edi
                addl %edi, %esi
                # Emitting 37
                movl $37, %edi
              subl $4, %esp
              movl %edx, 0(%esp)
              subl $4, %esp
              movl %eax, 0(%esp)
              movl $0, %edx
              movl %edi, %eax
              idivl %esi
              movl %eax, %edi
              movl 0(%esp), %eax
              addl $4, %esp
              movl 0(%esp), %edx
              addl $4, %esp
              # Emitting 3
              movl $3, %esi
            subl $4, %esp
            movl %edx, 0(%esp)
            subl $4, %esp
            movl %eax, 0(%esp)
            movl $0, %edx
            movl %esi, %eax
            idivl %edi
            movl %edx, %esi
            movl 0(%esp), %eax
            addl $4, %esp
            movl 0(%esp), %edx
            addl $4, %esp
            # Emitting 1
            movl $1, %edi
          subl %esi, %edi
        leal varr3, %esi
        movl %edi, (%esi)
        # Emitting r4 = r3
          # Emitting r3
          movl varr3, %edi
        leal varr4, %esi
        movl %edi, (%esi)
        # Emitting write(((((r4 * 10) % i4) - 2) + ((2 * 5) / -(i1))))
        subl $4, %esp
        movl %eax, 0(%esp)
          # Emitting ((((r4 * 10) % i4) - 2) + ((2 * 5) / -(i1)))
            # Emitting (((r4 * 10) % i4) - 2)
              # Emitting ((r4 * 10) % i4)
                # Emitting (r4 * 10)
                  # Emitting 10
                  movl $10, %edi
                  # Emitting r4
                  movl varr4, %esi
                imull %edi, %esi
                # Emitting i4
                movl vari4, %edi
              subl $4, %esp
              movl %edx, 0(%esp)
              subl $4, %esp
              movl %eax, 0(%esp)
              movl $0, %edx
              movl %esi, %eax
              idivl %edi
              movl %edx, %esi
              movl 0(%esp), %eax
              addl $4, %esp
              movl 0(%esp), %edx
              addl $4, %esp
              # Emitting 2
              movl $2, %edi
            subl %edi, %esi
            # Emitting ((2 * 5) / -(i1))
              # Emitting (2 * 5)
                # Emitting 5
                movl $5, %edi
                # Emitting 2
                movl $2, %edx
              imull %edi, %edx
              # Emitting -(i1)
                # Emitting i1
                movl vari1, %edi
              negl %edi
            xchgl %edx, %ecx
            subl $4, %esp
            movl %edx, 0(%esp)
            subl $4, %esp
            movl %eax, 0(%esp)
            movl $0, %edx
            movl %ecx, %eax
            idivl %edi
            movl %eax, %ecx
            movl 0(%esp), %eax
            addl $4, %esp
            movl 0(%esp), %edx
            addl $4, %esp
          addl %ecx, %esi
        subl $8, %esp
        movl %esi, 4(%esp)
        movl $printfinteger, 0(%esp)
        call _printf
        addl $8, %esp
        movl 0(%esp), %eax
        addl $4, %esp
        # Emitting writeln()
        subl $4, %esp
        movl %eax, 0(%esp)
        subl $16, %esp
        movl $printfnewline, 0(%esp)
        call _printf
        addl $16, %esp
        movl 0(%esp), %eax
        addl $4, %esp
    movl $0, %eax
    ret
