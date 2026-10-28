	.text
	.file	"toy_opt.c"
	.globl	bar                             // -- Begin function bar
	.p2align	2
	.type	bar,@function
bar:                                    // @bar
	.cfi_startproc
// %bb.0:
	sub	sp, sp, #16
	.cfi_def_cfa_offset 16
	mov	w8, #100                        // =0x64
	str	w8, [sp, #8]
	mov	w8, #20                         // =0x14
	str	w8, [sp, #4]
	ldr	w8, [sp, #8]
	subs	w8, w8, #0
	cset	w8, le
	tbnz	w8, #0, .LBB0_2
	b	.LBB0_1
.LBB0_1:
	ldr	w8, [sp, #8]
	add	w8, w8, #1
	str	w8, [sp, #12]
	b	.LBB0_3
.LBB0_2:
	ldr	w8, [sp, #8]
	subs	w8, w8, #1
	str	w8, [sp, #12]
	b	.LBB0_3
.LBB0_3:
	ldr	w0, [sp, #12]
	add	sp, sp, #16
	.cfi_def_cfa_offset 0
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
	mov	w8, wzr
	str	w8, [sp, #4]                    // 4-byte Folded Spill
	stur	wzr, [x29, #-4]
	bl	bar
	str	w0, [sp, #8]
	ldr	w1, [sp, #8]
	adrp	x0, .L.str
	add	x0, x0, :lo12:.L.str
	bl	printf
	ldr	w0, [sp, #4]                    // 4-byte Folded Reload
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
	.addrsig_sym bar
	.addrsig_sym printf
