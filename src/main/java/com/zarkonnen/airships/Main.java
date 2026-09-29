package com.zarkonnen.airships;

import com.zarkonnen.catengine.Condition;
import com.zarkonnen.catengine.ExceptionHandler;
import com.zarkonnen.catengine.lwjgl3.Lwjgl3Engine;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Locale;
import javax.swing.JOptionPane;
import org.joda.time.DateTime;

public strictfp class Main {
	public static boolean ON_STEAM_DECK = false;
	
	public static void main(String[] args) {
		if (com.zarkonnen.airships.SteamBackend.switchedOn && !System.getProperty("deck", "true").equals("false")) {
			com.zarkonnen.airships.SteamBackend.tick();
			ON_STEAM_DECK = com.zarkonnen.airships.SteamBackend.utils.isSteamRunningOnSteamDeck();
		}
		if (ON_STEAM_DECK) {
			System.setProperty("LWJGL_DISABLE_XRANDR", "true");
		}
		if (System.getProperty("os.name", "NONE").toLowerCase(Locale.ENGLISH).contains("win")) {
			System.setProperty("org.lwjgl.librarypath", AGame.getStaticGameDirectory().getAbsolutePath() + "\\lib\\native\\");
		}
		if (!SimplePref.WINDOW_BORDER.get() || (LaunchSettings.useCustomWindow && LaunchSettings.customWindowBorderless)) {
			System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
		}
		try {
			Errstream.install();
			System.err.println("");
			System.err.println("Booting v " + AGame.VERSION + " @ " + new Date());
			System.err.println("OS: " + System.getProperty("os.name", "NONE"));
			System.err.println("static: " + AGame.getStaticGameDirectory().getAbsolutePath() + " exists " + AGame.getStaticGameDirectory().exists());
			System.err.println("gamedir: " + AGame.getGameDirectory().getAbsolutePath() + " exists " + AGame.getGameDirectory().exists());
			if (!ON_STEAM_DECK && !System.getProperty("os.name").contains("Mac") && !System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows")) {
				// Should be on Linux. Check if xrandr is available.
				try {
					Runtime.getRuntime().exec(new String[] { "xrandr", "-q" });
				} catch (Exception e) {
					System.err.println("Assuming we're on Linux but xrandr is not available. Disabling xrandr.");
					System.setProperty("LWJGL_DISABLE_XRANDR", "true");
				}
			}
			Lwjgl3Engine e = new Lwjgl3Engine("Airships", "/com/zarkonnen/airships/images/", "/com/zarkonnen/airships/sounds/", LaunchSettings.targetFPS);
			final AirshipGame g = new AirshipGame();
			e.setExceptionHandler(new ExceptionHandler() {
				@Override
				public void handle(Exception e, boolean fatal) {
					g.reportError("OpenGL Crash", e, null, false, true);
					JOptionPane.showMessageDialog(null, "Unable to create OpenGL window.\nPlease check that your drivers are up to date and that your graphics card supports OpenGL.");
					System.exit(1);
				}
			});
			try {
				e.setup(g);
			} catch (ExceptionInInitializerError eiie) {
				if (eiie.getCause() instanceof ArrayIndexOutOfBoundsException) {
					JOptionPane.showMessageDialog(null, "Unable to find screen resolution information. Please contact the developer.");
					System.exit(1);
				} else {
					throw eiie;
				}
			}
			e.setExceptionHandler(g);
			e.reportHandler = g;
			System.err.println(new DateTime());
			// 窗口图标（GLFW 下可选，暂略）
			//File imagesDir = new File(new File(AGame.getStaticGameDirectory(), "data"), "images");
			//Display.setIcon(...);
			e.setRunInBackground(true);
			try {
				e.runUntil(Condition.ALWAYS);
			} catch (IllegalStateException ise) {
				int id = AGame.ANIM_R.nextInt();
				ise.printStackTrace();
				StringWriter sw = new StringWriter();
				ise.printStackTrace(new PrintWriter(sw));
				reportError("IllegalStateException " + id + " " + sw + "\n" + pathReport());
				try { Thread.sleep(1000); } catch (Exception x) {}
				e.runUntil(Condition.ALWAYS);
			}
		} catch (Throwable t) {
			t.printStackTrace();
			StringWriter sw = new StringWriter();
			t.printStackTrace(new PrintWriter(sw));
			reportError(sw.toString() + "\n" + pathReport());
			try { Thread.sleep(5000); } catch (Exception e) {}
			JOptionPane.showMessageDialog(null, "Fatal error. Please report this to the developer.\n" + t.toString() + "\n" + sw.toString());
		}
	}
	
	public static String pathReport() {
		StringBuilder sb = new StringBuilder();
		sb.append("os.name = ").append(System.getProperty("os.name", "NONE")).append("\n");
		sb.append("java.version = ").append(System.getProperty("java.version", "NONE")).append("\n");
		sb.append("java.library.path = ").append(System.getProperty("java.library.path", "NONE")).append("\n");
		sb.append(". = ").append(new File("").getAbsolutePath()).append("\n");
		sb.append("new home path = ").append(AGame.getStaticGameDirectory().getAbsolutePath()).append("\n");
		if (System.getProperty("java.library.path") != null) {
			File f = new File("").getAbsoluteFile();
			for (String s : System.getProperty("java.library.path").split("[/]")) {
				f = new File(f, s);
			}
			sb.append(f.getAbsolutePath()).append(" exists = ").append(f.exists()).append("\n");
			if (f.exists()) {
				for (File f2 : f.listFiles()) {
					sb.append(" - ").append(f2.getName()).append("\n");
				}
			}
		}
		return sb.toString();
	}
	
	public static void reportError(String text) {
		Integration integration = new Integration(
			new WebIntegrationBackend(
					"https://airships.zarkonnen.com/api/",
					"https://airships.zarkonnen.com/static/media/",
					"https://airships.zarkonnen.com",
					"https://airships.zarkonnen.com/multiplayer_calendar"
			));
		integration.sendFeedback("MAJOR ERROR: " + text);
	}
}
