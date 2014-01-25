#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <time.h> 

int main(int argc, char *argv[])
{
    printf("Started...");
    int listenfd = 0, connfd = 0;
    struct sockaddr_in serv_addr; 

    char readBuff[1025];
    char midi;
    time_t ticks; 

    listenfd = socket(AF_INET, SOCK_STREAM, 0);
    memset(&serv_addr, '0', sizeof(serv_addr));
    memset(readBuff, '0', sizeof(readBuff)); 

    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    serv_addr.sin_port = htons(4444); 

    bind(listenfd, (struct sockaddr*)&serv_addr, sizeof(serv_addr)); 

    listen(listenfd, 10); 

    while(1)
    {
        printf("Waiting...");
        connfd = accept(listenfd, (struct sockaddr*)NULL, NULL); 
        printf("OH MY GOD FUCK YES");

        ticks = time(NULL);
        while(1)
        {
            read(connfd, &midi, sizeof(midi)); 
            printf("%d\n", midi);
        }
        close(connfd);
        sleep(1);
     }
}
