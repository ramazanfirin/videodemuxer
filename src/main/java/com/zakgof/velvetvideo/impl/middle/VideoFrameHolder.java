package com.zakgof.velvetvideo.impl.middle;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

import com.zakgof.velvetvideo.IVideoDecoderStream;
import com.zakgof.velvetvideo.IVideoFrame;
import com.zakgof.velvetvideo.VelvetVideoException;
import com.zakgof.velvetvideo.impl.JNRHelper;
import com.zakgof.velvetvideo.impl.VelvetVideoLib.DemuxerImpl;
import com.zakgof.velvetvideo.impl.jnr.AVFrame;
import com.zakgof.velvetvideo.impl.jnr.AVPixelFormat;
import com.zakgof.velvetvideo.impl.jnr.AVRational;
import com.zakgof.velvetvideo.impl.jnr.LibAVUtil;
import com.zakgof.velvetvideo.impl.jnr.LibSwScale;
import com.zakgof.velvetvideo.impl.jnr.SwsContext;

import jnr.ffi.Pointer;
import jnr.ffi.Struct;

public class VideoFrameHolder implements AutoCloseable, IFrameHolder {

	public final AVFrame frame;
	private final AVFrame biframe;
	private final SwsContext scaleCtx;
	private final int width;
	private final int height;
	private AVRational timebase;
	private static final LibSwScale libswscale = JNRHelper.load(LibSwScale.class, "swscale",5);
	private static final LibAVUtil libavutil = JNRHelper.load(LibAVUtil.class, "avutil", 56);

	public VideoFrameHolder(int width, int height, AVPixelFormat srcFormat, AVPixelFormat destFormat,
			AVRational timebase, boolean encode) {
		this.width = width;
		this.height = height;
		this.frame = alloc(width, height, encode ? destFormat : srcFormat);
		this.biframe = alloc(width, height, encode ? srcFormat : destFormat);
		this.timebase = timebase;
		scaleCtx = libswscale.sws_getContext(width, height, srcFormat, width, height, destFormat, 0, null, null, null);
	}

	public AVFrame alloc(int width, int height, AVPixelFormat format) {
		AVFrame f = libavutil.av_frame_alloc();
		f.width.set(width);
		f.height.set(height);
		f.format.set(format.ordinal());
		libavutil.checkcode(libavutil.av_frame_get_buffer(f, 0));
		return f;
	}

	public AVFrame setPixels(BufferedImage image) {
		byte[] bytes = bytesOf(image);
		biframe.data[0].get().put(0, bytes, 0, bytes.length);
		libavutil.checkcode(
				libswscale.sws_scale(scaleCtx, JNRHelper.ptr(biframe.data[0]), JNRHelper.ptr(biframe.linesize[0]), 0,
						height, JNRHelper.ptr(frame.data[0]), JNRHelper.ptr(frame.linesize[0])));
		return frame;
	}

	public BufferedImage getPixels(AVFrame f) {
		libavutil.checkcode(libswscale.sws_scale(scaleCtx, JNRHelper.ptr(f.data[0]), JNRHelper.ptr(f.linesize[0]), 0,
				height, JNRHelper.ptr(biframe.data[0]), JNRHelper.ptr(biframe.linesize[0])));
		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		byte[] bytes = bytesOf(bi);
		biframe.data[0].get().get(0, bytes, 0, bytes.length);
		return bi;
	}

	private static byte[] bytesOf(BufferedImage image) {
		if (image.getType() != BufferedImage.TYPE_3BYTE_BGR) {
			BufferedImage newimage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
			Graphics2D g = newimage.createGraphics();
			g.drawImage(image, 0, 0, null);
			g.dispose();
			image = newimage;
		}
		Raster raster = image.getRaster();
		DataBuffer buffer = raster.getDataBuffer();
		if (buffer instanceof DataBufferByte) {
			return ((DataBufferByte) buffer).getData();
		}
		throw new VelvetVideoException("Unsupported image data buffer type");
	}

	@Override
	public IVideoFrame decode(AVFrame frame, DemuxerImpl.AbstractDecoderStream stream) {
		BufferedImage bi = getPixels(frame);
		long pts = pts();
		if (pts == LibAVUtil.AVNOPTS_VALUE) {
			pts = 0;
		}
		long nanostamp = pts * 1000000000L * timebase.num.get() / timebase.den.get();
		long duration = libavutil.av_frame_get_pkt_duration(frame);
		long nanoduration = duration * 1000000000L * timebase.num.get() / timebase.den.get();
		return new VideoFrameImpl(bi, nanostamp, nanoduration, (IVideoDecoderStream) stream);
	}

	@Override
	public AVFrame frame() {
		// TODO DRY - abstract class
		return frame;
	}

	@Override
	public void close() {
		libavutil.av_frame_free(new Pointer[] { Struct.getMemory(frame) });
		libavutil.av_frame_free(new Pointer[] { Struct.getMemory(biframe) });
		libswscale.sws_freeContext(scaleCtx);
	}

}