#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

typedef unsigned char uint8_t;
typedef unsigned short uint16_t;
typedef unsigned int uint32_t;

#define MAX_BUFFER_SIZE 2048

/* ============================================================
 * 1. 各层结构体定义 (参考报告 Protocols.h)
 * ============================================================ */

// 数据链路层——以太网帧结构 IEEE 802.3
typedef struct {
    uint8_t dst_mac[6];   // 目标MAC地址
    uint8_t src_mac[6];   // 源MAC地址
    uint8_t type[2];      // 类型字段
    uint32_t crc;         // 帧校验序列
} ethernet_frame;

// 网络层——IP数据报
typedef struct {
    uint8_t version;          // 4 bits 版本
    uint8_t header_length;    // 4 bits 首部长度
    uint8_t type_of_service;  // 服务类型
    uint16_t total_length;    // 总长度
    uint16_t identification;  // 标识
    uint8_t flags;            // 3 bits 标志
    uint16_t fragment_offset; // 13 bits 片偏移
    uint8_t time_to_live;     // 生存时间
    uint8_t protocol;         // 协议
    uint16_t header_checksum; // 首部校验和
    uint8_t src_ip[4];        // 源IP地址
    uint8_t dst_ip[4];        // 目标IP地址
} ipv4_frame;


// 计算IP首部校验和 (16位反码求和)
uint16_t Checksum(uint8_t *buffer, int size) {
    uint32_t cksum = 0;
    while (size > 1) {
        cksum += *(uint16_t *)buffer;
        buffer += 2;
        size -= 2;
    }
    if (size) {
        cksum += *(uint8_t *)buffer;
    }
    while (cksum >> 16) {
        cksum = (cksum >> 16) + (cksum & 0xffff);
    }
    return (uint16_t)(~cksum);
}

// 计算CRC32校验和 (标准查表法或直接计算，这里用直接计算)
uint32_t crc32(uint8_t *data, int length) {
    uint32_t crc = 0xFFFFFFFF;
    for (int i = 0; i < length; i++) {
        crc ^= data[i];
        for (int j = 0; j < 8; j++) {
            if (crc & 1)
                crc = (crc >> 1) ^ 0xEDB88320;
            else
                crc >>= 1;
        }
    }
    return ~crc;
}

// 辅助打印函数：以十六进制展示内存内容
void printb(uint8_t *buffer, int len) {
    printf("负载数据段内容 (%d bytes):\n", len);
    for (int i = 0; i < len; i++) {
        printf("%02x | ", buffer[i]);
        if ((i + 1) % 16 == 0) printf("\n");
    }
    printf("\n");
}

void ip_print(ipv4_frame *ip) {
    printf("IP Layer:\n");
    printf(" - Version: %d\n", ip->version);
    printf(" - Header Length: %d\n", ip->header_length);
    printf(" - Type of Service: %d\n", ip->type_of_service);
    printf(" - Total Length: %d\n", ip->total_length);
    printf(" - Identification: %d\n", ip->identification);
    printf(" - Time to Live: %d\n", ip->time_to_live);
    printf(" - Protocol: %d\n", ip->protocol);
    printf(" - Header Checksum: %d\n", ip->header_checksum);
    printf(" - Source IP: %d.%d.%d.%d\n", ip->src_ip[0], ip->src_ip[1], ip->src_ip[2], ip->src_ip[3]);
    printf(" - Destination IP: %d.%d.%d.%d\n", ip->dst_ip[0], ip->dst_ip[1], ip->dst_ip[2], ip->dst_ip[3]);
}

void data_link_layer_print(ethernet_frame *eth) {
    printf("Data Link Layer:\n");
    printf(" - Source MAC: %02x:%02x:%02x:%02x:%02x:%02x\n", 
           eth->src_mac[0], eth->src_mac[1], eth->src_mac[2], eth->src_mac[3], eth->src_mac[4], eth->src_mac[5]);
    printf(" - Destination MAC: %02x:%02x:%02x:%02x:%02x:%02x\n", 
           eth->dst_mac[0], eth->dst_mac[1], eth->dst_mac[2], eth->dst_mac[3], eth->dst_mac[4], eth->dst_mac[5]);
    printf(" - Type: %02x%02x\n", eth->type[0], eth->type[1]);
    printf(" - CRC: %u\n", eth->crc);
}

/* ============================================================
 * 2. 封装函数实现
 * ============================================================ */

// 网络层封装函数
void ip_init(uint8_t *buffer, ipv4_frame *ip, uint8_t src_ip[4], uint8_t dst_ip[4], uint16_t tot) {
    printf("============== IPv4 封装 ==============\n");
    
    // 设置 IPv4 帧的各个字段
    ip->version = 4;
    ip->header_length = 5;
    ip->type_of_service = 0;
    ip->total_length = 20 + tot; // IPv4 长度包括头部(20)和数据
    ip->identification = 0;
    ip->flags = 0;
    ip->fragment_offset = 0;
    ip->time_to_live = 255;
    ip->protocol = 17;           // 保持UDP协议号
    ip->header_checksum = 0;     // 先将校验和置零
    memcpy(ip->src_ip, src_ip, 4);
    memcpy(ip->dst_ip, dst_ip, 4);

    // 构建用于计算头部校验和的头部缓冲
    uint8_t checksum_header[20];
    uint8_t version_hdlen = (ip->version << 4) | ip->header_length;
    uint16_t flag_offset = (ip->flags << 13) | ip->fragment_offset;

    // 填充头部缓冲
    memcpy(checksum_header, &version_hdlen, 1);
    memcpy(checksum_header + 1, &ip->type_of_service, 1);
    memcpy(checksum_header + 2, &ip->total_length, 2);
    memcpy(checksum_header + 4, &ip->identification, 2);
    memcpy(checksum_header + 6, &flag_offset, 2);
    memcpy(checksum_header + 8, &ip->time_to_live, 1);
    memcpy(checksum_header + 9, &ip->protocol, 1);
    memcpy(checksum_header + 10, &ip->header_checksum, 2); // 此时是0
    memcpy(checksum_header + 12, src_ip, 4);
    memcpy(checksum_header + 16, dst_ip, 4);

    // 计算校验和
    ip->header_checksum = Checksum(checksum_header, 20);
    
    // 重新构建头部放入最终buffer
    memcpy(buffer, &version_hdlen, 1);
    memcpy(buffer + 1, &ip->type_of_service, 1);
    memcpy(buffer + 2, &ip->total_length, 2);
    memcpy(buffer + 4, &ip->identification, 2);
    memcpy(buffer + 6, &flag_offset, 2);
    memcpy(buffer + 8, &ip->time_to_live, 1);
    memcpy(buffer + 9, &ip->protocol, 1);
    memcpy(buffer + 10, &ip->header_checksum, 2);
    memcpy(buffer + 12, src_ip, 4);
    memcpy(buffer + 16, dst_ip, 4);
    
    ip_print(ip);
}

// 数据链路层封装函数
void data_link_layer_init(uint8_t *buffer, ethernet_frame *data_link, 
                          uint8_t src_mac[6], uint8_t dst_mac[6], uint16_t tot) {
    printf("============== 数据链路层封装 ==============\n");

    // 设置以太网帧各个字段
    memcpy(data_link->src_mac, src_mac, 6);
    memcpy(data_link->dst_mac, dst_mac, 6);
    data_link->type[0] = 0x08; // 上层协议类型 0x0800 表示 IPv4
    data_link->type[1] = 0x00;
    data_link->crc = 0;

    // 构建用于计算 CRC 的伪首部 (Header + Data)
    // tot 是 IP头长度 + 数据长度
    uint8_t *pseudo_header = (uint8_t *)malloc(14 + tot); 
    memcpy(pseudo_header, dst_mac, 6);
    memcpy(pseudo_header + 6, src_mac, 6);
    memcpy(pseudo_header + 12, data_link->type, 2);
    memcpy(pseudo_header + 14, buffer + 14, tot); // 将IP包(含数据)拷贝进来

    // 计算 CRC32
    data_link->crc = crc32(pseudo_header, 14 + tot);
    free(pseudo_header);

    memcpy(buffer, dst_mac, 6);
    memcpy(buffer + 6, src_mac, 6);
    memcpy(buffer + 12, data_link->type, 2);
    // 数据已经在 buffer + 14 处
    // 追加 CRC
    memcpy(buffer + 14 + tot, &data_link->crc, 4);

    data_link_layer_print(data_link);
    printf("数据链路传输成功！\n\n");
}

/* ============================================================
 * 3. 解封装函数实现
 * ============================================================ */

// 数据链路层解封装函数
void ethernet_receiver(uint8_t *buffer, ethernet_frame *ethernet_frame, uint16_t total_frame_len) {
    printf("============== 数据链路层解包 ==============\n");
    
    uint16_t data_len = total_frame_len - 14 - 4; // 减去头(14)和CRC(4)

    // 提取字段
    memcpy(ethernet_frame->dst_mac, buffer, 6);
    memcpy(ethernet_frame->src_mac, buffer + 6, 6);
    memcpy(ethernet_frame->type, buffer + 12, 2);
    memcpy(&ethernet_frame->crc, buffer + total_frame_len - 4, 4);

    // 重新计算 CRC 进行校验
    uint8_t *check_buf = (uint8_t*)malloc(total_frame_len - 4);
    memcpy(check_buf, buffer, total_frame_len - 4);
    uint32_t crc_result = crc32(check_buf, total_frame_len - 4);
    free(check_buf);

    printb(buffer + 14, data_len); // 打印IP包内容

    if (crc_result == ethernet_frame->crc) {
        printf("- CRC 校验通过\n");
    } else {
        printf("- CRC 校验失败 (Calc: %u, Recv: %u)\n", crc_result, ethernet_frame->crc);
    }
    
    data_link_layer_print(ethernet_frame);
}

// 网络层解封装函数
void ipv4_receiver(uint8_t *buffer, ipv4_frame *ip, uint16_t data_len) {
    printf("============== 网络层解包 ==============\n");
    
    // 这里的 buffer 指向 IP 头部起始位置
    uint8_t version_hdlen = buffer[0];
    ip->version = version_hdlen >> 4;
    ip->header_length = version_hdlen & 0x0F;
    
    ip->type_of_service = buffer[1];
    memcpy(&ip->total_length, buffer + 2, 2);
    memcpy(&ip->identification, buffer + 4, 2);
    
    uint16_t flag_offset;
    memcpy(&flag_offset, buffer + 6, 2);
    ip->flags = flag_offset >> 13;
    ip->fragment_offset = flag_offset & 0x1FFF;
    
    ip->time_to_live = buffer[8];
    ip->protocol = buffer[9];
    memcpy(&ip->header_checksum, buffer + 10, 2);
    memcpy(ip->src_ip, buffer + 12, 4);
    memcpy(ip->dst_ip, buffer + 16, 4);

    // 打印数据部分 (跳过IP头20字节)
    int payload_len = data_len - 20;
    printb(buffer + 20, payload_len);
    
    uint8_t header_backup[20];
    memcpy(header_backup, buffer, 20);
    // 将校验和字段置0用于重算
    header_backup[10] = 0;
    header_backup[11] = 0;
    
    uint16_t calc_sum = Checksum(header_backup, 20);
    printf("校验和: %d\n", calc_sum);
    
    if (calc_sum == ip->header_checksum) {
        printf("校验和校验通过！\n");
    } else {
        printf("校验和校验失败！\n");
    }

    ip_print(ip);
    
    // 简单展示应用数据
    printf("\n[最终获取的数据]: ");
    for(int i=0; i<payload_len; i++) {
        printf("%c", buffer[20+i]);
    }
    printf("\n");
}

/* ============================================================
 * 4. 主程序：代码运行结果展示
 * ============================================================ */
int main() {
    // 模拟数据
    char *message = "Hello, Computer Network Project!";
    int msg_len = strlen(message);
    
    // 定义缓冲区
    // 预留空间：14(Ethernet Head) + 20(IP Head) + Data + 4(CRC)
    uint8_t tx_buffer[MAX_BUFFER_SIZE]; 
    memset(tx_buffer, 0, MAX_BUFFER_SIZE);

    // 指针定位：为了方便封装，我们先把数据放在预留好头部的位置
    // IP头开始位置应为 tx_buffer + 14
    // 数据开始位置应为 tx_buffer + 14 + 20
    uint8_t *ip_ptr = tx_buffer + 14;
    uint8_t *data_ptr = tx_buffer + 14 + 20;
    
    memcpy(data_ptr, message, msg_len);

    // 定义地址
    uint8_t src_ip[4] = {192, 168, 1, 1};
    uint8_t dst_ip[4] = {192, 168, 1, 2};
    uint8_t src_mac[6] = {0x00, 0x0c, 0x29, 0x1a, 0x2b, 0x3c};
    uint8_t dst_mac[6] = {0x00, 0x0c, 0x29, 0x1a, 0x2b, 0x3d};

    ipv4_frame ip_frame;
    ethernet_frame eth_frame;

    printf("\n>>> 开始封装过程 <<<\n\n");

    // 1. 网络层封装
    // 传入 IP头起始位置，IP载荷长度(msg_len)
    ip_init(ip_ptr, &ip_frame, src_ip, dst_ip, msg_len);

    // 2. 数据链路层封装
    // 传入 帧头起始位置(tx_buffer)，负载长度(IP头20 + 数据)
    data_link_layer_init(tx_buffer, &eth_frame, src_mac, dst_mac, 20 + msg_len);

    // 模拟传输：tx_buffer 现在包含了完整的帧
    int total_frame_len = 14 + 20 + msg_len + 4;
    
    printf("\n>>> 模拟传输完成，开始解封装过程 <<<\n\n");

    // 3. 数据链路层解封装
    ethernet_frame recv_eth_frame;
    ethernet_receiver(tx_buffer, &recv_eth_frame, total_frame_len);

    // 4. 网络层解封装
    // 剥离以太网头(14字节)，传入IP包起始位置
    ipv4_frame recv_ip_frame;
    // 数据长度 = 总帧长 - 以太头14 - CRC4
    ipv4_receiver(tx_buffer + 14, &recv_ip_frame, total_frame_len - 18);

    return 0;
}