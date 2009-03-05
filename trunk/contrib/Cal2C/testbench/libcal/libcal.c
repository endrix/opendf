#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include "SDL.h"

static SDL_Surface *m_screen;
static int m_width;
static int m_height;
static int m_x;
static int m_y;

int currentSystemTime(void) {
	return 0;
}

int openFile(char *file_name) {
	FILE *F;

	if (!strcmp(file_name, "dummy")) {
		return 0;
	}

	F = fopen(file_name, "rb");
	if (!F) {
		printf("Warning: file not found: %s\n", file_name);
	}
	return (int)F;
}

static Uint32 t;
static int nb_images_start;
static int nb_images_end;
static int nb_mb_start;
static int nb_mb_end;

void picture_displayImage(void) {
	int t2;

	if (t == 0) {
		t = SDL_GetTicks();
	}

	nb_mb_end++;

	if (m_x == m_width - 1 && m_y == m_height - 1) {
		nb_images_end++;
		SDL_Flip(m_screen);

		t2 = SDL_GetTicks();
		if (t2 - t > 1000) {
			printf("%f images/sec\n", 1000.0f * (float)(nb_images_end - nb_images_start) / (float)(t2 - t));
			printf("%f mb/sec\n", 1000.0f * (float)(nb_mb_end - nb_mb_start) / (float)(t2 - t));
			t = t2;
			nb_images_start = nb_images_end;
			nb_mb_start = nb_mb_end;
		}
	}
}

/**
  <=> setPixel(int row, int col, int red, int green, int blue)
  @see ptolemy.media.Picture
*/
void picture_setPixel(int y, int x, int r, int g, int b) {
	SDL_Surface *surface = m_screen;
	SDL_Event event;
	int bpp;
	Uint8 *p;

	m_x = x;
	m_y = y;

	// Clip values
	if (r > 255) {
		r = 255;
	}
	if (g > 255) {
		g = 255;
	}
	if (b > 255) {
		b = 255;
	}
	if (r < 0) {
		r = 0;
	}
	if (g < 0) {
		g = 0;
	}
	if (b < 0) {
		b = 0;
	}

	if ( SDL_MUSTLOCK(surface) ) {
		if ( SDL_LockSurface(surface) < 0 ) {
			fprintf(stderr, "Can't lock screen: %s\n", SDL_GetError());
			return;
		}
	}
	bpp = surface->format->BytesPerPixel;
	/* Here p is the address to the pixel we want to set */
	p = (Uint8 *)surface->pixels + y * surface->pitch + x * bpp;
	if(SDL_BYTEORDER == SDL_BIG_ENDIAN) {
		p[2] = b & 0xff;
		p[1] = g & 0xff;
		p[0] = r & 0xff;
	} else {
		p[0] = b & 0xff;
		p[1] = g & 0xff;
		p[2] = r & 0xff;
	}

	if ( SDL_MUSTLOCK(surface) ) {
		SDL_UnlockSurface(surface);
	}

    /* Grab all the events off the queue. */
    while( SDL_PollEvent( &event ) ) {
        switch( event.type ) {
        case SDL_QUIT:
			exit(0);
            break;
		default:
			break;
        }

    }
}

int readByte(int fd) {
	unsigned char buf[1];
	FILE *F = (FILE *)fd;
	if (F) {
		if (fread(buf, 1, 1, F) < 1) {
			exit(-1);
		}
		return buf[0];
	} else {
		return -1;
	}
}

int JFrame(char *title ) {
	SDL_WM_SetCaption(title, NULL);
	return 0;
}

int Picture(int width, int height) {
	m_width = width;
	m_height = height;
	m_screen = SDL_SetVideoMode(m_width, m_height, 24, SDL_HWSURFACE | SDL_DOUBLEBUF);
	if ( m_screen == NULL ) {
		fprintf(stderr, "Couldn't set %ix%ix24 video mode: %s\n", width, height,
			SDL_GetError());
		exit(1);
	}
	return 0;
}

static FILE *traces;

void libcal_exit(void) {
	int t2;

	fclose(traces);
	traces = NULL;

	t2 = SDL_GetTicks();
	printf("%f images/sec\n", 1000.0f * (float)nb_images_end / (float)(t2 - t));
	printf("%f mb/sec\n", 1000.0f * (float)nb_mb_end / (float)(t2 - t));

	/* Clean up on exit */
    SDL_Quit();
}

void libcal_init() {
	traces = fopen("traces.txt", "w");

	/* First, initialize SDL's video subsystem. */
    if( SDL_Init( SDL_INIT_VIDEO ) < 0 ) {
        /* Failed, exit. */
        fprintf( stderr, "Video initialization failed: %s\n",
			SDL_GetError( ) );
    }

	atexit(libcal_exit);
}

void libcal_printf(const char * format, ...) {
	va_list ap;
	va_start(ap, format);
	vfprintf(traces, format, ap);
	fflush(traces);
	va_end(ap);
}
