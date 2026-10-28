#!/bin/sh

TOP_DIR=$(dirname $(readlink -f "$0"))
KERNEL_DIR=${TOP_DIR}/kernel/kernel/kernel
SIGN_TOOL_DIR=${TOP_DIR}/tools/signtool
OUTPUT_DIR=${TOP_DIR}/output
HEADER_VERSION="0.0.0.0.0"
SCRIPTS_DIR=${TOP_DIR}/scripts


function sign_image()
{
    image_name=$1
    tag_value=$2

    #添加256字节校验头
    python3 $SIGN_TOOL_DIR/esbc_header/esbc_header.py -raw_img $OUTPUT_DIR/$image_name -out_img $OUTPUT_DIR/$image_name -version $HEADER_VERSION -nvcnt 0 -tag $tag_value -platform hi1910Brc

    #添加8K校验头
    python3 $SIGN_TOOL_DIR/image_pack/image_pack.py -raw_img $OUTPUT_DIR/$image_name -out_img $OUTPUT_DIR/$image_name -platform hi1910Brc -version $HEADER_VERSION
    echo "sign ${OUTPUT_DIR}/${image_name} success!"
}


sign_image Image uimage

