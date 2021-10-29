package com.zakgof.velvetvideo.test;

import java.awt.image.BufferedImage;
import java.io.File;

import com.zakgof.velvetvideo.IDemuxer;
import com.zakgof.velvetvideo.IVelvetVideoLib;
import com.zakgof.velvetvideo.IVideoDecoderStream;
import com.zakgof.velvetvideo.IVideoFrame;
import com.zakgof.velvetvideo.impl.VelvetVideoLib;

public class Test {

	public static void main(String[] args) {
		
		IVelvetVideoLib lib = VelvetVideoLib.getInstance();
		String filePath = "D:/KBB/convert_test/New project.mp4";
		
		BufferedImage image = null;
		
		try (IDemuxer demuxer = lib.demuxer(new File(filePath))) {
			IVideoDecoderStream videoStream = demuxer.videoStream(0);
		    IVideoFrame videoFrame;
		    while ((videoFrame = videoStream.nextFrame()) != null) {
		   	    image = videoFrame.image();
			   	break;
		    }
		}  
		
		System.out.println(image.getHeight());
		
	}
	
}
