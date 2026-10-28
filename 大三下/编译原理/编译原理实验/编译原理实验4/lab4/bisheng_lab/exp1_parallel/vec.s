	.text
	.file	"vec_add.c"
	.section	.rodata.cst16,"aM",@progbits,16
	.p2align	4, 0x0                          // -- Begin function main
.LCPI0_0:
	.word	0                               // 0x0
	.word	1                               // 0x1
	.word	2                               // 0x2
	.word	3                               // 0x3
	.text
	.globl	main
	.p2align	2
	.type	main,@function
main:                                   // @main
	.cfi_startproc
// %bb.0:
	stp	x29, x30, [sp, #-16]!           // 16-byte Folded Spill
	.cfi_def_cfa_offset 16
	mov	x29, sp
	.cfi_def_cfa w29, 16
	.cfi_offset w30, -8
	.cfi_offset w29, -16
	adrp	x9, .LCPI0_0
	mov	w8, #2304                       // =0x900
	movi	v0.4s, #4
	movk	w8, #61, lsl #16
	movi	v1.2d, #0x00ffff0000ffff
	adrp	x10, a+16
	add	x10, x10, :lo12:a+16
	ldr	q2, [x9, :lo12:.LCPI0_0]
	movi	v3.4s, #3
	mov	w9, #2304                       // =0x900
	movi	v4.4s, #12
	movk	w9, #61, lsl #16
	movi	v5.4s, #8
	adrp	x11, b+16
	add	x11, x11, :lo12:b+16
	mov	v6.16b, v2.16b
.LBB0_1:                                // =>This Inner Loop Header: Depth=1
	mul	v17.4s, v6.4s, v3.4s
	subs	x9, x9, #8
	add	v7.4s, v2.4s, v0.4s
	and	v16.16b, v2.16b, v1.16b
	and	v7.16b, v7.16b, v1.16b
	stp	q16, q7, [x10, #-16]
	add	x10, x10, #32
	add	v7.4s, v17.4s, v4.4s
	and	v16.16b, v17.16b, v1.16b
	and	v7.16b, v7.16b, v1.16b
	stp	q16, q7, [x11, #-16]
	add	x11, x11, #32
	add	v2.4s, v2.4s, v5.4s
	add	v6.4s, v6.4s, v5.4s
	b.ne	.LBB0_1
// %bb.2:
	mov	w10, #2304                      // =0x900
	adrp	x9, a+16
	add	x9, x9, :lo12:a+16
	movk	w10, #61, lsl #16
	adrp	x11, b+16
	add	x11, x11, :lo12:b+16
	adrp	x12, c+16
	add	x12, x12, :lo12:c+16
.LBB0_3:                                // =>This Inner Loop Header: Depth=1
	ldp	q0, q1, [x9, #-16]
	add	x9, x9, #32
	subs	x10, x10, #8
	ldp	q2, q3, [x11, #-16]
	add	x11, x11, #32
	add	v0.4s, v2.4s, v0.4s
	add	v1.4s, v3.4s, v1.4s
	stp	q0, q1, [x12, #-16]
	add	x12, x12, #32
	b.ne	.LBB0_3
// %bb.4:
	movi	v0.2d, #0000000000000000
	adrp	x9, c+16
	add	x9, x9, :lo12:c+16
	movi	v1.2d, #0000000000000000
	movi	v2.2d, #0000000000000000
	movi	v3.2d, #0000000000000000
.LBB0_5:                                // =>This Inner Loop Header: Depth=1
	ldp	q4, q5, [x9, #-16]
	add	x9, x9, #32
	subs	x8, x8, #8
	saddw2	v1.2d, v1.2d, v4.4s
	saddw	v0.2d, v0.2d, v4.2s
	saddw2	v3.2d, v3.2d, v5.4s
	saddw	v2.2d, v2.2d, v5.2s
	b.ne	.LBB0_5
// %bb.6:
	add	v0.2d, v2.2d, v0.2d
	adrp	x0, .L.str
	add	x0, x0, :lo12:.L.str
	add	v1.2d, v3.2d, v1.2d
	add	v0.2d, v0.2d, v1.2d
	addp	d0, v0.2d
	fmov	x1, d0
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
