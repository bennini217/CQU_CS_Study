#!/bin/bash

output_img="kunpengpro.img"

src="$(dirname "$(realpath "${BASH_SOURCE[0]}")")"
fw_dir="${src}/fw"

function cleanup {
    # 解除 loop 设备
    if [[ -n "$loop_device" ]]; then
        kpartx -d "$loop_device"
        losetup -d "$loop_device"
    fi

    # 要删除的文件列表
    files=(
        "${src}/boot_image_info"
        "${src}/parttion_head_info"
        "${fw_dir}/Image"
    )

    # 删除临时文件
    for file in "${files[@]}"; do
        [ -f "$file" ] && rm -f "$file"
    done
}

trap cleanup EXIT

function mk_tf_img() {
    local input_img="$1"
    local host_arch=$(uname -m)
    local image_size=70

    # 复制镜像文件
    cp "${input_img}" "${fw_dir}/Image"

    # 创建一个空的 .img 文件
    dd if=/dev/zero of="${output_img}" bs=1M count="${image_size}" > /dev/null 2>&1

    # 通过 losetup 将 .img 文件关联到 loop 设备
    if [ -f /.dockerenv ]; then
        echo "Running inside a Docker container"
        devfs=`mount | grep "/dev " | cut -d' ' -f1`
        if [ "$devfs" == "tmpfs" ]; then
            mount -t devtmpfs nodev /dev
        fi
    fi
    loop_device=$(losetup -f)
    losetup "${loop_device}" "${output_img}" || { echo "Error: Failed to set up loop device."; exit 1; }

    # 创建分区
    parted "${loop_device}" mklabel msdos > /dev/null 2>&1 || { echo "Error: Failed to create partition label."; exit 1; }
    parted "${loop_device}" mkpart primary ext4 1MiB "$((image_size - 1))MiB" > /dev/null 2>&1 || { echo "Error: Failed to create partition."; exit 1; }

    # 格式化分区
    mkfs.ext4 "${loop_device}p1" > /dev/null 2>&1 || { echo "Error: Failed to format partition."; exit 1; }

    # 挂载分区
    kpartx -av "${output_img}" > /dev/null 2>&1 || { echo "Error: Failed to mount partition."; exit 1; }

    # 获取 UUID
    uuid=$(blkid "${loop_device}p1" -s UUID -o value) || { echo "Error: Failed to get UUID."; exit 1; }

    # 检查主机架构
    if [[ ${host_arch} == "x86_64" ]]; then
        if [[ ! -f /lib64/ld-linux-aarch64.so.1 ]]; then
            cp "${src}/lib/ld-linux-aarch64.so.1" /lib64/
        fi
        export LD_LIBRARY_PATH="${LD_LIBRARY_PATH}:${src}/lib"
    else
        echo -e "\e[31mError: Only the x86_64 platform is supported.\e[0m"
        exit 1
    fi

    # 运行 emmc-head
    ${src}/bin/emmc-head "${fw_dir}" "${uuid}" "${uuid}" > /dev/null 2>&1 || { echo -e "\e[31mError: The error occurred while running emmc-head.\e[0m"; exit 1; }

    # 将数据写入 .img 文件
    local HEAD_OFFSET=2048
    local HEAD_BAK_OFFSET=2176
    local BOOTIMGDIR_OFFSET=2304

    dd if="${src}/parttion_head_info" of="${loop_device}" seek="${HEAD_OFFSET}" count=2 bs=512     > /dev/null 2>&1 || { echo -e "\e[31mError: An error occurred while writing partition head info.\e[0m"; exit 1; }
    dd if="${src}/parttion_head_info" of="${loop_device}" seek="${HEAD_BAK_OFFSET}" count=2 bs=512 > /dev/null 2>&1 || { echo -e "\e[31mError: An error occurred while writing partition head backup info.\e[0m"; exit 1; }
    dd if="${src}/boot_image_info" of="${loop_device}" seek="${BOOTIMGDIR_OFFSET}" count=8 bs=512  > /dev/null 2>&1 || { echo -e "\e[31mError: An error occurred while writing boot_image_info.\e[0m"; exit 1; }

    local IMAGE_OFFSET=0
    local IMAGE_SIZE=61440
    local DTB_OFFSET=81920
    local DTB_SIZE=4096
    local TEE_OFFSET=90112
    local TEE_SIZE=4096
    local BOOTIMG_OFFSET_A=$[16777216/512]

    dd if=${fw_dir}/Image        of=${loop_device} count=${IMAGE_SIZE} seek=$[BOOTIMG_OFFSET_A+IMAGE_OFFSET] bs=512 > /dev/null 2>&1
    dd if=${fw_dir}/dt.img       of=${loop_device} count=${DTB_SIZE}   seek=$[BOOTIMG_OFFSET_A+DTB_OFFSET]   bs=512 > /dev/null 2>&1
    dd if=${fw_dir}/itrustee.img of=${loop_device} count=${TEE_SIZE}   seek=$[BOOTIMG_OFFSET_A+TEE_OFFSET]   bs=512 > /dev/null 2>&1

    # 清理工作
    rm -f "${src}/boot_image_info" "${src}/parttion_head_info" "${fw_dir}/Image"
}

# 参数解析
while getopts ":i:o:s:" opt; do
    case ${opt} in
        i)
            input_img="$OPTARG"
            ;;
        o)
            output_img="$OPTARG"
            ;;
        \?)
            echo "Invalid option: -$OPTARG" >&2
            exit 1
            ;;
        :)
            echo "Option -$OPTARG requires an argument." >&2
            exit 1
            ;;
    esac
done

# 检查输入参数是否设置
if [[ -z "$input_img" ]]; then
    #echo "Error: An input image file is required. Please use the -i option to specify the path to the input image file."
    echo "Usage: ./mk_tf_img.sh -i <input_image_path> [options]"
    echo ""
    echo "Options:"
    echo "  -i <input_image_path>    Path to the input image file (required)."
    echo "  -o <output_image_path>   Path to save the output image file (optional)."
    echo ""
    echo "Examples:"
    echo "  ./mk_tf_img.sh -i /path/to/input.img"
    echo "  ./mk_tf_img.sh -i /path/to/input.img -o /path/to/kunpeng.img"

    exit 1
fi

# 检查 input_img 是否存在
if [[ ! -f "$input_img" ]]; then
    echo "Error: Input image file '$input_img' does not exist."
    exit 1
fi

mk_tf_img "$input_img"
