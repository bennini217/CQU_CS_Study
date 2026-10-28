	.text
	.file	"loop_fusion_test.c"
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
	sub	sp, sp, #96
	.cfi_def_cfa_offset 96
	stp	x29, x30, [sp, #32]             // 16-byte Folded Spill
	stp	x24, x23, [sp, #48]             // 16-byte Folded Spill
	stp	x22, x21, [sp, #64]             // 16-byte Folded Spill
	stp	x20, x19, [sp, #80]             // 16-byte Folded Spill
	add	x29, sp, #32
	.cfi_def_cfa w29, 64
	.cfi_offset w19, -8
	.cfi_offset w20, -16
	.cfi_offset w21, -24
	.cfi_offset w22, -32
	.cfi_offset w23, -40
	.cfi_offset w24, -48
	.cfi_offset w30, -56
	.cfi_offset w29, -64
	mov	w0, #13824                      // =0x3600
	movk	w0, #366, lsl #16
	bl	malloc
	adrp	x21, a
	mov	x19, x0
	str	x0, [x21, :lo12:a]
	mov	w0, #13824                      // =0x3600
	movk	w0, #366, lsl #16
	bl	malloc
	adrp	x22, b
	mov	x20, x0
	str	x0, [x22, :lo12:b]
	mov	w0, #13824                      // =0x3600
	movk	w0, #366, lsl #16
	bl	malloc
	adrp	x23, c
	mov	w8, #1                          // =0x1
	str	x0, [x23, :lo12:c]
	cbz	x19, .LBB0_20
// %bb.1:
	cbz	x20, .LBB0_20
// %bb.2:
	cbz	x0, .LBB0_20
// %bb.3:
	mov	w2, #13824                      // =0x3600
	mov	w1, wzr
	movk	w2, #366, lsl #16
	mov	w24, #36224                     // =0x8d80
	movk	w24, #91, lsl #16
	bl	memset
	adrp	x10, .LCPI0_0
	add	x8, x19, #16
	movi	v0.4s, #4
	add	x9, x20, #16
	movi	v1.4s, #3, msl #8
	ldr	q2, [x10, :lo12:.LCPI0_0]
	mov	w10, #36224                     // =0x8d80
	movi	v3.4s, #3
	movk	w10, #91, lsl #16
	movi	v4.4s, #12
	movi	v5.4s, #8
	mov	v6.16b, v2.16b
.LBB0_4:                                // =>This Inner Loop Header: Depth=1
	mul	v17.4s, v6.4s, v3.4s
	subs	x10, x10, #8
	add	v7.4s, v2.4s, v0.4s
	and	v16.16b, v2.16b, v1.16b
	and	v7.16b, v7.16b, v1.16b
	stp	q16, q7, [x8, #-16]
	add	x8, x8, #32
	add	v7.4s, v17.4s, v4.4s
	and	v16.16b, v17.16b, v1.16b
	and	v7.16b, v7.16b, v1.16b
	stp	q16, q7, [x9, #-16]
	add	x9, x9, #32
	add	v2.4s, v2.4s, v5.4s
	add	v6.4s, v6.4s, v5.4s
	b.ne	.LBB0_4
// %bb.5:
	add	x1, sp, #16
	mov	w0, #1                          // =0x1
	bl	clock_gettime
	ldr	x8, [x21, :lo12:a]
	mov	w14, #36224                     // =0x8d80
	ldr	x10, [x22, :lo12:b]
	movk	w14, #91, lsl #16
	ldr	x9, [x23, :lo12:c]
	add	x11, x8, #16
	add	x12, x10, #16
	add	x13, x9, #16
.LBB0_6:                                // =>This Inner Loop Header: Depth=1
	ldp	q0, q1, [x11, #-16]
	add	x11, x11, #32
	subs	x14, x14, #8
	ldp	q2, q3, [x12, #-16]
	add	x12, x12, #32
	add	v0.4s, v2.4s, v0.4s
	add	v1.4s, v3.4s, v1.4s
	stp	q0, q1, [x13, #-16]
	add	x13, x13, #32
	b.ne	.LBB0_6
// %bb.7:
	mov	w13, #36224                     // =0x8d80
	add	x11, x8, #16
	add	x12, x9, #16
	movk	w13, #91, lsl #16
.LBB0_8:                                // =>This Inner Loop Header: Depth=1
	ldp	q0, q1, [x11, #-16]
	add	x11, x11, #32
	subs	x13, x13, #8
	add	v0.4s, v0.4s, v0.4s
	ldp	q2, q3, [x12, #-16]
	add	v1.4s, v1.4s, v1.4s
	add	v0.4s, v2.4s, v0.4s
	add	v1.4s, v3.4s, v1.4s
	stp	q0, q1, [x12, #-16]
	add	x12, x12, #32
	b.ne	.LBB0_8
// %bb.9:
	mov	w14, #36224                     // =0x8d80
	add	x11, x8, #16
	add	x12, x10, #16
	add	x13, x9, #16
	movk	w14, #91, lsl #16
.LBB0_10:                               // =>This Inner Loop Header: Depth=1
	ldp	q0, q1, [x11, #-16]
	add	x11, x11, #32
	subs	x14, x14, #8
	ldp	q2, q3, [x12, #-16]
	add	x12, x12, #32
	add	v0.4s, v2.4s, v0.4s
	add	v1.4s, v3.4s, v1.4s
	stp	q0, q1, [x13, #-16]
	add	x13, x13, #32
	b.ne	.LBB0_10
// %bb.11:
	mov	w13, #36224                     // =0x8d80
	add	x11, x8, #16
	add	x12, x9, #16
	movk	w13, #91, lsl #16
.LBB0_12:                               // =>This Inner Loop Header: Depth=1
	ldp	q0, q1, [x11, #-16]
	add	x11, x11, #32
	subs	x13, x13, #8
	add	v0.4s, v0.4s, v0.4s
	ldp	q2, q3, [x12, #-16]
	add	v1.4s, v1.4s, v1.4s
	add	v0.4s, v2.4s, v0.4s
	add	v1.4s, v3.4s, v1.4s
	stp	q0, q1, [x12, #-16]
	add	x12, x12, #32
	b.ne	.LBB0_12
// %bb.13:
	mov	w13, #36224                     // =0x8d80
	add	x11, x8, #16
	add	x10, x10, #16
	add	x12, x9, #16
	movk	w13, #91, lsl #16
.LBB0_14:                               // =>This Inner Loop Header: Depth=1
	ldp	q0, q1, [x11, #-16]
	add	x11, x11, #32
	subs	x13, x13, #8
	ldp	q2, q3, [x10, #-16]
	add	x10, x10, #32
	add	v0.4s, v2.4s, v0.4s
	add	v1.4s, v3.4s, v1.4s
	stp	q0, q1, [x12, #-16]
	add	x12, x12, #32
	b.ne	.LBB0_14
// %bb.15:
	mov	w10, #36224                     // =0x8d80
	add	x8, x8, #16
	add	x9, x9, #16
	movk	w10, #91, lsl #16
.LBB0_16:                               // =>This Inner Loop Header: Depth=1
	ldp	q0, q1, [x8, #-16]
	add	x8, x8, #32
	subs	x10, x10, #8
	add	v0.4s, v0.4s, v0.4s
	ldp	q2, q3, [x9, #-16]
	add	v1.4s, v1.4s, v1.4s
	add	v0.4s, v2.4s, v0.4s
	add	v1.4s, v3.4s, v1.4s
	stp	q0, q1, [x9, #-16]
	add	x9, x9, #32
	b.ne	.LBB0_16
// %bb.17:
	mov	x1, sp
	mov	w0, #1                          // =0x1
	bl	clock_gettime
	ldp	x10, x8, [sp, #8]
	movi	v0.2d, #0000000000000000
	ldr	x9, [sp]
	ldr	x11, [sp, #24]
	ldr	x12, [x23, :lo12:c]
	movi	v1.2d, #0000000000000000
	sub	x8, x9, x8
	movi	v3.2d, #0000000000000000
	sub	x9, x10, x11
	movi	v2.2d, #0000000000000000
	add	x10, x12, #16
.LBB0_18:                               // =>This Inner Loop Header: Depth=1
	ldp	q4, q5, [x10, #-16]
	add	x10, x10, #32
	subs	x24, x24, #8
	saddw2	v1.2d, v1.2d, v4.4s
	saddw	v0.2d, v0.2d, v4.2s
	saddw2	v2.2d, v2.2d, v5.4s
	saddw	v3.2d, v3.2d, v5.2s
	b.ne	.LBB0_18
// %bb.19:
	add	v0.2d, v3.2d, v0.2d
	mov	w10, #51712                     // =0xca00
	add	v1.2d, v2.2d, v1.2d
	movk	w10, #15258, lsl #16
	adrp	x0, .L.str
	add	x0, x0, :lo12:.L.str
	add	v0.2d, v0.2d, v1.2d
	madd	x2, x8, x10, x9
	mov	w1, wzr
	addp	d0, v0.2d
	fmov	x3, d0
	bl	printf
	ldr	x0, [x21, :lo12:a]
	bl	free
	ldr	x0, [x22, :lo12:b]
	bl	free
	ldr	x0, [x23, :lo12:c]
	bl	free
	mov	w8, wzr
.LBB0_20:
	mov	w0, w8
	.cfi_def_cfa wsp, 96
	ldp	x20, x19, [sp, #80]             // 16-byte Folded Reload
	ldp	x22, x21, [sp, #64]             // 16-byte Folded Reload
	ldp	x24, x23, [sp, #48]             // 16-byte Folded Reload
	ldp	x29, x30, [sp, #32]             // 16-byte Folded Reload
	add	sp, sp, #96
	.cfi_def_cfa_offset 0
	.cfi_restore w19
	.cfi_restore w20
	.cfi_restore w21
	.cfi_restore w22
	.cfi_restore w23
	.cfi_restore w24
	.cfi_restore w30
	.cfi_restore w29
	ret
.Lfunc_end0:
	.size	main, .Lfunc_end0-main
	.cfi_endproc
                                        // -- End function
	.type	a,@object                       // @a
	.local	a
	.comm	a,8,8
	.type	b,@object                       // @b
	.local	b
	.comm	b,8,8
	.type	c,@object                       // @c
	.local	c
	.comm	c,8,8
	.type	.L.str,@object                  // @.str
	.section	.rodata.str1.1,"aMS",@progbits,1
.L.str:
	.asciz	"FUSED=%d ns %lld checksum %lld\n"
	.size	.L.str, 32

	.ident	"BiSheng Enterprise 4.2.0.B009 clang version 17.0.6 (958fd14d28f0)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
