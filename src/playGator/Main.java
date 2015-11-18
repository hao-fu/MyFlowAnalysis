/*
 * reference: presto.android.Main
 */
package playGator;

import java.util.Map;

import presto.android.Configs;
import presto.android.Debug;
import soot.Pack;
import soot.PackManager;
import soot.SceneTransformer;
import soot.Transform;

public class Main {
	public static void main(String[] args) {
		Debug.v().setStartTime();
		parseArgs(args);
		System.out.println("finish parsing args!");
		presto.android.Main.checkAndPrintEnvironmentInformation(args);
		System.out.println("finish check and print print env info!");
		setupAndInvokeSoot();
	}

	// TODO more args
	public static void parseArgs(String[] args) {
		args = new String[] {
				"-project", "/home/hao/workspace/ApkSamples/Decompiled/app-debug.apk",
				"-sdk", "/home/hao/Android/Sdk",
				"-android", "/home/hao/workspace/gator-3.0/AndroidBench/platform/android-17/framework.jar:"
						+ "/home/hao/workspace/gator-3.0/AndroidBench/platform/android-17/bouncycastle.jar"
						+ "/home/hao/workspace/gator-3.0/AndroidBench/platform/android-17/ext.jar:"
						+ "/home/hao/workspace/gator-3.0/AndroidBench/platform/android-17/android-policy.jar:"
						+ "/home/hao/workspace/gator-3.0/AndroidBench/platform/android-17/:services.jar:"
						+ "/home/hao/workspace/gator-3.0/SootAndroid/deps/annotations.jar:"
						+ "/home/hao/workspace/gator-3.0/SootAndroid/deps/android-support-v4.jar:"
						+ "/home/hao/Android/Sdk/platforms/android-17/android.jar",
				"-apiLevel", "17",
				"-jre", "/home/hao/workspace/gator-3.0/AndroidBench/platform/android-17/core.jar",
				"-guiAnalysis",
		};
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if ("-project".equals(arg)) {
				// the target project
				Configs.project = args[++i];
			} else if ("-sdk".equals(arg)) {
				Configs.sdkDir = args[++i];
			} else if ("-guiAnalysis".equals(arg)) {
				Configs.guiAnalysis = true;
			} else if ("-android".equals(arg)) {
				// To read in platformJar
				Configs.android = args[++i];
			} else if ("-verbose".equals(arg)) {
				Configs.verbose = true;
			} else if ("-apiLevel".equals(arg)) {
				Configs.apiLevel = args[++i];
			} else if ("-jre".equals(arg)) {
				Configs.jre = args[++i];
			} else {
				throw new RuntimeException("Unknow option: " + arg);
			} 
		}
		
		Configs.processing();
	}

	public static void setupAndInvokeSoot() {
		String classpath = presto.android.Main.computeClasspath();
		String packName = "wjtp";
		String phaseName = "wjtp.gui";
		String[] sootArgs = { "-w", "-p", "cg", "all-reachable:true", "-p",
				"cg.cha", "enabled:true", "-p", phaseName, "enabled:true",
				"-f", "n", "-keep-line-number", "-allow-phantom-refs",
				"-process-dir", Configs.bytecodes, "-cp", classpath, };
		invokeSoot(packName, phaseName, sootArgs);
	}

	public static void invokeSoot(String packName, String phaseName,
			String[] sootArgs) {
		// create the phase and add it to the pack
		Pack pack = PackManager.v().getPack(packName);

		pack.add(new Transform(phaseName, new SceneTransformer() {
			@Override
			protected void internalTransform(String phaseName,
					Map<String, String> options) {
				EntryPointAnalysis.v().run();
			}
		}));
		// invoke Soot
		soot.Main.main(sootArgs);
	}
}
