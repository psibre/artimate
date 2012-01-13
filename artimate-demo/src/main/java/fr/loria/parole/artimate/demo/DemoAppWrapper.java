package fr.loria.parole.artimate.demo;

import com.jdotsoft.jarloader.JarClassLoader;

public class DemoAppWrapper {

	public static void main(String[] args) {
		JarClassLoader jcl = new JarClassLoader();
		try {
			jcl.invokeMain("fr.loria.parole.artimate.demo.DemoApp", args);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
