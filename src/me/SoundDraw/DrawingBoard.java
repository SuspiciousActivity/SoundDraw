package me.SoundDraw;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Arrays;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class DrawingBoard extends JPanel {

	private static final int STEP = 100;
	private static final int DEFAULT_SIZE = STEP * 5;

	private byte[] data = new byte[DEFAULT_SIZE];
	private int lastEditedSize = DEFAULT_SIZE;

	private EnumMouseDrag mouseDrag = EnumMouseDrag.NONE;
	private Point lastPos;

	private int centerOff = 0;
	private int movingCenterOff = 0;
	private int zoom = 1;

	public DrawingBoard() {
		setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		setBackground(Color.black);
		MouseListener listener = new MouseListener();
		addMouseListener(listener);
		addMouseMotionListener(listener);
		addMouseWheelListener(listener);
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

		int start = 0;

		int length = data.length / zoom;
		if (zoom != 1) {
			int mid = data.length / 2 + getCenterOffset();
			start = mid - length / 2;
		}

		g.setColor(Color.white);
		for (int i = 0; i < length - 1; i++) {
			g.drawLine(i * width / length, (data[start + i] + 128) * height / 256, (i + 1) * width / length,
					(data[start + i + 1] + 128) * height / 256);
		}
	}

	private int getCenterOffset() {
		return centerOff + movingCenterOff;
	}

	private enum EnumMouseDrag {
		NONE, DRAW, MOVE;
	}

	private class MouseListener extends MouseAdapter {
		@Override
		public void mousePressed(MouseEvent e) {
			switch (e.getButton()) {
			case MouseEvent.BUTTON1:
				mouseDrag = EnumMouseDrag.DRAW;
				lastPos = e.getPoint();
				firePaint(e.getPoint());
				e.consume();
				break;
			case MouseEvent.BUTTON3:
				mouseDrag = EnumMouseDrag.MOVE;
				lastPos = e.getPoint();
				e.consume();
				break;
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			switch (e.getButton()) {
			case MouseEvent.BUTTON1:
				firePaint(e.getPoint());
				mouseDrag = EnumMouseDrag.NONE;
				lastPos = null;
				e.consume();
				break;
			case MouseEvent.BUTTON3:
				fireMove(e.getPoint());
				mouseDrag = EnumMouseDrag.NONE;
				lastPos = null;
				centerOff += movingCenterOff;
				movingCenterOff = 0;
				e.consume();
				break;
			}
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			switch (mouseDrag) {
			case DRAW:
				firePaint(e.getPoint());
				e.consume();
				break;
			case MOVE:
				fireMove(e.getPoint());
				e.consume();
				break;
			default:
				break;
			}
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if (mouseDrag != EnumMouseDrag.NONE)
				return;
			fireZoom(e.getPoint(), (int) Math.signum(e.getPreciseWheelRotation()));
		}

		private void firePaint(Point pos) {
			if (mouseDrag != EnumMouseDrag.DRAW)
				return;
			lastEditedSize = data.length;

			int width = getWidth();
			int height = getHeight();
			float stepHalfX = (float) width / data.length * zoom / 2;
			float stepHalfY = (float) height / 256 / 2;

			if (pos.y < 0)
				pos.y = 0;
			else if (pos.y >= height)
				pos.y = height - 1;

			if (lastPos == null)
				lastPos = pos;

			try {
				Point pLow = pos.x < lastPos.x ? pos : lastPos;
				Point pHigh = pos.x < lastPos.x ? lastPos : pos;

				int start = 0;

				int length = data.length / zoom;
				if (zoom != 1) {
					int mid = data.length / 2 + getCenterOffset();
					start = mid - length / 2;
				}

				int lastIdx = (int) ((pLow.x + stepHalfX) * data.length / zoom / width + start);
				int newIdx = (int) ((pHigh.x + stepHalfX) * data.length / zoom / width + start);
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

		private void fireMove(Point pos) {
			if (mouseDrag != EnumMouseDrag.MOVE)
				return;

			float width = getWidth();

			int newOff = (int) ((float) (lastPos.x - pos.x) / zoom / (width / data.length));

			float half = data.length / 2f;
			int lowerBound = (int) Math.ceil(half / zoom - half - centerOff);
			int higherBound = (int) Math.floor(data.length - half / zoom - half - centerOff);

			if (newOff < lowerBound) {
				newOff = lowerBound;
			} else if (newOff > higherBound) {
				newOff = higherBound;
			}

			movingCenterOff = newOff;

			repaint();
		}

		private void fireZoom(Point pos, int wheel) {
			float width = getWidth();

			int oldZoom = zoom;

			if (wheel < 0) {
				zoom = Math.min(zoom + 1, 32);
			} else if (wheel > 0) {
				zoom = Math.max(zoom - 1, 1);
			}

			if (oldZoom == zoom)
				return;

			int pseudoMoveOff = (int) ((float) (pos.x - width / 2) / oldZoom / zoom / (width / data.length));
			centerOff += wheel < 0 ? pseudoMoveOff : -pseudoMoveOff;

			float half = data.length / 2f;
			int lowerBound = (int) Math.ceil(half / zoom - half - centerOff);
			int higherBound = (int) Math.floor(data.length - half / zoom - half - centerOff);

			if (lowerBound > 0) {
				centerOff += lowerBound;
			} else if (higherBound < 0) {
				centerOff += higherBound;
			}

			repaint();
		}
	}

}
