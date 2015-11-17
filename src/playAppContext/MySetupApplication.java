package playAppContext;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.xmlpull.v1.XmlPullParserException;

import app.ConditionalSourceSinkManager;
import soot.G;
import soot.Main;
import soot.Scene;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.ipc.IIPCManager;
import soot.options.Options;

public class MySetupApplication extends SetupApplication {
	/**
	 * Creates a new instance of the {@link SetupApplication} class
	 * 
	 * @param androidJar
	 *            The path to the Android SDK's "platforms" directory if Soot shall automatically select the JAR file to
	 *            be used or the path to a single JAR file to force one.
	 * @param apkFileLocation
	 *            The path to the APK file to be analyzed
	 * @param extraJar
	 * 			  The jar libs       
	 */
	public MySetupApplication(String androidJar, String apkFileLocation,
			String extraJar) {
		super(androidJar, apkFileLocation);
		this.extraJar = extraJar;
	}
	
	/**
	 * Creates a new instance of the {@link SetupApplication} class
	 * 
	 * @param androidJar
	 *            The path to the Android SDK's "platforms" directory if Soot shall automatically select the JAR file to
	 *            be used or the path to a single JAR file to force one.
	 * @param apkFileLocation
	 *            The path to the APK file to be analyzed
	 * @param extraJar
	 * 			  The jar libs 
	 * @param ipcManager
	 * 			  The IPC manager to use for modelling inter-component and inter-application data flows              
	 */
	public MySetupApplication(String androidJar, String apkFileLocation,
			IIPCManager ipcManager, String extraJar) {
		super(androidJar, apkFileLocation, ipcManager);
		this.extraJar = extraJar;
	}
	
	/**
	 * Calculates the sets of sources, sinks, entry points, and callbacks methods for the given APK file.
	 * 
	 * @param sources
	 *            The methods that shall be considered as sources
	 * @param sinks
	 *            The methods that shall be considered as sinks
	 * @throws IOException
	 *             Thrown if the given source/sink file could not be read.
	 * @throws XmlPullParserException
	 *             Thrown if the Android manifest file could not be read.
	 */
	public void calculateSourcesSinksEntrypoints(
			Set<AndroidMethod> sourceMethods, Set<AndroidMethod> sinkMethods)
			throws IOException, XmlPullParserException {
		// read manifest of apk
		ProcessManifest processMan = new ProcessManifest(apkFileLocation);
		appPackageName = processMan.getPackageName();
		// Gets all classes the contain entry points in this applications
		entrypoints = processMan.getEntryPointClasses();

		long beforeARSC = System.nanoTime(); 
		// parse resource def file: resources.arsc 
		ARSCFileParser resParser = new ARSCFileParser();
		resParser.parse(apkFileLocation);
		logger.info("ARSC file parsing took "
				+ (System.nanoTime() - beforeARSC) / 1.0E9D + " seconds");
		resourcePackages = resParser.getPackages();

		if (enableCallbacks) {
			// Calculates the set of callback methods declared in the XML resource files or the app's source code
			calculateCallbackMethods(resParser);
		}
		sources = new HashSet<>(sourceMethods);
		sinks = new HashSet<>(sinkMethods);

		System.out.println("Entry point calculation done.");

		G.reset();

		Set<AndroidMethod> callbacks = new HashSet<>();
		for (Set<AndroidMethod> methods : callbackMethods.values()) {
			callbacks.addAll(methods);
		}
		sourceSinkManager = new ConditionalSourceSinkManager(sources, sinks,
				callbacks, layoutMatchingMode, layoutControls);
		sourceSinkManager.setAppPackageName(appPackageName);
		sourceSinkManager.setResourcePackages(resourcePackages);
		sourceSinkManager.setEnableCallbackSources(enableCallbackSources);

		entryPointCreator = createEntryPointCreator();
	}

	public static String findJarfiles(String dirName) {
		File dir = new File(dirName);

		File[] jarFiles = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".jar");
			}
		});
		StringBuffer sb = new StringBuffer();
		File[] arrayOfFile1;
		int j = (arrayOfFile1 = jarFiles).length;
		for (int i = 0; i < j; i++) {
			File f = arrayOfFile1[i];
			sb.append(f.getAbsolutePath());
			sb.append(File.pathSeparator);
		}
		return sb.toString();
	}

	protected void initializeSoot() {
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_output_format(11);
		Options.v().set_whole_program(true);
		Options.v().set_process_dir(Collections.singletonList(apkFileLocation));
		Options.v().set_soot_classpath(
				findJarfiles(extraJar)
						+ File.pathSeparator
						+ Scene.v().getAndroidJarPath(androidJar,
								apkFileLocation));
		if (forceAndroidJar) {
			Options.v().set_force_android_jar(androidJar);
		} else
			Options.v().set_android_jars(androidJar);
		Options.v().set_src_prec(5);
		Main.v().autoSetOptions();

		switch (callgraphAlgorithm) {
		case AutomaticSelection:
			Options.v().setPhaseOption("cg.spark", "on");
			break;
		case RTA:
			Options.v().setPhaseOption("cg.spark", "on");
			Options.v().setPhaseOption("cg.spark", "rta:true");
			break;
		case OnDemand:
			Options.v().setPhaseOption("cg.spark", "on");
			Options.v().setPhaseOption("cg.spark", "vta:true");
			break;
		case CHA:
		default:
			throw new RuntimeException("Invalid callgraph algorithm");
		}

		Scene.v().loadNecessaryClasses();
	}
}
