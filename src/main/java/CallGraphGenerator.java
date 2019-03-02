/*
 * Generate detailed Call Graph
 */


import org.apache.commons.io.FilenameUtils;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.dot.DotGraph;
import soot.util.queue.QueueReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import static soot.util.dot.DotGraph.DOT_EXTENSION;

public class CallGraphGenerator {
    /**
     *
     * @param args args[0]: the apk file; args[1]: android.jar
     */
    public static void main(String[] args) {
        assert args.length >= 2;
        File file = new File(args[0]);
        String apkPath = file.getAbsolutePath();
        String platformPath = args[1];

        buildCallGraph(apkPath, platformPath);
        DotGraph dot = new DotGraph("callgraph");
        analyzeCG(dot, Scene.v().getCallGraph());
        String dest = file.getName();
        String fileNameWithOutExt = FilenameUtils.removeExtension(dest);
        String destination = "./sootOutput/" + fileNameWithOutExt;
        dot.plot(destination + DOT_EXTENSION);
    }

    /**
     * Iterate over the call Graph by visit edges one by one.
     * @param dot dot instance to create a dot file
     * @param cg call graph
     */
    public static void analyzeCG(DotGraph dot, CallGraph cg) {
        QueueReader<Edge> edges = cg.listener();
        Set<String> visited = new HashSet<>();

        File resultFile = new File("./sootOutput/CG.log");
        PrintWriter out = null;
        try {
            out = new PrintWriter(resultFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        assert out != null;
        out.println("CG begins==================");
        // iterate over edges of the call graph
        while (edges.hasNext()) {
            Edge edge = edges.next();
            SootMethod target = (SootMethod) edge.getTgt();
            MethodOrMethodContext src = edge.getSrc();
            if (!visited.contains(src.toString())) {
                dot.drawNode(src.toString());
                visited.add(src.toString());
            }
            if (!visited.contains(target.toString())) {
                dot.drawNode(target.toString());
                visited.add(target.toString());
            }
            out.println(src + "  -->   " + target);
            dot.drawEdge(src.toString(), target.toString());
        }

        out.println("CG ends==================");
        out.close();
        System.out.println(cg.size());
    }

    public static void buildCallGraph(String apkDir, String platformDir) {
        SetupApplication app = new SetupApplication(platformDir, apkDir);
        app.constructCallgraph();
    }

}