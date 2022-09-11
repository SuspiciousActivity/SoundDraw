package me.SoundDraw;

@FunctionalInterface
public interface IntToByteFunction {
	byte apply(int value);
}