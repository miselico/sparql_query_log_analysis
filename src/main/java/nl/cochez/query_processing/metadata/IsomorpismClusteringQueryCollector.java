package nl.cochez.query_processing.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.core.BasicPattern;
import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;
import org.jgrapht.graph.DefaultDirectedGraph;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;

public class IsomorpismClusteringQueryCollector implements IQueryCollector {

	/**
	 * This edge does not implement equals/hashcode to prevent multiple edges with
	 * he same identifier to replace each other
	 *
	 */
	public static class Edge {
		public final String value;
		private final String group;

//		public Edge(String value) {
//			this.value = value;
//			
//			this.group = getGroup(value);
//			// Use the version below instead to get make the isomorphism checker care about
//			// the actual property
//			// this.group = value;
//		}

		private Edge(String value, String group) {
			this.value = value;
			this.group = group;
		}

		@Override
		public String toString() {
			return this.group + "{" + this.value + "}";
			// return group;
		}

		public static Comparator<Edge> SEMANTIC_COMPARATOR = new Comparator<IsomorpismClusteringQueryCollector.Edge>() {

			@Override
			public int compare(Edge o1, Edge o2) {
				return o1.group.compareTo(o2.group);
			}
		};

		public static Edge forVariable(String name) {
			String thegroup = "variable";
			return new Edge(name, thegroup);
		}

		public static Edge forURL(String uri) {
			String thegroup = EquivalenceClasses.getEquivalentOrDefault(uri, EquivalenceClasses.PROPERTY_GROUP);
			return new Edge(uri, thegroup);
		}

	}

	public static class Node {
		public final String value;
		private final String group;

		private Node(String value, String group) {
			super();
			this.value = value;
			this.group = group;
		}

		public static Node forURLEntity(String label) {
			return new Node(label, EquivalenceClasses.getEquivalentOrDefault(label, EquivalenceClasses.ENTITY_GROUP));
		}

		public static Node forBlanknode(String label) {
			return new Node(label, EquivalenceClasses.ENTITY_GROUP);
		}

		public static Node forVariable(String name) {
			return new Node(name, "variable");
		}

		public static Node forLiteral(String value) {
			return new Node(value, "literal");
		}

		@Override
		public String toString() {
			return group.toString() + "{" + value.toString() + "}";
			// return value.toString();
		}

		public static Comparator<Node> SEMANTIC_COMPARATER = new Comparator<IsomorpismClusteringQueryCollector.Node>() {

			@Override
			public int compare(Node o1, Node o2) {
				return o1.group.compareTo(o2.group);
			}
		};

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Node other = (Node) obj;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}

	}

	static DefaultDirectedGraph<Node, Edge> simplify(BasicPattern bgp) {
		DefaultDirectedGraph<Node, Edge> asGraph = new DefaultDirectedGraph<Node, Edge>(Edge.class);
		for (Triple triple : bgp) {

			Node subject;
			Node object;
			Edge predicate;

			if (triple.getSubject().isVariable()) {
				subject = Node.forVariable(triple.getSubject().getName());
			} else if (triple.getSubject().isBlank()) {
				subject = Node.forBlanknode(triple.getSubject().getBlankNodeLabel());
			} else {
				subject = Node.forURLEntity(triple.getSubject().getURI());
			}

			if (triple.getPredicate().isVariable()) {
				predicate = Edge.forVariable(triple.getPredicate().getName());

			} else {
				predicate = Edge.forURL(triple.getPredicate().getURI());
			}
			if (triple.getObject().isVariable()) {
				object = Node.forVariable(triple.getObject().getName());
			} else if (triple.getObject().isBlank()) {
				object = Node.forBlanknode(triple.getObject().getBlankNodeLabel());
			} else if (triple.getObject().isLiteral()) {
				object = Node.forLiteral(triple.getObject().getLiteralLexicalForm());
			} else {
				object = Node.forURLEntity(triple.getObject().getURI());
			}

			asGraph.addVertex(subject);
			asGraph.addVertex(object);
			asGraph.addEdge(subject, object, predicate);
		}

		return asGraph;

	}

	/**
	 * A class to speed up comparisons by first precomputing some graph features to
	 * quickly eliminate some candidates
	 * 
	 * @author cochez
	 *
	 */
	static class QuerySignature {
		private final int edgeCount;
//TODO add more parts to the signature
		// private final int variable_node_count;
//		private final int entityCount;

		public QuerySignature(DefaultDirectedGraph<Node, Edge> query) {
			this.edgeCount = query.edgeSet().size();

//			int _varCount = 0;
//			int _entityCount = 0;
//			for (Triple triple : triples) {
//				if (triple.getSubject().isVariable()) {
//					_varCount++;
//				} else {
//					_entityCount++;
//				}
//				if (triple.getObject().isVariable()) {
//					_varCount++;
//				} else {
//					_entityCount++;
//				}
//				if ()
//				triple.getPredicate()
//
//			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + edgeCount;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			QuerySignature other = (QuerySignature) obj;
			if (edgeCount != other.edgeCount)
				return false;
			return true;
		}

	}

	static class GraphWithCountAndIsomorphs {
		public DefaultDirectedGraph<Node, Edge> graph;
		public int count;
		public final ArrayList<DefaultDirectedGraph<Node, Edge>> isomorphs;

		public GraphWithCountAndIsomorphs(DefaultDirectedGraph<Node, Edge> graph, int count) {
			this.graph = graph;
			this.count = count;
			this.isomorphs = new ArrayList<DefaultDirectedGraph<Node, Edge>>();
			this.isomorphs.add(graph);
		}

		public int getCount() {
			return count;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("count", this.count).add("graph", this.graph).toString();
		}

	}

	private AllBGPOpVisitor visitor = new AllBGPOpVisitor() {

		@Override
		public void visit(OpBGP opBGP) {
			IsomorpismClusteringQueryCollector.this.addBGP(opBGP.getPattern());
		}
	};

	// maps from the signatuer to a list of potentially isomorpic graphs
	private ArrayListMultimap<QuerySignature, GraphWithCountAndIsomorphs> theGraphCollection = ArrayListMultimap.create();

	private int count = 0;
	private int failures = 0;

	private void addBGP(BasicPattern bgp) {
		DefaultDirectedGraph<Node, Edge> graphReadyForIsomorphismcheck = IsomorpismClusteringQueryCollector.simplify(bgp);
		QuerySignature signature = new QuerySignature(graphReadyForIsomorphismcheck);
		List<GraphWithCountAndIsomorphs> candidates = theGraphCollection.get(signature);
		if (candidates.isEmpty()) {
			// we are sure there is no isomorphism to this graph
			theGraphCollection.put(signature, new GraphWithCountAndIsomorphs(graphReadyForIsomorphismcheck, 1));
			return;
		}
		for (GraphWithCountAndIsomorphs candidate : candidates) {
			// check isomporisms
			VF2GraphIsomorphismInspector<Node, Edge> inspector = new VF2GraphIsomorphismInspector<Node, Edge>(candidate.graph, graphReadyForIsomorphismcheck, Node.SEMANTIC_COMPARATER,
					Edge.SEMANTIC_COMPARATOR, true);
			if (inspector.isomorphismExists()) {
				candidate.count++;
				candidate.isomorphs.add(graphReadyForIsomorphismcheck);
				return;
			}
		}
		// we haven't found a match, so add to these candidates
		candidates.add(new GraphWithCountAndIsomorphs(graphReadyForIsomorphismcheck, 1));
	}

	@Override
	public void add(Query q) {
		Op op = Algebra.compile(q);
		op.visit(visitor);
		this.count++;

		if ((this.count % 10000) == 0) {
			this.optimize();
		}
	}

	@Override
	public void reportFailure(String input) {
		this.failures++;
	}

	@Override
	public void stats() {
		int uniqueIsomorpisms = 0;
		ArrayList<GraphWithCountAndIsomorphs> counts = Lists.newArrayList();
		for (Entry<QuerySignature, GraphWithCountAndIsomorphs> graphWithCount : this.theGraphCollection.entries()) {
			uniqueIsomorpisms++;
			counts.add(graphWithCount.getValue());
		}
		Collections.sort(counts, new Comparator<GraphWithCountAndIsomorphs>() {

			@Override
			public int compare(GraphWithCountAndIsomorphs o1, GraphWithCountAndIsomorphs o2) {
				return o2.getCount() - o1.getCount();
			}
		});

		int skipped = 0;
		for (GraphWithCountAndIsomorphs graphWithCount : counts) {
			if (graphWithCount.count > 100) {
				System.out.println(graphWithCount);
			} else {
				skipped++;
			}
		}
		System.out.println("Skipped " + skipped + " more shapes with frequency < 100");
		System.out.println("Total number of uinique isomorpisms: " + uniqueIsomorpisms);
		System.out.println("Total number of query parsing errors: " + this.failures);
		for (GraphWithCountAndIsomorphs graphWithCount : counts) {
			if (graphWithCount.count < 100) {
				System.out.println(graphWithCount);
				for (DefaultDirectedGraph<Node, Edge> b : graphWithCount.isomorphs) {
					System.out.println("\t" + b);
				}
				break;
			}
		}

	}

	private void optimize() {

		// we are putting frequently occuring isomorpisms at the front
		for (Collection<GraphWithCountAndIsomorphs> candidate_list : this.theGraphCollection.asMap().values()) {
			Collections.sort((List<GraphWithCountAndIsomorphs>) candidate_list, new Comparator<GraphWithCountAndIsomorphs>() {

				@Override
				public int compare(GraphWithCountAndIsomorphs o1, GraphWithCountAndIsomorphs o2) {
					return Integer.compare(o1.count, o2.count);
				}
			}.reversed());
		}

	}
}
