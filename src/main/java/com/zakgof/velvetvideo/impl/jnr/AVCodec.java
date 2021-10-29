package com.zakgof.velvetvideo.impl.jnr;

import java.util.HashSet;
import java.util.Set;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class AVCodec extends Struct {

    public AVCodec(Runtime runtime) {
        super(runtime);
    }

    /**
     * Name of the codec implementation. The name is globally unique among encoders and among decoders (but an encoder and a decoder can share the same name). This is the primary way to find a codec from the user perspective.
     */

    public Struct.String name = new UTF8StringRef();
    /**
     * Descriptive name for the codec, meant to be more human readable than name.
     */
    public Struct.String long_name = new UTF8StringRef();

    public Signed32 type = new Signed32();
    public Signed32 id = new Signed32();
    Signed32 capabilities = new Signed32();
    StructRef<AVRational> supported_framerates = new StructRef<>(AVRational.class); ///< array of supported framerates, or NULL if any, array is terminated by {0,0}
    public Pointer pix_fmts = new Pointer();
    public Pointer supported_samplerates = new Pointer();
    public Pointer sample_fmts = new Pointer();

    @Override
    public java.lang.String toString() {
    	return name.get() + "/" + long_name.get() + " type=" + type.get();
    }

	public Set<AVSampleFormat> sampleFormats() {
		Set<AVSampleFormat> formats = new HashSet<>();
		for (int offset = 0;;) {
			int val = sample_fmts.get().getInt(offset);
			if (val == -1)
				return formats;
			formats.add(AVSampleFormat.values()[val]);
			offset += 4;
		}
	}
}