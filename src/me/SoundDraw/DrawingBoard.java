package me.SoundDraw;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class DrawingBoard extends JPanel {

	private static final int STEP = 100;
	private static final int DEFAULT_SIZE = STEP * 5;

	private final MouseListener listener;
	private byte[] data = new byte[DEFAULT_SIZE];
	private int lastEditedSize = DEFAULT_SIZE;

	private boolean down = false;
	private Point lastPos;

	public DrawingBoard() {
		listener = new MouseListener();
		setBackground(Color.black);
		addMouseListener(listener);
		addMouseMotionListener(listener);
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				repaint();
			}
		});
	}

	public byte getSampleAt(int idx) {
		return data[idx % data.length];
	}

	public int resizeData(int mul) {
		int size = mul * STEP;

		if (data.length == size)
			return -1;

		int oldSize = data.length;
		data = Arrays.copyOf(data, size);

		if (oldSize < size) {
			for (int i = lastEditedSize; i + lastEditedSize <= size; i += lastEditedSize) {
				System.arraycopy(data, 0, data, i, lastEditedSize);
			}
		} else if (size < lastEditedSize) {
			lastEditedSize = size;
		}

		SwingUtilities.invokeLater(this::repaint);

		return size;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		int width = getWidth();
		int height = getHeight();

		g.setColor(Color.darkGray);
		g.drawLine(0, height / 2, width, height / 2);

		g.setColor(Color.white);
		for (int i = 0; i < data.length - 1; i++) {
			g.drawLine(i * width / data.length, (data[i] + 128) * height / 256, (i + 1) * width / data.length,
					(data[i + 1] + 128) * height / 256);
		}
	}

	private class MouseListener extends MouseAdapter {
		@Override
		public void mousePressed(MouseEvent e) {
			down = true;
			lastPos = e.getPoint();
			firePaint(e.getPoint());
			e.consume();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			firePaint(e.getPoint());
			down = false;
			lastPos = null;
			e.consume();
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			firePaint(e.getPoint());
			e.consume();
		}

		private void firePaint(Point pos) {
			if (!down)
				return;
			lastEditedSize = data.length;

			int width = getWidth();
			int height = getHeight();
			int stepHalfX = width / data.length / 2;
			int stepHalfY = height / 256 / 2;

			if (pos.y < 0)
				pos.y = 0;
			else if (pos.y >= height)
				pos.y = height - 1;

			if (lastPos == null)
				lastPos = pos;

			try {
				Point pLow = pos.x < lastPos.x ? pos : lastPos;
				Point pHigh = pos.x < lastPos.x ? lastPos : pos;

				int lastIdx = (pLow.x + stepHalfX) * data.length / width;
				int newIdx = (pHigh.x + stepHalfX) * data.length / width;
				int lastVal = pLow.y;
				int newVal = pHigh.y;
				int diffIdx = newIdx - lastIdx + 1;
				float diff = newVal - lastVal;

				for (int curIdx = lastIdx, off = 0; (curIdx = lastIdx + off) <= newIdx; off++) {
					if (curIdx < 0 || curIdx >= data.length)
						continue;
					int val = Math.round(lastVal + diff * off / diffIdx);
					data[curIdx] = (byte) (((val * 256 + stepHalfY) / height) - 128);
					curIdx++;
				}

				repaint();
			} finally {
				lastPos = pos;
			}
		}
	}

}
