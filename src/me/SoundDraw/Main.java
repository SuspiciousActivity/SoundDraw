package me.SoundDraw;

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.util.Locale;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.JLabel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import java.awt.FlowLayout;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.Color;
import javax.swing.SpringLayout;
import javax.swing.BoxLayout;
import java.awt.Component;
import javax.swing.Box;

public class Main {

	public static final int SAMPLE_RATE = 44100;

	private JFrame frmAudiodraw;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Main window = new Main();
					window.frmAudiodraw.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 * 
	 * @throws LineUnavailableException
	 */
	public Main() throws LineUnavailableException {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 * 
	 * @throws LineUnavailableException
	 */
	private void initialize() throws LineUnavailableException {
		frmAudiodraw = new JFrame();
		frmAudiodraw.getContentPane().setBackground(Color.DARK_GRAY);
		frmAudiodraw.setTitle("Sound Draw");
		frmAudiodraw.setBounds((int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2 - 250),
				(int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2 - 150), 500, 300);
		frmAudiodraw.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		SpringLayout springLayout = new SpringLayout();
		frmAudiodraw.getContentPane().setLayout(springLayout);

		DrawingBoard board = new DrawingBoard();
		springLayout.putConstraint(SpringLayout.WEST, board, 10, SpringLayout.WEST, frmAudiodraw.getContentPane());
		springLayout.putConstraint(SpringLayout.SOUTH, board, -10, SpringLayout.SOUTH, frmAudiodraw.getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, board, -10, SpringLayout.EAST, frmAudiodraw.getContentPane());
		frmAudiodraw.getContentPane().add(board);

		JPanel controls = new JPanel();
		controls.setBackground(Color.DARK_GRAY);
		springLayout.putConstraint(SpringLayout.NORTH, controls, 10, SpringLayout.NORTH, frmAudiodraw.getContentPane());
		springLayout.putConstraint(SpringLayout.WEST, controls, 10, SpringLayout.WEST, frmAudiodraw.getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, controls, -10, SpringLayout.EAST, frmAudiodraw.getContentPane());
		springLayout.putConstraint(SpringLayout.NORTH, board, 10, SpringLayout.SOUTH, controls);
		frmAudiodraw.getContentPane().add(controls);

		JPanel panelVolume = new JPanel();
		panelVolume.setBackground(Color.DARK_GRAY);
		panelVolume.setLayout(new BoxLayout(panelVolume, BoxLayout.X_AXIS));

		JLabel lblVolume = new JLabel("Volume");
		lblVolume.setForeground(Color.WHITE);
		panelVolume.add(lblVolume);

		JSlider sliderVolume = new JSlider();
		sliderVolume.setBackground(Color.DARK_GRAY);
		panelVolume.add(sliderVolume);
		sliderVolume.setFocusable(false);
		sliderVolume.setValue(10);

		JPanel panelLength = new JPanel();
		panelLength.setBackground(Color.DARK_GRAY);
		panelLength.setLayout(new BoxLayout(panelLength, BoxLayout.X_AXIS));

		JLabel lblLength = new JLabel("Length (0.05s)");
		lblLength.setForeground(Color.WHITE);
		panelLength.add(lblLength);

		JSlider sliderLength = new JSlider();
		sliderLength.setMaximum(SAMPLE_RATE / 100 * 2);
		sliderLength.setBackground(Color.DARK_GRAY);
		sliderLength.setSnapToTicks(true);
		sliderLength.setValue(22);
		sliderLength.setMinimum(22);
		panelLength.add(sliderLength);
		sliderLength.setFocusable(false);
		sliderLength.addChangeListener(e -> {
			int size = board.resizeData(sliderLength.getValue());
			if (size != -1) {
				lblLength
						.setText("Length (" + String.format(Locale.ENGLISH, "%.2fs", size / (float) SAMPLE_RATE) + ")");
			}
		});
		controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
		controls.add(panelVolume);

		Component spacer = Box.createHorizontalStrut(20);
		controls.add(spacer);
		controls.add(panelLength);

		AudioPlayer player = new AudioPlayer(board::getSampleAt, sliderVolume::getValue);
		player.start();
	}
}
