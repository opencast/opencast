#include <gst/gst.h>
#include <gst/app/gstappsink.h>
#include <gtk/gtk.h>
#include <string.h>

/* the main event loop continuously checking event sources */
static GMainLoop *loop = NULL;

/* the pipeline that controls the data flow */
static GstElement *pipeline = NULL;

/* Global variable to determine what time was lasted used to save an image */
static gint64 previous = -1;

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
			g_error("%s:%s\n", err->message, debug);
			
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

static void queue_overrun(GstElement *queue, gpointer data)
{
  guint cur_bytes;
  guint max_bytes;
  guint diff_bytes;
  g_object_get(G_OBJECT (queue), "current-level-bytes", &cur_bytes, NULL);
  g_object_get(G_OBJECT (queue), "max-size-bytes", &max_bytes, NULL);
  diff_bytes = cur_bytes - max_bytes;
  gint64 seconds = GST_TIME_AS_SECONDS (gst_clock_get_time(gst_element_get_clock(GST_ELEMENT (queue))));
  
  g_print("%" G_GINT64_FORMAT "\t%s\toverrun by %d bytes\n", seconds, gst_element_get_name(queue), diff_bytes);
}

/* G_CALLBACK function that takes a buffer and if it exists at a certain time
 * in the stream it will be saved as a PNG to disk for confidence monitoring */
static void new_buffer(GstElement *element, gpointer data)
{
  GstAppSink *appsink = (GstAppSink *) element;
  gint64 interval = *((gint64 *) data);
  gchar imgname[20];
  GstBuffer *b = gst_app_sink_pull_buffer(appsink);
  GstBuffer *buffer = gst_buffer_copy(b);
  gint64 seconds = GST_TIME_AS_SECONDS (gst_clock_get_time(gst_element_get_clock(GST_ELEMENT (appsink))));
  gst_buffer_unref(b);
  
  if (seconds % interval == 0 && seconds != previous) {
    gint width, height;
    gboolean result;
    GdkPixbufLoader *loader;
    GdkPixbuf *pixbuf;
    GError *error = NULL;
    GstCaps *caps = GST_BUFFER_CAPS (buffer);
    GstStructure *structure = gst_caps_get_structure (caps, 0);
    
    result = gst_structure_get_int(structure, "width", &width);
    result |= gst_structure_get_int(structure, "height", &height);
    if (!result)
      return;
    
    sprintf(imgname, "%" G_GINT64_FORMAT, seconds);
    loader = gdk_pixbuf_loader_new();
    if (gdk_pixbuf_loader_write(loader, buffer->data, buffer->size, &error) &&
        gdk_pixbuf_loader_close(loader, &error)) {
      pixbuf = gdk_pixbuf_loader_get_pixbuf(loader);
      if (pixbuf) {
        g_object_ref(pixbuf);
        char output[27] = "images/";
        strcat(imgname, ".jpg");
        strcat(output, imgname);
        gdk_pixbuf_save (pixbuf, output, "jpeg", &error, NULL);
      }
     } else {
      GST_WARNING("Could not convert buffer to pixbuf: %s", error->message);
      g_error_free(error);
     }
    previous = seconds;
  }
  gst_buffer_unref(buffer);
}

/* Function that will send an EOS to the pipeline when called */
void send_eos(int sig)
{
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
			gst_element_send_event(timeout->pipeline, gst_event_new_eos());
			return FALSE;
		}
	}
	return TRUE;
}

static void set_queue_size(GstElement *queue)
{
  g_object_set(G_OBJECT (queue), "max-size-buffers", 0, NULL);
  g_object_set(G_OBJECT (queue), "max-size-time", 0, NULL);
  g_object_set(G_OBJECT (queue), "max-size-bytes", 512000000, NULL);
}

/* Main entry */
int main(int argc, char *argv[])
{
	/* initialize elemnts in our pipeline and the bus to receive messages */
	GstElement *v4lsrc, *queue, *videorate, *capsfilter, *queue2, *ffmpegcolorspace, *encode, *mux, *filesink;
	GstElement *tee, *queue3, *ffmpegcolorspace1, *capsfilter1, *jpegenc, *queue4, *appsink;
	GstBus *bus;
	GstCaps *fpsCaps, *rgbCaps;

  /* Setup Ctrl-C to send the EOS to the pipeline. */
	(void) signal(SIGINT, send_eos);

  /* Argument parsing */
	gchar *source = "";
	gchar *outputfile = "vga.mpg";
	gchar *enc = "ffenc_mpeg2video";
	guint bitrate = 300000;
	gchar *container = "mpegtsmux";
	guint length = 0;
	gint64 interval = 5;
	gint quantizer = 21;
	GOptionContext *context = NULL;
	GError *err;
	GOptionEntry entries[] = 
	{
		{"device", 'd', 0, G_OPTION_ARG_STRING, &source, "Epiphan VGA2USB device location", NULL},
		{"output", 'o', 0, G_OPTION_ARG_STRING, &outputfile, "Name of file to save (default: vga.mpg)", NULL},
		{"encode", 'e', 0, G_OPTION_ARG_STRING, &enc, "GStreamer encoding element (default: ffenc_mpeg2video)", NULL},
		{"bitrate", 'b', 0, G_OPTION_ARG_INT, &bitrate, "Bitrate (in bits) to use for encoding (default: 300000)", NULL},
		{"container", 'c', 0, G_OPTION_ARG_STRING, &container, "GStreamer container element (default: mpegtsmux)", NULL},
		{"length", 't', 0, G_OPTION_ARG_INT, &length, "Time to run pipeline in seconds.", NULL},
		{"interval", 'i', 0, G_OPTION_ARG_INT, &interval, "Interval between confidence images in seconds. (default: 5)", NULL},
		{"quantizer", 'q', 0, G_OPTION_ARG_INT, &quantizer, "Constant quantization value for H.264 encoding (default: 21)", NULL}, 
		{NULL}
	};
	if (!g_thread_supported())
	  g_thread_init (NULL);
	context = g_option_context_new("- VGA2USB With Confidence");
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
	
	mkdir("images", 00777);

	/* create the elements and construct the pipeline */
	pipeline = gst_pipeline_new("vga2usb_without_confidence");
	v4lsrc = gst_element_factory_make("v4lsrc", NULL);
	queue = gst_element_factory_make("queue", "q_v4lsrc");
	videorate = gst_element_factory_make("videorate", NULL);
	capsfilter = gst_element_factory_make("capsfilter", NULL);
	tee = gst_element_factory_make("tee", NULL);
	queue2 = gst_element_factory_make("queue", "q_tee1");
	ffmpegcolorspace = gst_element_factory_make("ffmpegcolorspace", NULL);
	encode = gst_element_factory_make(enc, NULL);
	mux = gst_element_factory_make(container, NULL);
	filesink = gst_element_factory_make("filesink", NULL);
	queue3 = gst_element_factory_make("queue", "q_tee2");
	ffmpegcolorspace1 = gst_element_factory_make("ffmpegcolorspace", NULL);
	capsfilter1 = gst_element_factory_make("capsfilter", NULL);
	jpegenc = gst_element_factory_make("jpegenc", NULL);
	queue4 = gst_element_factory_make("queue", "q_jpegenc");
	appsink = gst_element_factory_make("appsink", NULL);
	gst_bin_add_many(GST_BIN (pipeline), v4lsrc, queue, videorate, capsfilter, tee,
	    queue2, ffmpegcolorspace, encode, mux, filesink, queue3, 
	    ffmpegcolorspace1, capsfilter1, jpegenc, queue4, appsink, NULL);

  fpsCaps = gst_caps_from_string("video/x-raw-yuv, framerate=10/1");
	rgbCaps = gst_caps_from_string("video/x-raw-rgb,bpp=(int)24,depth=(int)24,endianness=(int)4321");

	/* set element properties */
	set_queue_size(queue);
	set_queue_size(queue2);
	set_queue_size(queue3);
	set_queue_size(queue4);
	g_object_set(G_OBJECT (v4lsrc), "device", source, NULL);
	g_object_set(G_OBJECT (capsfilter), "caps", fpsCaps, NULL);
	if (strcmp(enc, "x264enc") == 0)
	  g_object_set(G_OBJECT (encode), "quantizer", quantizer, NULL);
	else
	  g_object_set(G_OBJECT (encode), "bitrate", bitrate, NULL);
	g_object_set(G_OBJECT (filesink), "location", outputfile, NULL);
	g_object_set(G_OBJECT (capsfilter1), "caps", rgbCaps, NULL);
	g_object_set(G_OBJECT (appsink), "emit-signals", TRUE, NULL);
	
	
	/* helpful verbosity */
	g_print("VGA2USB Capture With Confidence Monitoring using %s\n", gst_version_string());
	g_print(" * capturing from %s\n", source);
	g_print(" * saving video stream to %s\n", outputfile);
	if (strcmp(enc, "x264enc") == 0)
	  g_print(" * using %s to encode with a quantizer of %d\n", enc, quantizer);
	else
	  g_print(" * using %s to encode with a bitrate of %d\n", enc, bitrate);
	g_print(" * container for stream is %s\n", container);
	g_print(" * saving confidence images every %" G_GINT64_FORMAT " seconds\n", interval);


	
	bus = gst_element_get_bus(pipeline);
	gst_bus_add_watch(bus, message_handler, NULL);
	gst_object_unref(bus);

	/* link the sink and src pads of the elements to construct the pipeline */
	if (!gst_element_link(v4lsrc, queue)) {
		link_error(v4lsrc, queue);
		return FALSE;
	} else if (!gst_element_link(queue, videorate)) {
		link_error(queue, videorate);
		return FALSE;
	} else if (!gst_element_link(videorate, capsfilter)) {
	  link_error(videorate, capsfilter);
	  return FALSE;
	} else if (!gst_element_link(capsfilter, tee)) {
		link_error(capsfilter, tee);
		return FALSE;
	} else if (!gst_element_link(tee, queue2)) {
	  link_error(tee, queue2);
	  return FALSE;
  } else if (!gst_element_link(queue2, ffmpegcolorspace)) {
    link_error(queue2, ffmpegcolorspace);
    return FALSE;
  } else if (!gst_element_link(ffmpegcolorspace, encode)) {
    link_error(ffmpegcolorspace, encode);
    return FALSE;
	} else if (!gst_element_link(encode, mux)) {
	  link_error(encode, mux);
	  return FALSE;
	} else if (!gst_element_link(mux, filesink)) {
		link_error(mux, filesink);
		return FALSE;
	} else if (!gst_element_link(tee, queue3)) {
	  link_error(tee, queue3);
	  return FALSE;
	} else if (!gst_element_link(queue3, ffmpegcolorspace1)) {
	  link_error(queue3, ffmpegcolorspace1);
	  return FALSE;
	} else if (!gst_element_link(ffmpegcolorspace1, capsfilter1)) {
	  link_error(ffmpegcolorspace1, capsfilter1);
	  return FALSE;
	} else if (!gst_element_link(capsfilter1, jpegenc)) {
	  link_error(capsfilter1, jpegenc);
	  return FALSE;
	} else if (!gst_element_link(jpegenc, queue4)) {
	  link_error(jpegenc, queue4);
	  return FALSE;
	} else if (!gst_element_link(queue4, appsink)) {
	  link_error(queue4, appsink);
	  return FALSE;
	}
	
	g_signal_connect(queue, "overrun", G_CALLBACK (queue_overrun), NULL);
	g_signal_connect(queue2, "overrun", G_CALLBACK (queue_overrun), NULL);
	g_signal_connect(queue3, "overrun", G_CALLBACK (queue_overrun), NULL);
  g_signal_connect(queue4, "overrun", G_CALLBACK (queue_overrun), NULL);
	
	/* setup appsink to catch new-buffer signal and call function new_buffer 
	 * to handle it. */
	g_signal_connect(appsink, "new-buffer", G_CALLBACK (new_buffer), &interval);
	
		/* if provided a duration for the capture */
	if (length > 0)
	{
		GstTimeout *timeout;
		timeout = malloc(sizeof(GstTimeout));
		timeout->pipeline = pipeline;
		timeout->duration = GST_SECOND * length;
		g_timeout_add_seconds(1, (GSourceFunc) is_done, timeout);
		g_print(" * length of capture set to %d seconds\n", length);
	} else {
	  g_print(" * length of capture unset, running until interrupt caught\n");
	}

	/* start the pipeline */
	gst_element_set_state(GST_ELEMENT (pipeline), GST_STATE_PLAYING);
	while (GST_STATE (pipeline) != GST_STATE_PLAYING);
	g_print("\n*Capturing*\nPress Ctrl-C to send EOS\n");
	g_main_loop_run(loop);

	return TRUE;
}
