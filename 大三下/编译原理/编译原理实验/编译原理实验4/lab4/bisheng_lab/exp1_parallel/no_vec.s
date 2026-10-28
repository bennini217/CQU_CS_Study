	.text
	.file	"vec_add.c"
	.globl	main                            // -- Begin function main
	.p2align	2
	.type	main,@function
main:                                   // @main
	.cfi_startproc
// %bb.0:
	mov	w8, #2304                       // =0x900
	mov	w11, wzr
	mov	x12, xzr
	movk	w8, #61, lsl #16
	adrp	x9, a
	add	x9, x9, :lo12:a
	adrp	x10, b
	add	x10, x10, :lo12:b
.LBB0_1:                                // =>This Inner Loop Header: Depth=1
	lsl	x13, x12, #2
	and	w14, w12, #0xffff
	and	w15, w11, #0xffff
	add	x12, x12, #1
	add	w11, w11, #3
	cmp	x8, x12
	str	w14, [x9, x13]
	str	w15, [x10, x13]
	b.ne	.LBB0_1
// %bb.2:
	mov	w12, #9216                      // =0x2400
	mov	x11, xzr
	movk	w12, #244, lsl #16
	adrp	x13, c
	add	x13, x13, :lo12:c
.LBB0_3:                                // =>This Inner Loop Header: Depth=1
	ldr	w14, [x9, x11]
	ldr	w15, [x10, x11]
	add	w14, w15, w14
	str	w14, [x13, x11]
	add	x11, x11, #4
	cmp	x11, x12
	b.ne	.LBB0_3
// %bb.4:
	stp	x29, x30, [sp, #-16]!           // 16-byte Folded Spill
	.cfi_def_cfa_offset 16
	mov	x29, sp
	.cfi_def_cfa w29, 16
	.cfi_offset w30, -8
	.cfi_offset w29, -16
	mov	x1, xzr
	adrp	x9, c
	add	x9, x9, :lo12:c
.LBB0_5:                                // =>This Inner Loop Header: Depth=1
	ldrsw	x10, [x9], #4
	subs	x8, x8, #1
	add	x1, x1, x10
	b.ne	.LBB0_5
// %bb.6:
	adrp	x0, .L.str
	add	x0, x0, :lo12:.L.str
	bl	printf
	mov	w0, wzr
	.cfi_def_cfa wsp, 16
	ldp	x29, x30, [sp], #16             // 16-byte Folded Reload
	.cfi_def_cfa_offset 0
	.cfi_restore w30
	.cfi_restore w29
	ret
.Lfunc_end0:
	.size	main, .Lfunc_end0-main
	.cfi_endproc
                                        // -- End function
	.type	a,@object                       // @a
	.local	a
	.comm	a,16000000,4
	.type	b,@object                       // @b
	.local	b
	.comm	b,16000000,4
	.type	c,@object                       // @c
	.local	c
	.comm	c,16000000,4
	.type	.L.str,@object                  // @.str
	.section	.rodata.str1.1,"aMS",@progbits,1
.L.str:
	.asciz	"%lld\n"
	.size	.L.str, 6

	.ident	"BiSheng Enterprise 4.2.0.B009 clang version 17.0.6 (958fd14d28f0)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
