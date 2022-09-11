package me.SoundDraw;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.IntSupplier;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class AudioPlayer extends Thread {

	private final IntToByteFunction func;
	private final IntSupplier volume;

	private final AudioFormat format;
	private final SourceDataLine sourceDataLine;

	public AudioPlayer(IntToByteFunction func, IntSupplier volume) throws LineUnavailableException {
		this.func = func;
		this.volume = volume;

		format = new AudioFormat(Main.SAMPLE_RATE, 8, 1, true, false);
		DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
		sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
	}

	@Override
	public void run() {
		try {
			sourceDataLine.open(format);
		} catch (LineUnavailableException e) {
			throw new RuntimeException(e);
		}
		sourceDataLine.start();

		try (AudioGen gen = new AudioGen()) {
			byte[] data = new byte[4096];
			int read = 0;

			while ((read = gen.read(data)) > 0) {
				sourceDataLine.write(data, 0, read);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			sourceDataLine.drain();
			sourceDataLine.close();
		}
	}

	private class AudioGen extends InputStream {

		private int index;

		@Override
		public int read() throws IOException {
			return (int) (func.apply(index++) * volume.getAsInt() / 100);
		}

		@Override
		public int read(byte b[], int off, int len) throws IOException {
			if (b == null) {
				throw new NullPointerException();
			} else if (off < 0 || len < 0 || len > b.length - off) {
				throw new IndexOutOfBoundsException();
			} else if (len == 0) {
				return 0;
			}

			int c = read();
			b[off] = (byte) c;

			int i = 1;
			try {
				for (; i < len; i++) {
					c = read();
					b[off + i] = (byte) c;
				}
			} catch (IOException ee) {
			}
			return i;
		}

	}

}
