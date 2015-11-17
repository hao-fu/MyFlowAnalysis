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
		presto.android.Main.checkAndPrintEnvironmentInformation(args);
	}

	public static void parseArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if ("-project".equals(arg)) {
				// the target project
				Configs.project = args[++i];
			} else if ("-sdk".equals(arg)) {
				Configs.sdkDir = args[++i];
			}
		}
	}

	public static void setupSoot() {
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
