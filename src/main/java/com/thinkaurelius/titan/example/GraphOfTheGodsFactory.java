package com.thinkaurelius.titan.example;

import com.thinkaurelius.titan.core.EdgeLabel;
import com.thinkaurelius.titan.core.Multiplicity;
import com.thinkaurelius.titan.core.PropertyKey;
import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.TitanVertex;
import com.thinkaurelius.titan.core.TitanIndexQuery.Result;
import com.thinkaurelius.titan.core.attribute.Geoshape;
import com.thinkaurelius.titan.core.attribute.Text;
import com.thinkaurelius.titan.core.schema.ConsistencyModifier;
import com.thinkaurelius.titan.core.schema.SchemaAction;
import com.thinkaurelius.titan.core.schema.TitanGraphIndex;
import com.thinkaurelius.titan.core.schema.TitanManagement;
import com.thinkaurelius.titan.graphdb.database.management.ManagementSystem;

import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Example Graph factory that creates a {@link TitanGraph} based on roman mythology.
 * Used in the documentation examples and tutorials.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class GraphOfTheGodsFactory {

    public static final String INDEX_NAME = "search";

    public static TitanGraph create(final String directory) {
        TitanFactory.Builder config = TitanFactory.build();
        config.set("storage.backend", "cassandra");
        config.set("storage.directory", directory);
        config.set("index." + INDEX_NAME + ".backend", "elasticsearch");
        config.set("index." + INDEX_NAME + ".directory", directory + File.separator + "es");
        config.set("index." + INDEX_NAME + ".elasticsearch.local-mode", true);
        config.set("index." + INDEX_NAME + ".elasticsearch.client-only", false);

        TitanGraph graph = config.open();
        GraphOfTheGodsFactory.load(graph);
        return graph;
    }

    public static void loadWithoutMixedIndex(final TitanGraph graph, boolean uniqueNameCompositeIndex) {
        load(graph, null, uniqueNameCompositeIndex);
    }

    public static void load(final TitanGraph graph) {
        load(graph, INDEX_NAME, true);
    }
    
public static void load(final TitanGraph graph, String mixedIndexName, boolean uniqueNameCompositeIndex) {
    	
    	//Create Schema
    	System.out.println("li yuan's import code");
    	ManagementSystem mgmt = (ManagementSystem) graph.openManagement();
        PropertyKey nodeId = mgmt.makePropertyKey("nid").dataType(String.class).make();

        mgmt.buildIndex("vvvByNid", Vertex.class).addKey(nodeId).buildMixedIndex("search");
//        mgmt.buildIndex("vvvByNid", Vertex.class).addKey(nodeId).buildCompositeIndex();
        
        mgmt.makeEdgeLabel("follow").make();

        mgmt.makeVertexLabel("node").make();
        
        mgmt.commit();
        
        System.out.println("2");
        
        try {
			mgmt.awaitGraphIndexStatus(graph, "vvvByNid").call();
			
			System.out.println("3");
//			mgmt = (ManagementSystem) graph.openManagement();
//	        mgmt.updateIndex(mgmt.getGraphIndex("vvvByNid"), SchemaAction.REINDEX).get();
//	        System.out.println("4");
//	        mgmt.commit();
		} catch (InterruptedException  e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        
        
        try {
        	Map <Long, TitanVertex> catchNodeSet = new HashMap<Long, TitanVertex>();
//        	Map <String, TitanVertex> transactionNodeSet = new HashMap<String, TitanVertex>();
        	File graphFile = new File("/home/nkfly/soc-pokec-relationships.txt.out");
            BufferedReader reader = new BufferedReader(new FileReader(graphFile));
            int relationsPerTransaction = 100;
            int count = 0;
            long allCount = 0;
            
            String line;
            while ( (line = reader.readLine()) != null ) {
            	
            	if (allCount % 10000 == 0) {
            		System.out.println(allCount);
            	}
            	
            	if (allCount >= 20000000) {
            		break;
            	}
            	
            	if (count > relationsPerTransaction) {
            		graph.tx().commit();
            		graph.tx().open();
//            		tx = graph.newTransaction();
            		
//            		for (String nid : transactionNodeSet.keySet()) {
//            			globalNodeSet.put(nid, new Object());
//            		}
//            		transactionNodeSet = new HashMap<String, TitanVertex>();
            		count = 0;
            		
            		if (catchNodeSet.size() > 300000) {
            			catchNodeSet = new HashMap<Long, TitanVertex>();
            		}
            	}
            	
            	
            	String[] entries = line.split("\\s+");
            	String node1 = entries[0];
            	Long node1Long = Long.parseLong(node1);
            	String node2 = entries[1];
            	Long node2Long = Long.parseLong(node2);
            	
            	
            	TitanVertex v1 = catchNodeSet.get(node1Long);
            	if (v1 == null) {
            		Iterable<Result<TitanVertex>> vertices = graph.indexQuery("vvvByNid", "v.nid:(" + node1 + ")").vertices();
//                	Iterable<TitanVertex> vertices = graph.query().has("nid", Text.CONTAINS, node1).vertices();
                	Iterator n1Iter = vertices.iterator();
                	
                	if (n1Iter.hasNext()) {
                		Result<TitanVertex> result = (Result<TitanVertex>)n1Iter.next();
                		v1 = result.getElement();
//                		v1 = (TitanVertex)n1Iter.next();
                	} else {
                		v1 = graph.addVertex(T.label, "node", "nid", node1);
                	}
                	catchNodeSet.put(node1Long, v1);
            	}
            	
            	
            	TitanVertex v2 = catchNodeSet.get(node2Long);
            	if (v2 == null) {
            		Iterable<Result<TitanVertex>> vertices = graph.indexQuery("vvvByNid", "v.nid:(" + node2 + ")").vertices();
//                	vertices = graph.query().has("nid", Text.CONTAINS, node2).vertices();
                	
                	Iterator n2Iter = vertices.iterator();
                	
                	if (n2Iter.hasNext()) {
                		Result<TitanVertex> result = (Result<TitanVertex>)n2Iter.next();
                		v2 = result.getElement();
//                		v2 = (TitanVertex)n2Iter.next();
                	} else {
                		v2 = graph.addVertex(T.label, "node", "nid", node2);
                	}
                	
                	catchNodeSet.put(node2Long, v2);
            	}
            	
            	
            	v1.addEdge("follow", v2);
            	count++;
            	allCount++;
            }
        	
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
        // commit the transaction to disk
//        tx.commit();
        graph.tx().commit();
        
       
    }

//    public static void load(final TitanGraph graph, String mixedIndexName, boolean uniqueNameCompositeIndex) {
//    	
//    	//Create Schema
//    	System.out.println("li yuan's import code");
//        TitanManagement mgmt = graph.openManagement();
//        final PropertyKey nodeId = mgmt.makePropertyKey("nodeId").dataType(String.class).make();
//        TitanManagement.IndexBuilder nameIndexBuilder = mgmt.buildIndex("nodeId", Vertex.class).addKey(nodeId);
//        if (uniqueNameCompositeIndex)
//            nameIndexBuilder.unique();
//        TitanGraphIndex namei = nameIndexBuilder.buildCompositeIndex();
//        mgmt.setConsistency(namei, ConsistencyModifier.LOCK);
//
//        mgmt.makeEdgeLabel("follow").make();
//
//        mgmt.makeVertexLabel("node").make();
//        
//        mgmt.commit();
//
//        TitanTransaction tx = graph.newTransaction();
//        // vertices
//        
//        
//        try {
//        	
//        	Map <String, Vertex> nodeSet = new HashMap<String, Vertex>();
//        	File graphFile = new File("/home/nkfly/soc-pokec-relationships.txt");
//            BufferedReader reader = new BufferedReader(new FileReader(graphFile));
//            
//            String line;
//            while ( (line = reader.readLine()) != null ) {
//            	String[] entries = line.split("\\s+");
//            	String node1 = entries[0];
//            	String node2 = entries[1];
//            	
//            	Vertex v1 = nodeSet.get(node1);
//            	if (v1 == null) {
//            		v1 = tx.addVertex(T.label, "node", "nodeId", node1);
//            		nodeSet.put(node1, v1);
//            	}
//            	
//            	Vertex v2 = nodeSet.get(node2);
//            	if (v2 == null) {
//            		v2 = tx.addVertex(T.label, "node", "nodeId", node2);
//            		nodeSet.put(node2, v2);
//            	}
//            	
//            	v1.addEdge("follow", v2);
//            	
//            }
//        	
//        } catch (Exception e) {
//        	e.printStackTrace();
//        }
//        
//        // commit the transaction to disk
//        tx.commit();
//        
//        
//
////        //Create Schema
////    	System.out.println("li yuan's code");
////        TitanManagement mgmt = graph.openManagement();
////        final PropertyKey name = mgmt.makePropertyKey("name").dataType(String.class).make();
////        TitanManagement.IndexBuilder nameIndexBuilder = mgmt.buildIndex("name", Vertex.class).addKey(name);
////        if (uniqueNameCompositeIndex)
////            nameIndexBuilder.unique();
////        TitanGraphIndex namei = nameIndexBuilder.buildCompositeIndex();
////        mgmt.setConsistency(namei, ConsistencyModifier.LOCK);
////        final PropertyKey age = mgmt.makePropertyKey("age").dataType(Integer.class).make();
////        if (null != mixedIndexName)
////            mgmt.buildIndex("vertices", Vertex.class).addKey(age).buildMixedIndex(mixedIndexName);
////
////        final PropertyKey time = mgmt.makePropertyKey("time").dataType(Integer.class).make();
////		final PropertyKey reason = mgmt.makePropertyKey("reason").dataType(String.class).make();
////        final PropertyKey place = mgmt.makePropertyKey("place").dataType(Geoshape.class).make();
////        if (null != mixedIndexName)
////            mgmt.buildIndex("edges", Edge.class).addKey(reason).addKey(place).buildMixedIndex(mixedIndexName);
////
////        mgmt.makeEdgeLabel("father").multiplicity(Multiplicity.MANY2ONE).make();
////        mgmt.makeEdgeLabel("mother").multiplicity(Multiplicity.MANY2ONE).make();
////        EdgeLabel battled = mgmt.makeEdgeLabel("battled").signature(time).make();
////        mgmt.buildEdgeIndex(battled, "battlesByTime", Direction.BOTH, Order.decr, time);
////        mgmt.makeEdgeLabel("lives").signature(reason).make();
////        mgmt.makeEdgeLabel("pet").make();
////        mgmt.makeEdgeLabel("brother").make();
////
////        mgmt.makeVertexLabel("titan").make();
////        mgmt.makeVertexLabel("location").make();
////        mgmt.makeVertexLabel("god").make();
////        mgmt.makeVertexLabel("demigod").make();
////        mgmt.makeVertexLabel("human").make();
////        mgmt.makeVertexLabel("monster").make();
////
////        mgmt.commit();
////
////        TitanTransaction tx = graph.newTransaction();
////        // vertices
////
////        Vertex saturn = tx.addVertex(T.label, "titan", "name", "saturn", "age", 10000);
////
////        Vertex sky = tx.addVertex(T.label, "location", "name", "sky");
////
////        Vertex sea = tx.addVertex(T.label, "location", "name", "sea");
////
////        Vertex jupiter = tx.addVertex(T.label, "god", "name", "jupiter", "age", 5000);
////
////        Vertex neptune = tx.addVertex(T.label, "god", "name", "neptune", "age", 4500);
////
////        Vertex hercules = tx.addVertex(T.label, "demigod", "name", "hercules", "age", 30);
////
////        Vertex alcmene = tx.addVertex(T.label, "human", "name", "alcmene", "age", 45);
////
////        Vertex pluto = tx.addVertex(T.label, "god", "name", "pluto", "age", 4000);
////
////        Vertex nemean = tx.addVertex(T.label, "monster", "name", "nemean");
////
////        Vertex hydra = tx.addVertex(T.label, "monster", "name", "hydra");
////
////        Vertex cerberus = tx.addVertex(T.label, "monster", "name", "cerberus");
////
////        Vertex tartarus = tx.addVertex(T.label, "location", "name", "tartarus");
////
////        // edges
////
////        jupiter.addEdge("father", saturn);
////        jupiter.addEdge("lives", sky, "reason", "loves fresh breezes");
////        jupiter.addEdge("brother", neptune);
////        jupiter.addEdge("brother", pluto);
////
////        neptune.addEdge("lives", sea).property("reason", "loves waves");
////        neptune.addEdge("brother", jupiter);
////        neptune.addEdge("brother", pluto);
////
////        hercules.addEdge("father", jupiter);
////        hercules.addEdge("mother", alcmene);
////        hercules.addEdge("battled", nemean, "time", 1, "place", Geoshape.point(38.1f, 23.7f));
////        hercules.addEdge("battled", hydra, "time", 2, "place", Geoshape.point(37.7f, 23.9f));
////        hercules.addEdge("battled", cerberus, "time", 12, "place", Geoshape.point(39f, 22f));
////
////        pluto.addEdge("brother", jupiter);
////        pluto.addEdge("brother", neptune);
////        pluto.addEdge("lives", tartarus, "reason", "no fear of death");
////        pluto.addEdge("pet", cerberus);
////
////        cerberus.addEdge("lives", tartarus);
////
////        // commit the transaction to disk
////        tx.commit();
//    }

    /**
     * Calls {@link TitanFactory#open(String)}, passing the Titan configuration file path
     * which must be the sole element in the {@code args} array, then calls
     * {@link #load(com.thinkaurelius.titan.core.TitanGraph)} on the opened graph,
     * then calls {@link com.thinkaurelius.titan.core.TitanGraph#close()}
     * and returns.
     * <p/>
     * This method may call {@link System#exit(int)} if it encounters an error, such as
     * failure to parse its arguments.  Only use this method when executing main from
     * a command line.  Use one of the other methods on this class ({@link #create(String)}
     * or {@link #load(com.thinkaurelius.titan.core.TitanGraph)}) when calling from
     * an enclosing application.
     *
     * @param args a singleton array containing a path to a Titan config properties file
     */
    public static void main(String args[]) {
        if (null == args || 1 != args.length) {
            System.err.println("Usage: GraphOfTheGodsFactory <titan-config-file>");
            System.exit(1);
        }

        TitanGraph g = TitanFactory.open(args[0]);
        load(g);
        g.close();
    }
}
