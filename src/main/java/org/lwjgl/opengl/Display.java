package org.lwjgl.opengl;

import static org.lwjgl.glfw.GLFW.*;
import com.zarkonnen.catengine.lwjgl3.Lwjgl3Engine;
import org.lwjgl.glfw.GLFWVidMode;

/** LWJGL2 Display 兼容层（窗口定位与桌面分辨率）。 */
public class Display {
	private Display() {}

	public static void setLocation(int x, int y) {
		if (Lwjgl3Engine.glfwWindow != 0) {
			glfwSetWindowPos(Lwjgl3Engine.glfwWindow, x, y);
		}
	}

	public static DisplayMode getDesktopDisplayMode() {
		long monitor = glfwGetPrimaryMonitor();
		GLFWVidMode vm = glfwGetVideoMode(monitor);
		if (vm == null) return new DisplayMode(1920, 1080);
		return new DisplayMode(vm.width(), vm.height());
	}

	public static boolean isCreated() { return Lwjgl3Engine.glfwWindow != 0; }

	public static void setResizable(boolean resizable) {
		if (Lwjgl3Engine.glfwWindow != 0) {
			glfwSetWindowAttrib(Lwjgl3Engine.glfwWindow, GLFW_RESIZABLE, resizable ? GLFW_TRUE : GLFW_FALSE);
		}
	}
}
