CC=gcc
LIBS=`pkg-config gstreamer-0.10 gtk+-2.0 --libs`
CFLAGS=`pkg-config gstreamer-0.10 gtk+-2.0 --cflags`

all: h_wo_c  h_with_c vga_with_c

h_wo_c: h_wo_c.c
	$(CC) -Wall $(LIBS) $(CFLAGS) h_wo_c.c -o h_wo_c
  
 
h_with_c: h_with_c.c
	$(CC) -Wall $(LIBS) $(CFLAGS) -lgstapp-0.10 h_with_c.c -o h_with_c

vga_with_c: vga_with_c.c
	$(CC) -Wall $(LIBS) $(CFLAGS) -lgstapp-0.10 vga_with_c.c -o vga_with_c
  
clean:
	rm -f h_wo_c h_with_c vga_with_c
