package fr.loria.parole.TongueDemo2;

import com.jdotsoft.jarloader.JarClassLoader;

public class TongueDemoWrapper {

	public static void main(String[] args) {
		JarClassLoader jcl = new JarClassLoader();
		try {
			jcl.invokeMain("fr.loria.parole.TongueDemo2.TongueDemo", args);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
