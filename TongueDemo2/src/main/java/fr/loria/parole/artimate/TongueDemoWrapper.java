package fr.loria.parole.artimate;

import com.jdotsoft.jarloader.JarClassLoader;

public class TongueDemoWrapper {

	public static void main(String[] args) {
		JarClassLoader jcl = new JarClassLoader();
		try {
			jcl.invokeMain("fr.loria.parole.artimate.TongueDemo", args);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
