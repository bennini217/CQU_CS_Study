	.text
	.file	"toy_opt.c"
	.globl	bar                             // -- Begin function bar
	.p2align	2
	.type	bar,@function
bar:                                    // @bar
	.cfi_startproc
// %bb.0:
	mov	w0, #101                        // =0x65
	ret
.Lfunc_end0:
	.size	bar, .Lfunc_end0-bar
	.cfi_endproc
                                        // -- End function
	.globl	main                            // -- Begin function main
	.p2align	2
	.type	main,@function
main:                                   // @main
	.cfi_startproc
// %bb.0:
	sub	sp, sp, #32
	.cfi_def_cfa_offset 32
	stp	x29, x30, [sp, #16]             // 16-byte Folded Spill
	add	x29, sp, #16
	.cfi_def_cfa w29, 16
	.cfi_offset w30, -8
	.cfi_offset w29, -16
	mov	w8, #101                        // =0x65
	adrp	x0, .L.str
	add	x0, x0, :lo12:.L.str
	stur	w8, [x29, #-4]
	ldur	w1, [x29, #-4]
	bl	printf
	mov	w0, wzr
	.cfi_def_cfa wsp, 32
	ldp	x29, x30, [sp, #16]             // 16-byte Folded Reload
	add	sp, sp, #32
	.cfi_def_cfa_offset 0
	.cfi_restore w30
	.cfi_restore w29
	ret
.Lfunc_end1:
	.size	main, .Lfunc_end1-main
	.cfi_endproc
                                        // -- End function
	.type	.L.str,@object                  // @.str
	.section	.rodata.str1.1,"aMS",@progbits,1
.L.str:
	.asciz	"%d\n"
	.size	.L.str, 4

	.ident	"BiSheng Enterprise 4.2.0.B009 clang version 17.0.6 (958fd14d28f0)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
