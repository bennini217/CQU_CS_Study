#!/usr/bin/env python
#-*- coding: utf-8 -*-
#----------------------------------------------------------------------------
# Purpose:
# Copyright Huawei Technologies Co., Ltd. 2010-2025. All rights reserved.
#----------------------------------------------------------------------------
import struct
import sys
from tools import *

# typedef struct {
#     uint32_t uwLPreamble;
#     uint32_t uwLHeadLen;
#     uint32_t uwLUserLen;
#     uint8_t  ucLUserDefineData[32];
#     uint8_t  ucLHash[HASH_SIZE];
#     uint32_t uwSubKeyCertOffset1;
#     uint32_t uwSubKeyCertOffset2;
#     uint32_t uwL2SignAlg;
#     uint32_t uwRootPubKLen;
#     uint8_t  ucRootPubKE[RSA_ROOTKEY_WIDTH_BYTES];
#     uint8_t  ucRootPubK[RSA_ROOTKEY_WIDTH_BYTES];
#     uint32_t uwLCodeOffset;
#     uint32_t uwLCodeLen;
#     uint32_t uwLSign1Offset;
#     uint32_t uwLSign2Offset;
#     uint32_t uwLHeadMagic;
#     uint32_t uwRootPubPadAlg;
#     uint32_t uwRootPubPadSlen;
# } SE_IMAGE_HEAD;

def __construct_header(N_buf, E_buf, hash_buf, code_len, pss=False):
    s = struct.Struct('III32s32sIIII512s512sIIIIIII')
    uwLPreamble = 0x55AA55AA
    uwLHeadLen = 0x478
    uwLUserLen = 0
    if sys.version > '3':
        ucLUserDefineData = int(0).to_bytes(32, 'big')
    else:
        ucLUserDefineData = to_bytes(0, 32)
    ucLHash = hash_buf
    uwSubKeyCertOffset1 = 0x1000
    uwSubKeyCertOffset2 = 0x2000
    uwL2SignAlg = 0
    uwRootPubKLen = 512
    ucRootPubKE = E_buf
    ucRootPubK = N_buf
    uwLCodeOffset = 0x10000
    uwLCodeLen = code_len
    uwLSign1Offset = 0x3000
    uwLSign2Offset = 0x4000
    uwLHeadMagic = 0x33CC33CC
    uwRootPubPadAlg = 0x1 if pss else 0
    uwRootPubPadSlen = 0x20 if pss else 0
    pack_list = (uwLPreamble, uwLHeadLen, uwLUserLen, ucLUserDefineData, ucLHash, uwSubKeyCertOffset1, uwSubKeyCertOffset2, uwL2SignAlg,
                 uwRootPubKLen, ucRootPubKE, ucRootPubK, uwLCodeOffset, uwLCodeLen, uwLSign1Offset, uwLSign2Offset, uwLHeadMagic, uwRootPubPadAlg, uwRootPubPadSlen)
    header = s.pack(*pack_list)
    # print(binascii.hexlify(header))
    return header

def __get_filelen(f):
    f.seek(0, 2)
    length = f.tell()
    f.seek(0)
    return length

def __write_header(out, header):
    offset = 0
    out.seek(offset)
    out.write(header)


def __write_2nd_header(out, hash_buf, code_len, suffix=False ):
    offset_preamble = 0x5000
    offset_hash = (0x5000 + 0x2C)
    offset_code_len = (0x5000 + 0x460)
    offset_head_magic = (0x5000 + 0x46C)
    
    preamble = struct.pack('<I', 0x55AA55AA)
    head_magic = struct.pack('<I', 0x33CC33CC)
    code_len_bin = struct.pack('<I', code_len)

    out.seek(offset_preamble)
    out.write(preamble)
    out.seek(offset_hash)
    out.write(hash_buf)
    out.seek(offset_code_len)
    out.write(code_len_bin)
    out.seek(offset_head_magic)
    out.write(head_magic)


def __write_cert(out, cert, pss=False):
    offset = 0x1000
    uwSubPadAlg = 0x1 if pss else 0
    uwSubPadSlen = 0x20 if pss else 0
    s = struct.Struct('II')
    subpad_pack_list = (uwSubPadAlg, uwSubPadSlen)
    subPad = s.pack(*subpad_pack_list)
    with open(cert, 'rb') as c_f:
        cert_buf = c_f.read()
        out.seek(offset)
        out.write(cert_buf)
        out.write(subPad)
        out.seek(offset + 0x1000)
        out.write(cert_buf)
        out.write(subPad)


def __write_signature(out, sig):
    offset = 0x3000
    with open(sig, 'rb') as s_f:
        sig_buf = s_f.read()
        out.seek(offset)
        out.write(sig_buf)
        out.seek(offset + 0x1000)
        out.write(sig_buf)


def __write_raw_img(out, raw):
    offset = 0x10000
    out.seek(offset)
    with open(raw, 'rb') as raw_file:
        for byte_block in iter(lambda: raw_file.read(4096), b""):
            out.write(byte_block)


def __add_in_tail(offset, inFile, outFile, header, length):
    outFile.seek(offset, 1)
    outFile.write(header)
    outFile.write(inFile.read())
    outFile.seek(-(length + 16), 1)


def __construct_cms_header(tag, length):
    s = struct.Struct('12sI')
    if (len(tag) > 11):
        raise RuntimeError('name too long')
    value = (tag.encode(), length)
    header = s.pack(*value)
    # print(binascii.hexlify(header))
    return header


def __write_cms(out, cms, aligned_bytes):
    with open(cms, 'rb') as cf:
        length = __get_filelen(cf)
        header = __construct_cms_header('cms', length)
        __add_in_tail(aligned_bytes, cf, out, header, length)


def __write_ini(out, ini, pss=False):
    with open(ini, 'rb') as ifile:
        length = __get_filelen(ifile)
        header = __construct_cms_header('ini', length)
        cms_size = 0x4000 if pss else 0x2000
        __add_in_tail(cms_size, ifile, out, header, length)


def __write_crl(out, crl):
    with open(crl, 'rb') as af:
        length = __get_filelen(af)
        header = __construct_cms_header('crl', length)
        __add_in_tail(2 * 1024, af, out, header, length)


def __construct_version(ver):
    s = struct.Struct("4I")
    value = list(map(int, ver.split('.'), [16] * 5))
    value[3] = (value[3] << 16) | (value[4] & 0xFFFF)
    pvalue = (value[0], value[1], value[2], value[3])
    return s.pack(*pvalue)


def __write_version(out, ver, suffix=False):
    if ver == ' ':
        ver = '1.0.0.0.0'
    version = __construct_version(ver)
    # print(binascii.hexlify(version))
    offset = 0x480
    out.seek(offset)
    out.write(version)


def __add_magic_number_and_file_size(out, cms_flag, suffix=False):
    if cms_flag and suffix:
        raise RuntimeError("Invalid Param: --addcms and -S can't input in the same time.")

    out.seek(0, 2)
    fileSize = out.tell()
    s = struct.Struct('QI')
    if cms_flag:
        value = (0xABCD1234AA55AA55, fileSize)
    else:
        value = (0x0, fileSize)
    stream = s.pack(*value)
    # print(binascii.hexlify(stream))
    offset = 0x490
    out.seek(offset, 0)
    out.write(stream)


# platform_api as follow

def write_header_huawei(args, out, hash_buf, code_len):
    if args.addsoc:
        from Crypto.PublicKey import RSA
        with open(args.root, 'rb') as r_f:
            pk = RSA.importKey(r_f.read())
        if sys.version > '3':
            N_buf, E_buf = pk.n.to_bytes(512, 'big'), pk.e.to_bytes(512, 'big')
        else:
            N_buf, E_buf = to_bytes(pk.n, 512), to_bytes(pk.e, 512)
        header = __construct_header(N_buf, E_buf, hash_buf, code_len, pss=args.pss)
        __write_cert(out, args.cert, pss=args.pss)
        __write_signature(out, args.sig)
    else:
        if sys.version > '3':
            N_buf, E_buf = int(0).to_bytes(512, 'big'), int(0).to_bytes(512, 'big')
        else:
            N_buf, E_buf = to_bytes(0, 512), to_bytes(0, 512)
        header = __construct_header(N_buf, E_buf, hash_buf, code_len, pss=args.pss)

    __write_header(out, header)


def write_header_customer(args, out, hash_buf, code_len):
    __write_2nd_header(out, hash_buf, code_len, args.S)


def write_image(args, out):
    __write_raw_img(out, args.raw)


def write_cms(args, out, code_len):
    if args.addcms:
        __write_cms(out, args.cms, 32 - code_len % 16)
        __write_ini(out, args.ini, pss=args.pss)
        __write_crl(out, args.crl)

# list[0]: hash_buf
# list[1]: code_len

def write_extern(args, out, list):
    __write_version(out, args.ver, args.S)
    __add_magic_number_and_file_size(out, args.addcms)
    write_header_customer(args, out, list[0], list[1])

