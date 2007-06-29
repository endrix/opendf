package net.sf.caltrop.actors;

/** A class for reading, displaying and writing static images.
It is intended for use within CAL actor source.
@author Dave Parlour (dave.parlour@xilinx.com)

Copyright (c) 2007 Xilinx Inc.
All Rights Reserved

Disclaimer:  THIS PROGRAM IS PROVIDED "AS IS" WITH NO WARRANTY 
              WHATSOEVER AND XILINX SPECIFICALLY DISCLAIMS ANY 
              IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
              A PARTICULAR PURPOSE, OR AGAINST INFRINGEMENT.  
*/

import java.util.*;
import ptolemy.media.Picture;
import javax.swing.JFrame;
import java.io.*;
import java.lang.Math;

public class Image
{
	/**
	 * Predefined constant for RGB 4:4:4 image type.
	 * All three component planes are size width x height.
	 */
	public static final int RGB = 0;
	
	/**
	 * Predefined constant for YUV 4:4:4 image type.
	 * All three component planes are size width x height.
	 */
	public static final int YUV    = 1;
	
	/**
	 * Predefined constant for YUV 4:2:2 image type.
	 * The Y component plane is size width x height. The
	 * U and V component planes are size (width / 2) x height.
	 */
	public static final int YUV422 = 2;
	
	/**
	 * Predefined constant for YUV 4:2:0 image type.
	 * The Y component plane is size width x height. The
	 * U and V component planes are size (width / 2) x (height / 2).
	 */
	public static final int YUV420 = 3;
	
	private int type;
	private int width;
	private int height;
	
	// Storage for the three component planes (R,G,B or Y,U,V) stored in scan order
	private List<Integer> buf[];

	// Utilities to get buffer sizes, convert between index, coordinates
	private int bufferSize( int component )
	{
		int sz = width * height;

		if( component < 0 || component > 2 )
			throw new RuntimeException("Bad component index (" + component + ") in Image.bufferSize()");
		if( type == RGB    ) return sz;
		if( type == YUV420 ) return sz / (component == 0 ? 1 : 4);
		if( type == YUV422 ) return sz / (component == 0 ? 1 : 2);
		throw new RuntimeException("Bad image type (" + type + ") in Image.bufferSize()");
	}
	
	/** Create an image with pre-existing arrays of pixel data.
	 * In CAL the component planes should be
	 *  created as linear lists of the appropriate size.
	 * 
	 * @param t image format - see predefined constants
	 * @param w image width in pixels
	 * @param h image height in pixels
	 * @param comp0 first component plane (R or Y)
	 * @param comp1 second component plane (G or U)
	 * @param comp2 third component plane (B or V)
	 * @throws RunTimeException if the component lists are not of the apprpriate size
	 */
	public Image( int t, int w, int h, List comp0, List comp1, List comp2 )
	{
		type = t;
		width = w;
		height = h;
		List pixels[] = { comp0, comp1, comp2 };
		
		for( int i = 0; i<3; i++ )
			if( pixels[i].size() != bufferSize( i ) )
				throw new RuntimeException("Bad pixel plane size (" + pixels[i].size() + ") for image type "
				+ getTypeString() + " " + width + "x" + height );

		buf = pixels;
	}

	/** Create an image from a file with component pixel values stored in planar fashion.
	 * Pixel values are read one byte per pixel.
	 * 
	 * @param t image format - see predefined constants
	 * @param w image width in pixels
	 * @param h image height in pixels
	 * @param fileName file containing planar raw pixel data
	 * @throws RunTimeException if the file cannot be opened or does not contain enough data
	 */
	public Image( int t, int w, int h, String fileName )
	{
		type = t;
		width = w;
		height = h;

		FileInputStream fd;
		
		try
		{
			 fd = new FileInputStream( fileName );
		}
		catch( Exception e )
		{
			throw new RuntimeException( "Unable to open file '" + fileName + "' for reading" );
		}
		
		buf = new ArrayList[3];
		
		for( int i=0; i<3; i++ )
		{
			int sz = bufferSize(i);
			byte b[] = new byte[sz];
			boolean eof;
			
			try
			{
				eof = fd.read( b ) < sz;
			}
			catch( Exception e )
			{
				throw new RuntimeException( "I/O error while reading file '" + fileName + "', component " + i );
			}

			if( eof )
			{
				throw new RuntimeException( "Too few bytes in file '" + fileName 
						+ "' [" + width + "x" + height + " " + getTypeString() + "]" );			
			}
		
			buf[i] = new ArrayList<Integer>( sz );
			for( int j=0; j<sz; j++  ) buf[i].add( new Integer( b[j] & 255 ) );
		}
	}
	
	/** Get the image type as a string.
	 * 
	 * @return image type string
	 * @throws RunTimeException if the type is not recognized
	 */
	public String getTypeString()
	{
		if( type == RGB    ) return "RGB4:4:4";
		if( type == YUV    ) return "YUV4:4:4";
		if( type == YUV420 ) return "YUV4:2:0";
		if( type == YUV422 ) return "YUV4:2:2";
		throw new RuntimeException( "Bad image type (" + type + ")" );
	}

	/** Get the image width
	 * 
	 * @return image width in pixels
	 */
	public int getWidth()  { return width;  }
	
	/** Get the image height
	 * 
	 * @return image height in pixels
	 */
	public int getHeight() { return height; }
	
	/** Get the image type
	 * 
	 * @return image type as a predefined constant
	 */
	public int getType()   { return type;   }
	
	/** get one component plane of the image
	 * 
	 * @param i plane index in the range 0-2
	 * @return a linear list of the plane values
	 */
	public List<Integer> getPixels( int i ) { return buf[i]; };

	private static int rYUV( int py, int pu, int pv  )
	{
		int r = ( 76306 * (py - 16) ) + 32768;
        r = ( r+(104597*(pv-128)))/65536;
        return r < 0 ? 0 : ( r > 255 ? 255 : r );
	}
	
	private static int gYUV( int py, int pu, int pv  )
	{
		int g = ( 76306 * (py - 16) ) + 32768;
        g = ( g-((25675*(pu-128))+(53279*(pv-128))))/ 65536;
        return g < 0 ? 0 : ( g > 255 ? 255 : g );
	}
	
	private static int bYUV( int py, int pu, int pv  )
	{
		int b = ( 76306 * (py - 16) ) + 32768;
        b = ( b+(132201*(pu-128)))/65536;
        return b < 0 ? 0 : ( b > 255 ? 255 : b );
	}
	
	private static void drawYUV( Picture pic, int y, int x, int py, int pu, int pv  )
	{
		pic.setPixel( y, x, rYUV(py,pu,pv), gYUV(py,pu,pv), bYUV(py,pu,pv) );
	}
	
	private static int yRGB( int pr, int pg, int pb )
	{
		int py = ( ( 66*pr + 129 * pg + 25 * pb + 128 ) >> 8 ) + 16;
		
		return py < 0 ? 0 : ( py  > 255 ? 255 : py );
	}
	
	private static int uRGB( int pr, int pg, int pb )
	{
		int pu = ( ( -38 * pr -74 * pg + 112 * pb + 128 ) >> 8 ) + 128;
		
		return pu < 0 ? 0 : ( pu  > 255 ? 255 : pu );
	}
	
	private static int vRGB( int pr, int pg, int pb )
	{
		int pv = ( ( 112 * pr - 94 * pg - 18 * pb + 128 ) >> 8) + 128;
		
		return pv < 0 ? 0 : ( pv  > 255 ? 255 : pv );
	}
	
	/** Display the image in a pop-up window
	 * 
	 */
	public void display( )
	{
		display( "Untitled" );
	}
	
	/** Display the image in a pop-up window with specified title
	 * 
	 * @param title title string to display with the window
	 */
	public void display( String title )
	{
	    Picture pic = new Picture( width, height );
	    JFrame frame = new JFrame( title + " [" + width + "x" + height + " " + getTypeString() + "]" );

	    frame.getContentPane().add( pic );
	    frame.pack();
	    frame.setVisible(true);

	    int x, y;
	    int i = 0;
	    for( y = 0; y < height; y++ )
	      for( x = 0; x < width; x++)
	      {
	    	       if( type == RGB    ) pic.setPixel( y, x, buf[0].get(i), buf[1].get(i), buf[2].get(i) );
	    	  else if( type == YUV    ) drawYUV( pic, y, x, buf[0].get(i), buf[1].get(i), buf[2].get(i) );
	    	  else if( type == YUV420 )
	    	  {
	    		  int ii = x / 2 + ( width / 2 ) * ( y / 2 );
	    		  drawYUV( pic, y, x, buf[0].get(i), buf[1].get(ii), buf[2].get(ii) );
	    	  }
	    	  else if( type == YUV422 )
	    	  {
	    		  int ii = (x / 2) + ( width / 2 ) * y;
	    		  drawYUV( pic, y, x, buf[0].get(i), buf[1].get(ii), buf[2].get(ii) );
	    	  }
	    	       
	    	  i = i + 1;
	      }
	    
	    pic.displayImage();
	    pic.paint( pic.getGraphics() );
	    // frame.paint( frame.getGraphics() );
	}

	/** Save the image to a file.
	 * The three component planes are stored the same planar format read by this class.
	 * Pixel values are saved one byte per pixel.
	 * 
	 * @param fileName file to be written
	 * @throws RunTimeException if the file cannot be opened or written
	 */
	public void save( String fileName )
	{
		FileOutputStream fd;
		
		try
		{
			 fd = new FileOutputStream( fileName );
		}
		catch( Exception e )
		{
			throw new RuntimeException( "Unable to open file '" + fileName + "' for writing" );
		}
		
		try
		{
			for( int i=0; i<3; i++ )
			{
				int sz = bufferSize(i);
				byte[] b = new byte[ sz ];
				for( int j=0; j<sz; j++ ) b[j] = buf[i].get(j).byteValue();
				fd.write( b );
			}
			fd.close();
		}
		catch( Exception e )
		{
			throw new RuntimeException( "Error while writing file '" + fileName );
		}
	}

	private static void putBytes( FileOutputStream fd, int i, int n ) throws IOException
	{
		boolean bigEndian = false;
		
		for( int j=0; j< n; j++ )
			fd.write( (i >> (bigEndian ? (n - j - 1) : j ) * 8 ) & 255 );
	}

	/** Save a copy of the image in BMP format.
	 * If the Image object has one of the YUV types, the pixels
	 * are converted to RGB format for writing.
	 * @param fileName the file to be written
	 * @throws RunTimeException if the file cannot be opened or written
	 */
	public void saveAsBMP( String fileName )
	{
		FileOutputStream fd;
		
		try
		{
			 fd = new FileOutputStream( fileName );
		}
		catch( Exception e )
		{
			throw new RuntimeException( "Unable to open file '" + fileName + "' for writing" );
		}
		
		try
		{
			// BITMAPFILEHEADER - 14 bytes
			fd.write( 'B' );
			fd.write( 'M' );
			putBytes( fd, 54 + 3 * bufferSize(0), 4 );
			putBytes( fd,  0, 4 );
			putBytes( fd, 54, 4 );
			
			// BITMAPINFOHEADER - 40 bytes
			putBytes( fd, 40, 4 );            // biSize
			putBytes( fd, width, 4 );
			putBytes( fd, height, 4 );
			putBytes( fd, 1, 2 );             // biPlanes
			putBytes( fd, 24, 2 );            // biBitCount
			putBytes( fd, 0, 4 );             // biCompression
			putBytes( fd, 0, 4 );             // biSizeImage
			putBytes( fd, 0, 4 );             // biXPelsPerMeter
			putBytes( fd, 0, 4 );             // biYPelsPerMeter
			putBytes( fd, 0, 4 );             // biClrUsed
			putBytes( fd, 0, 4 );             // biClrImportant
			
		    for( int y = height - 1; y >= 0 ; y-- )
		    {
		    	for( int x = 0; x < width; x++)
		    	{
		    		int i = x + y * width;
		    		int r, g, b;
		    		if( type == RGB )
		    		{
		    			r = buf[0].get(i);
		    			g = buf[1].get(i);
		    			b = buf[2].get(i);
		    		}
		    		else
		    		{
		    			int py = buf[0].get(i);
		    			int pu = 0, pv = 0;
		    			if( type == YUV  )
		    			{
		    				pu = buf[1].get(i);
		    				pv = buf[2].get(i);
		    			}
		    			else if( type == YUV420 )
		    			{
		    				int ii = x / 2 + ( width / 2 ) * ( y / 2 );
		    				pu = buf[1].get(ii);
		    				pv = buf[2].get(ii);
		    			}
		    			else if( type == YUV422 )
		    			{
		    				int ii = (x / 2) + ( width / 2 ) * y;
		    				pu = buf[1].get(ii);
		    				pv = buf[2].get(ii);
		    			}
		    		  
		    			r = rYUV( py, pu, pv );
		    			g = gYUV( py, pu, pv );
		    			b = bYUV( py, pu, pv );
		    		}
		    		fd.write( b );
		    		fd.write( g );
		    		fd.write( r );
		    	}
		    	
		    	// Each row in file must be a multiple of 4 bytes
		    	for( int i = width * 3; i % 4 != 0;  i++ )
		    		fd.write( 0 );
		   }
		}
		catch( Exception e )
		{
			throw new RuntimeException( "Error while writing file '" + fileName );
		}
	}
	
	/** Measure the PSNR of the difference between one plane of this image and another.
	 * PSNR is computed as 10 x log10( 255**2 * number of pixels / sum of squared differences )
	 * @param other the other image to difference with
	 * @param plane the index of the component plane in the range 0-2
	 * @return PSNR in dB to two decimal places.
	 * @throws RunTimeException if the two images do not have the same type or dimensions.
	 */
	public double PSNR( Image other, int plane )
	{
		double floor = 1.0e-20;
		
		if( type != other.getType() || width != other.getWidth() || height != other.getHeight() )
			throw new RuntimeException( "Cannot compute PSNR for incompatible images" );
		
		double ssd   = 0.0;
		List<Integer> thisBuf = buf[ plane ];
		List<Integer> otherBuf = other.getPixels( plane );
		int sz = bufferSize( plane );
		
		for( int i = 0; i < sz; i++ )
		{
			int thisPixel = thisBuf.get( i ).intValue();
			int diff = thisPixel - otherBuf.get( i ).intValue();
			ssd = ssd + diff * diff;
		}

		if( ssd < floor ) ssd = floor;
		double psnr = 10.0 * Math.log10( 255.0 * 255.0 * sz / ssd );
		
		return Math.floor( psnr * 100.0 + 0.5 ) / 100.0;
	}

	/** Create an Image object that is the YUV4:2:2 representation of an RGB4:4:4 Image.
	 * @return a new object with the YUV4:2:2 representation of the source image
	 * @throws RunTimeException if rgb444 is not of type RGB
	 */
	public Image YUV422fromRGB444( )
	{
		int offset = 6;
		
		if( type != RGB )
			throw new RuntimeException( "Did not get expected input image type 'RGB' in YUV422fromRGB444()");
		
		double coeff[] =
		{ 0.0032609706416012,  0.0076236834211388, -0.0223492954179013, -0.0542960830726165,
          0.1257320943643338,  0.4400286300634440,  0.4400286300634440,  0.1257320943643338,
         -0.0542960830726165, -0.0223492954179013,  0.0076236834211388
        };

		List<Integer> y    = new ArrayList<Integer>( bufferSize( 0 ) );
		List<Integer> u444 = new ArrayList<Integer>( bufferSize( 0 ) );
		List<Integer> v444 = new ArrayList<Integer>( bufferSize( 0 ) );
		List<Integer> u422 = new ArrayList<Integer>( bufferSize( 0 ) / 2 );
		List<Integer> v422 = new ArrayList<Integer>( bufferSize( 0 ) / 2 );

		for( int i = 0; i < bufferSize( 0 ); i++ )
		{
			y.add   ( new Integer( yRGB( buf[0].get(i).intValue(), buf[1].get(i).intValue(), buf[2].get(i).intValue() ) ) );
			u444.add( new Integer( uRGB( buf[0].get(i).intValue(), buf[1].get(i).intValue(), buf[2].get(i).intValue() ) ) );
			v444.add( new Integer( vRGB( buf[0].get(i).intValue(), buf[1].get(i).intValue(), buf[2].get(i).intValue() ) ) );
		}
		
		for( int i = 0; i < bufferSize( 0 ) / 2; i++ )
		{
			double uu = 0.5;
			double vv = 0.5;
			
			for( int n = 0; n < coeff.length; n++ )
			{
				int j = 2*i + n + (offset - coeff.length + 1);
				j = j < 0 ? 0 : ( j >= bufferSize( 0 ) ? bufferSize( 0 ) - 1 : j );
				
				uu += coeff[n] * u444.get( j ).intValue();
				vv += coeff[n] * v444.get( j ).intValue();
			}
			
			int pu = (int) uu;
			pu = pu < 0 ? 0 : ( pu > 255 ? 255 : pu );
			int pv = (int) vv;
			pv = pv < 0 ? 0 : ( pv > 255 ? 255 : pv );
			
			u422.add( new Integer( pu ) );
			v422.add( new Integer( pv ) );
		}
		
		return new Image( YUV422, width, height, y, u422, v422 );
	}
}