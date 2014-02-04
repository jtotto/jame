/* JAME Android MIDI Expression project.
Copyright (C) 2014  Joshua Otto

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. */

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
#include <alsa/asoundlib.h> // stdio, stdlib and unistd were also used in the example I'm working from, but were already imported.

int main(int argc, char *argv[])
{
    // ALSA stuff first.
    snd_seq_t *seq_handle;
    snd_seq_event_t ev; // No reason not to allocate this on the stack.
    int oportid;         /* output port */

    if (snd_seq_open(&seq_handle, "hw", SND_SEQ_OPEN_OUTPUT, 0) < 0) {
        printf("Error opening ALSA sequencer.\n");
        exit(1);
    }

    snd_seq_set_client_name(seq_handle, "JAME"); 

    if(oportid = snd_seq_create_simple_port
         (seq_handle, "Output",
          SND_SEQ_PORT_CAP_READ | SND_SEQ_PORT_CAP_SUBS_READ,
          SND_SEQ_PORT_TYPE_APPLICATION) < 0)
    {
        printf("Error opening output port.\n");
    }

    // TCP stuff now.
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

        ticks = time(NULL);
        while(1)
        {
            read(connfd, &midi, sizeof(midi)); 
            // printf("%d\n", midi);
            snd_seq_ev_clear(&ev);
            snd_seq_ev_set_source( &ev, oportid );
            snd_seq_ev_set_subs( &ev );
            snd_seq_ev_set_direct( &ev );
            snd_seq_ev_set_controller(&ev, 1, 1, midi);
            snd_seq_event_output_direct( seq_handle, &ev );
        }
        close(connfd);
        sleep(1);
     }
}
