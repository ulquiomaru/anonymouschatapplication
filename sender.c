#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>

#include <arpa/inet.h>
#include <linux/if_packet.h>
#include <net/if.h>
#include <netinet/ether.h>
#include <netinet/ip.h>
#include <netinet/udp.h>
#include <sys/ioctl.h>
#include <sys/socket.h>

#define MY_DEST_MAC0	0xFF
#define MY_DEST_MAC1	0xFF
#define MY_DEST_MAC2	0xFF
#define MY_DEST_MAC3	0xFF
#define MY_DEST_MAC4	0xFF
#define MY_DEST_MAC5	0xFF

#define BUF_SIZE	1500
#define DEFAULT_IF	"enp0s8"

struct pseudo_header {
	u_int32_t source_address;
	u_int32_t dest_address;
	u_int8_t placeholder;
	u_int8_t protocol;
	u_int16_t udp_length;
};

unsigned short csum(unsigned short *ptr, int nbytes) {
	register long sum;
	unsigned short oddbyte;
	register short answer;

	sum = 0;
	while (nbytes > 1) {
		sum += *ptr++;
		nbytes -= 2;
	}

	if (nbytes == 1) {
		oddbyte = 0;
		*((u_char*) &oddbyte) = *(u_char*) ptr;
		sum += oddbyte;
	}

	sum = (sum >> 16) + (sum & 0xffff);
	sum = sum + (sum >> 16);
	answer = (short) ~sum;

	return (answer);
}

int main(int argc, char *argv[]) {
	int sockfd;
	struct ifreq if_idx;
	struct ifreq if_mac;
	int tx_len = 0;

	/* packet */
	char sendbuf[BUF_SIZE];

	/* zero out the packet */
	memset(sendbuf, 0, BUF_SIZE);

	/* ethernet header */
	struct ether_header *eh = (struct ether_header *) sendbuf;

	/* ip header */
	struct iphdr *iph = (struct iphdr *) (sendbuf + sizeof(struct ether_header));

	/* udp header */
	struct udphdr *udph = (struct udphdr *) (sendbuf + sizeof(struct ether_header) + sizeof(struct iphdr));

	/* data */
	char *data = sendbuf + sizeof(struct ether_header) + sizeof(struct iphdr) + sizeof(struct udphdr);

	struct sockaddr_ll socket_address;

	char ifName[IFNAMSIZ];

	/* Get interface name */
	strcpy(ifName, DEFAULT_IF);

	/* get data */
	if (argc > 1)
		strcpy(data, argv[1]);
	else
		strcpy(data, "ABCDEFGHIJKLMNOPQRSTUVWXYZ");

	/* Open RAW socket to send on */
	if ((sockfd = socket(AF_PACKET, SOCK_RAW, IPPROTO_RAW)) == -1) {
		perror("Socket Error");
	}

	int broadcast = 1;
	if (setsockopt(sockfd, SOL_SOCKET, SO_BROADCAST, &broadcast, sizeof(broadcast)) == -1) {
		perror("Failed to broadcast");
		exit(1);
	}

	/* Get the index of the interface to send on */
	memset(&if_idx, 0, sizeof(struct ifreq));
	strncpy(if_idx.ifr_name, ifName, IFNAMSIZ - 1);
	if (ioctl(sockfd, SIOCGIFINDEX, &if_idx) < 0)
		perror("SIOCGIFINDEX");

	/* Get the MAC address of the interface to send on */
	memset(&if_mac, 0, sizeof(struct ifreq));
	strncpy(if_mac.ifr_name, ifName, IFNAMSIZ - 1);
	if (ioctl(sockfd, SIOCGIFHWADDR, &if_mac) < 0)
		perror("SIOCGIFHWADDR");

	/* Ethernet header */
	eh->ether_shost[0] = 0x00;
	eh->ether_shost[1] = 0x00;
	eh->ether_shost[2] = 0x00;
	eh->ether_shost[3] = 0x00;
	eh->ether_shost[4] = 0x00;
	eh->ether_shost[5] = 0x00;
	eh->ether_dhost[0] = MY_DEST_MAC0;
	eh->ether_dhost[1] = MY_DEST_MAC1;
	eh->ether_dhost[2] = MY_DEST_MAC2;
	eh->ether_dhost[3] = MY_DEST_MAC3;
	eh->ether_dhost[4] = MY_DEST_MAC4;
	eh->ether_dhost[5] = MY_DEST_MAC5;

	/* Ethertype field */
	eh->ether_type = htons(ETH_P_IP);

	/* Index of the network device */
	socket_address.sll_ifindex = if_idx.ifr_ifindex;

	/* Address length*/
	socket_address.sll_halen = ETH_ALEN;

	/* Destination MAC */
	socket_address.sll_addr[0] = MY_DEST_MAC0;
	socket_address.sll_addr[1] = MY_DEST_MAC1;
	socket_address.sll_addr[2] = MY_DEST_MAC2;
	socket_address.sll_addr[3] = MY_DEST_MAC3;
	socket_address.sll_addr[4] = MY_DEST_MAC4;
	socket_address.sll_addr[5] = MY_DEST_MAC5;

	char source_ip[32], *pseudogram;
	struct pseudo_header psh;	

	strcpy(source_ip, "0.0.0.0"); // Spoofed IP

	/* IP Header */
	iph->ihl = 5;
	iph->version = 4;
	iph->tos = 0;
	iph->tot_len = htons(sizeof(struct iphdr) + sizeof(struct udphdr) + strlen(data));
	iph->id = htons(12345); //ID of this packet
	iph->frag_off = htons(IP_DF);
	iph->ttl = 64;
	iph->protocol = IPPROTO_UDP;
	iph->check = 0; //Set to 0 before calculating checksum
	iph->saddr = inet_addr(source_ip);
	iph->daddr = inet_addr("192.168.56.255"); // Broadcast IP Address

	/* IP Checksum */
	iph->check = csum((unsigned short *) iph, 20);

	/* UDP Header */
	udph->source = htons(0);
	udph->dest = htons(7777);
	udph->len = htons(8 + strlen(data));
	udph->check = 0; //Set to 0 before calculating checksum

	/* UDP Checksum Pseudoheader */
	psh.source_address = inet_addr(source_ip);
	psh.dest_address = inet_addr("192.168.56.255");
	psh.placeholder = 0;
	psh.protocol = IPPROTO_UDP;
	psh.udp_length = htons(sizeof(struct udphdr) + strlen(data));

	int psize = sizeof(struct pseudo_header) + sizeof(struct udphdr) + strlen(data);
	pseudogram = malloc(psize);

	memcpy(pseudogram, (char*) &psh, sizeof(struct pseudo_header));
	memcpy(pseudogram + sizeof(struct pseudo_header), udph, sizeof(struct udphdr) + strlen(data));

	/* Set the UDP Checksum */
	udph->check = csum((unsigned short*) pseudogram, psize);

	/* Final transmit length in bytes */
	tx_len = sizeof(struct ether_header) + sizeof(struct iphdr) + sizeof(struct udphdr) + strlen(data);

	/* Send Packet */
	if (sendto(sockfd, sendbuf, tx_len, 0, (struct sockaddr*) &socket_address, sizeof(struct sockaddr_ll)) < 0) {
		printf("Send failed\n");
	}
	else {
		printf("Packet sent! (%d bytes)\n", tx_len);
	}
	return 0;
}
