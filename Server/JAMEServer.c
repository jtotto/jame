#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sys/types.h>

#include <alsa/asoundlib.h> 
#include <gtk/gtk.h>
#include <pthread.h>

// ALSA
snd_seq_t *seq_handle;
snd_seq_event_t ev; // No reason not to allocate this on the stack.
int oportid;        // output port id

// GTK
GtkWidget *portSpin, *channelSpin, *ccSpin;
int listening;
void initialize_GUI();

// Server
pthread_t worker;
#define SOCKET 0
#define CONNECTION 1
void *serve_midi(void *);

struct jame_configuration_t
{
    int port;
    int midi_cc;
    int midi_channel;
    snd_seq_t *alsa_seq_handle;
    snd_seq_event_t *event;
} worker_configuration;

int main(int argc, char *argv[])
{
    // ALSA stuff first.
    if (snd_seq_open(&seq_handle, "hw", SND_SEQ_OPEN_OUTPUT, 0) < 0) 
    {
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
        exit(1);
    }

    // These properties of the event don't change.
    snd_seq_ev_clear( &ev );
    snd_seq_ev_set_source( &ev, oportid );
    snd_seq_ev_set_subs( &ev );
    snd_seq_ev_set_direct( &ev );

    // GTK next.
    gtk_init(&argc, &argv); 
    initialize_GUI();
    gtk_main();
}

// GTK functions.
static gboolean delete_event(GtkWidget *widget, GdkEvent *event, gpointer data)
{
    pthread_cancel(worker);
    gtk_main_quit();
    return FALSE;
}

// I always found it super weird that GUI libraries don't let you provide two different callbacks for toggle buttons.
void toggleListening(GtkWidget *widget, gpointer data)
{
    listening = !listening; // Toggle the internal state monitor.
    if(listening)
    {
        worker_configuration.port = gtk_spin_button_get_value_as_int((GtkSpinButton *) portSpin);
        worker_configuration.midi_channel = gtk_spin_button_get_value_as_int((GtkSpinButton *) channelSpin);
        worker_configuration.midi_cc = gtk_spin_button_get_value_as_int((GtkSpinButton *) ccSpin);
        worker_configuration.alsa_seq_handle = seq_handle;
        worker_configuration.event = &ev;

        if(pthread_create(&worker, NULL, serve_midi, &worker_configuration) != 0)
        {
            printf("Couldn't start worker thread!\n");
            exit(1);
        }
    }
    else
    {
        if(pthread_cancel(worker) != 0)
        {
            printf("Error cancelling thread!\n");
            exit(1);
        }
    }
}

void initialize_GUI()
{
    GtkWidget *window; // The main UI window.
    GtkWidget *grid; // This grid holds all of the individual UI elements.
    GtkWidget *label; 

    listening = 0; // Used by the 'listen' callback to determine the correct action.
    window = gtk_window_new(GTK_WINDOW_TOPLEVEL);  // Required call.

    gtk_window_set_title (GTK_WINDOW(window), "JAME Server");

    g_signal_connect(window, "delete-event", G_CALLBACK(delete_event), NULL); 
    gtk_container_set_border_width(GTK_CONTAINER(window), 10); // Pretty self-explanatory - sets the border width of 'window.'

    grid = gtk_grid_new();
    gtk_grid_set_row_spacing(grid, 5);
    gtk_container_add(GTK_CONTAINER(window), grid); // adds the mainBox box to the window container.  Recall that the structure of GTK
                                                    // dictates that only one child be added using gtk_container_add - actual layout is done
                                                    // using the box construct itself
    
    // I could probably have defined a helper method for this part, but I think it would take away a lot of flexibility.

    // Port row.
    label = gtk_label_new("TCP Port: ");
    gtk_widget_show(label);
    gtk_misc_set_alignment(GTK_MISC(label),0.0,0.5);
    gtk_grid_attach(grid, label, 0, 0, 1, 1);

    GtkAdjustment *adjustment = (GtkAdjustment *) gtk_adjustment_new(4444.0, 0.0, 65535.0, 1.0, 10.0, 0.0);
    // Note that gtk_adjustment_new takes doubles for parameters, even though I'm specifying whole numbers.

    portSpin = gtk_spin_button_new(adjustment, 10.0, 0);   // takes the pointer to the necessary adjustment, the amount to climb when the
                                                           // increment/decrement buttons are pressed, and the number of decimal places to
                                                           // display.
    gtk_widget_show(portSpin);
    gtk_grid_attach_next_to(grid, portSpin, label, GTK_POS_RIGHT, 1, 1);

    // Channel row.
    label = gtk_label_new("MIDI Channel: ");
    gtk_widget_show(label);
    gtk_misc_set_alignment(GTK_MISC(label),0.0,0.5);
    gtk_grid_attach(grid, label, 0, 1, 1, 1);

    adjustment = (GtkAdjustment *) gtk_adjustment_new(0.0, 0.0, 15.0, 1.0, 10.0, 0.0);
    channelSpin = gtk_spin_button_new(adjustment, 1.0, 0);
    gtk_widget_show(channelSpin);
    gtk_grid_attach_next_to(grid, channelSpin, label, GTK_POS_RIGHT, 1, 1);

    // CC row.
    label = gtk_label_new("MIDI CC Parameter: ");
    gtk_widget_show(label);
    gtk_misc_set_alignment(GTK_MISC(label),0.0,0.5);
    gtk_grid_attach(grid, label, 0, 2, 1, 1);

    adjustment = (GtkAdjustment *) gtk_adjustment_new(0.0, 0.0, 127.0, 1.0, 10.0, 0.0);
    ccSpin = gtk_spin_button_new(adjustment, 10.0, 0);
    gtk_widget_show(ccSpin);
    gtk_grid_attach_next_to(grid, ccSpin, label, GTK_POS_RIGHT, 1, 1);

    // Listen button.
    GtkWidget *toggleButton = gtk_toggle_button_new_with_label("Hey, Listen!");
    gtk_widget_show(toggleButton);
    gtk_grid_attach(grid, toggleButton, 1, 3, 1, 1);

    g_signal_connect(toggleButton, "toggled", G_CALLBACK(toggleListening), NULL);

    // Show the window.
    gtk_widget_show(grid);
    gtk_widget_show(window);
}

// Server functions.
void *server_cleanup_handler(void *resources)
{
    int *resource_handles = (int *) resources;
    if(!(resource_handles[SOCKET] < 0))
    {
        close(resource_handles[SOCKET]);
    }
    if(!(resource_handles[CONNECTION] < 0))
    {
        close(resource_handles[CONNECTION]);
    }
}

void *serve_midi(void *args)
{
    struct jame_configuration_t *server_parameters = (struct jame_configuration_t *) args;

    // Initialize the MIDI event for this session.
    snd_seq_ev_set_controller(server_parameters->event, server_parameters->midi_channel, server_parameters->midi_cc, 0);
    signed char midi;
    signed char *event_value = &(server_parameters->event->data.control.value); // So I don't have to evaluate this address every time.

    struct sockaddr_in serv_addr; 
    int resource_handles[2] = { -1, -1 };

    pthread_cleanup_push(server_cleanup_handler, resource_handles); // Set the resource cleanup handler.

    resource_handles[SOCKET] = socket(AF_INET, SOCK_STREAM, 0);

    memset(&serv_addr, '0', sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    serv_addr.sin_port = htons(server_parameters->port); // Could the endianness conversion functions be named any more cryptically?

    bind(resource_handles[SOCKET], (struct sockaddr*)&serv_addr, sizeof(serv_addr)); 

    listen(resource_handles[SOCKET], 1);

    while(1)
    {
        resource_handles[CONNECTION] = accept(resource_handles[SOCKET], (struct sockaddr*)NULL, NULL); // Cancellation point.

        while(1)
        {
            // In case it's not clear: -1 is a sentinel value send by the client indicating that it is done transmitting data for now.
            if(read(resource_handles[CONNECTION], &midi, sizeof(midi)) < 0 || midi == -1) // Cancellation point.
            {
                printf("Terminating communication with this connection.\n");
                close(resource_handles[CONNECTION]);
                resource_handles[CONNECTION] = -1; // Mark the descriptor as invalid.
                break;
            }
            printf("%d\n", midi);
            *(event_value) = midi;
            snd_seq_event_output_direct( seq_handle, &ev );
        }
    }

    // Even though there's no way for the flow of control to reach this part, this is required.
    // pthread_cleanup_push is a macro that contains an open bracket which must be closed by the pthread_cleanup_pop macro.
    pthread_cleanup_pop(0);
}
