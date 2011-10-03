#include <gst/gst.h>
#include <string.h>

/* the main event loop continuously checking event sources */
static GMainLoop *loop = NULL;

/* the pipeline that controls the data flow */
static GstElement *pipeline = NULL;

typedef struct
{
	GstElement *pipeline;
	gint64 duration;
} GstTimeout;

/*
 * Message handler for the pipeline. Will shutdown the pipeline on an error 
 * or an end-of-stream event.
 */
static gboolean message_handler(GstBus *bus, GstMessage *msg, gpointer data)
{
	switch (GST_MESSAGE_TYPE (msg))
	{
		case GST_MESSAGE_ERROR:
		{
			GError *err;
			gchar *debug;
			gst_message_parse_error(msg, &err, &debug);
			g_print("ERROR  %s:%s\n", err->message, debug);
			
			g_main_loop_quit(loop);
			break;
		}
		case GST_MESSAGE_EOS:
		{
			g_print("\nPipeline received EOS...shutting down.\n");
			g_main_loop_quit(loop);
			gst_element_set_state(pipeline, GST_STATE_NULL);
			break;
		}
		default:
			break;
	}

	return TRUE;
}

/* Display error message when trying to link two elements in a pipeline */
static void link_error(GstElement *src, GstElement *sink)
{
	g_error("Error: Could not link %s to %s.\n", gst_element_get_name(src), gst_element_get_name(sink));
}

/* Dynamic pad creation and linking */
static void pad_added_callback(GstElement *element, GstPad *pad, gpointer data)
{
	GstPad *sinkpad;
	GstElement *sink = (GstElement *) data;

	sinkpad = gst_element_get_static_pad(sink, "sink");
	gst_pad_link(pad, sinkpad);
	gst_object_unref(sinkpad);
}

/* Function that will send an EOS to the pipeline when called */
void send_eos(int sig)
{
	g_print("\nInterrupt caught -- sending EOS...");
	gst_element_send_event(pipeline, gst_event_new_eos());
}

/* Check if the capture time is up, if so, send EOS to pipeline */
static gboolean is_done(GstTimeout *timeout)
{
	GstQuery *query = gst_query_new_position(GST_FORMAT_TIME);
	if (gst_element_query(timeout->pipeline, query))
	{

		gint64 pos;
		gst_query_parse_position(query, NULL, &pos);
		if (pos >= timeout->duration)
		{
			g_print("Times up...sending EOS.");
			gst_element_send_event(timeout->pipeline, gst_event_new_eos());
			return FALSE;
		}
	}
	return TRUE;
}

/* Main entry */
int main(int argc, char *argv[])
{
	/* initialize elemnts in our pipeline and the bus to receive messages */
	GstElement *filesrc, *queue, *mpegpsdemux, *mpegvideoparse, *mpeg2dec, *encode, *mux, *filesink;
	GstBus *bus;

  /* Setup Ctrl-C to send the EOS to the pipeline. */
	(void) signal(SIGINT, send_eos);

  /* Argument parsing */
	gchar *source = "";
	gchar *outputfile = "hauppauge.mpg";
	gchar *enc = "ffenc_mpeg2video";
	guint bitrate = 300000;
	gchar *container = "mpegtsmux";
	guint length = 0;
	GOptionContext *context = NULL;
	GError *err;
	GOptionEntry entries[] = 
	{
		{"device", 'd', 0, G_OPTION_ARG_STRING, &source, "Hauppauge device location", NULL},
		{"output", 'o', 0, G_OPTION_ARG_STRING, &outputfile, "Name of file to save (default: hauppauge.mpg)", NULL},
		{"encode", 'e', 0, G_OPTION_ARG_STRING, &enc, "GStreamer encoding element (default: ffenc_mpeg2video)", NULL},
		{"bitrate", 'b', 0, G_OPTION_ARG_INT, &bitrate, "Bitrate (in bits) to use for encoding (default: 300000)", NULL},
		{"container", 'c', 0, G_OPTION_ARG_STRING, &container, "GStreamer container element (default: mpegtsmux)", NULL},
		{"length", 't', 0, G_OPTION_ARG_INT, &length, "Time to run pipeline in seconds.", NULL},
		{NULL}
	};
	if (!g_thread_supported())
	  g_thread_init (NULL);
	context = g_option_context_new("- Hauppauge Without Confidence");
	g_option_context_add_main_entries(context, entries, NULL);
	g_option_context_add_group(context, gst_init_get_option_group());
	if (!g_option_context_parse(context, &argc, &argv, &err)) {
		g_error("%s\n", err->message);
		return FALSE;
	}
	if (strlen(source) == 0) {
		g_error("No source specified.");
		return FALSE;
	}
	
	/* initializes built-in elements and plugins */
	gst_init(&argc, &argv);

	/* creates the main event loop structure */
	loop = g_main_loop_new(NULL, FALSE);
	
	/* some helpful verbosity */
	g_print("Hauppauge Capture Without Confidence Monitoring using %s\n", gst_version_string());
	g_print(" * capturing from %s\n", source);
	g_print(" * saving video stream to %s\n", outputfile);
	g_print(" * using %s to encode with a bitrate of %d\n", enc, bitrate);
	g_print(" * container for stream is %s\n", container);

	/* create the elements and construct the pipeline */
	pipeline = gst_pipeline_new("hauppauge_without_confidence");
	filesrc = gst_element_factory_make("filesrc", NULL);
	queue = gst_element_factory_make("queue", NULL);
	mpegpsdemux = gst_element_factory_make("mpegpsdemux", NULL);
	mpegvideoparse = gst_element_factory_make("mpegvideoparse", NULL);
	mpeg2dec = gst_element_factory_make("mpeg2dec", NULL);
	encode = gst_element_factory_make(enc, NULL);
	mux = gst_element_factory_make(container, NULL);
	filesink = gst_element_factory_make("filesink", NULL);
	gst_bin_add_many(GST_BIN (pipeline), filesrc, queue, mpegpsdemux, mpegvideoparse, mpeg2dec, encode, mux, filesink, NULL);

	/* set element properties */
	g_object_set(G_OBJECT (filesrc), "location", source, NULL);
	g_object_set(G_OBJECT (encode), "bitrate", bitrate, NULL);
	g_object_set(G_OBJECT (filesink), "location", outputfile, NULL);

	bus = gst_element_get_bus(pipeline);
	gst_bus_add_watch(bus, message_handler, NULL);
	gst_object_unref(bus);

	/* link the sink and src pads of the elements to construct the pipeline */
	if (!gst_element_link(filesrc, queue)) {
		link_error(filesrc, queue);
		return FALSE;
	} else if (!gst_element_link(queue, mpegpsdemux)) {
		link_error(queue, mpegpsdemux);
		return FALSE;
	} else if (!gst_element_link(mpegvideoparse, mpeg2dec)) {
		link_error(mpegvideoparse, mpeg2dec);
		return FALSE;
	} else if (!gst_element_link(mpeg2dec, encode)) {
	  link_error(mpeg2dec, encode);
	  return FALSE;
	} else if (!gst_element_link(encode, mux)) {
	  link_error(encode, mux);
	  return FALSE;
	} else if (!gst_element_link(mux, filesink)) {
		link_error(mux, filesink);
		return FALSE;
	}
	
	/* listen for newly created pads to dynamically link to mpegpsdemux element */
	g_signal_connect(mpegpsdemux, "pad-added", G_CALLBACK (pad_added_callback), mpegvideoparse);
	
	/* if provided a duration for the capture */
	if (length > 0)
	{
		GstTimeout *timeout;
		timeout = malloc(sizeof(GstTimeout));
		timeout->pipeline = pipeline;
		timeout->duration = GST_SECOND * length;
		g_timeout_add_seconds(1, (GSourceFunc) is_done, timeout);
	}

	/* start the pipeline */
	gst_element_set_state(GST_ELEMENT (pipeline), GST_STATE_PLAYING);
	while (GST_STATE (pipeline) != GST_STATE_PLAYING);
	g_print("*Capturing*\nPress Ctrl-C to send EOS\n");
	g_main_loop_run(loop);

	return TRUE;
}
