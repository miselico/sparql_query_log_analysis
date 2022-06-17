package nl.cochez.query_processing.metadata;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;

import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

public class MainStatistics {

	private static class StatVisitor extends AllBGPOpVisitor {

		Multiset<String> subjects = HashMultiset.create();
		Multiset<String> predicates = HashMultiset.create();
		Multiset<String> objects = HashMultiset.create();
		Multiset<String> literal_values = HashMultiset.create();
		Multiset<String> literal_labels = HashMultiset.create();
		Multiset<String> languages = HashMultiset.create();
		Multiset<String> types = HashMultiset.create();

		@Override
		public void visit(OpBGP opBGP) {
			for (Triple triple : opBGP.getPattern().getList()) {
				Node s = triple.getSubject();
				if (s.isURI()) {
					subjects.add(s.getURI());
				} else if (s.isVariable()) {
					// TODO
				} else {
					// blank nodes ingored
				}
				Node p = triple.getPredicate();
				if (p.isURI()) {
					predicates.add(s.getURI());
				} else if (p.isVariable()) {
					// TODO
				} else {
					throw new AssertionError("This should never happen");
				}
				Node o = triple.getObject();
				if (o.isURI()) {
					objects.add(s.getURI());
				} else if (o.isVariable()) {
					// TODO
				} else if (o.isLiteral()) {
					LiteralLabel l = o.getLiteral();
					literal_values.add(l.getLexicalForm());
					String type = l.getDatatypeURI();
					if (type == null) {
						type = "no-type";
					}
					String language = l.language();
					if (language.equals("")) {
						language = "no-language-tag";
					}

					types.add(type);
					languages.add(language);
					literal_labels.add(type + "---" + language);

				} else {
					// blank nodes ingored
				}
			}

		}
	};

	public static void main(String[] args) throws IOException {
		Stopwatch watch = Stopwatch.createStarted();

		InputStream in = new FileInputStream("/home/cochez/papers/propertyGraphApproxQueries/wikidataQueries/2017-06-12_2017-07-09_organic.tsv.gz");

		final StatVisitor visitor = new StatVisitor();

		IQueryCollector collector = new IQueryCollector() {

			@Override
			public void stats() {
				System.out.println(visitor.subjects);
				System.out.println(visitor.predicates);
				System.out.println(visitor.objects);
				System.out.println(visitor.literal_values);
				System.out.println(visitor.languages);
				System.out.println(visitor.types);
				System.out.println(visitor.literal_labels);
			}

			@Override
			public void reportFailure(String input) {
//				throw new RuntimeException("");
				// System.err.println("ignoring" + input);
			}

			@Override
			public void add(Query q) {
				Op op = Algebra.compile(q);
				// System.out.println("NEXT QUERY");
				op.visit(visitor);
			}
		};

		// IterateQueriesFromWikidataLog.processFromFile(new GZIPInputStream(in),
		// collector);

		List<String> queries = Lists.newArrayList("SELECT * WHERE { <http://test.com/subject> ?p \"Hello\" }", "SELECT * WHERE { <http://test.com/subject> ?p 5 }",
				"SELECT * WHERE { <http://test.com/subject> ?p \"Hello\"@en }");
		for (String query : queries) {
			Query q = QueryFactory.create(query);
			System.out.println(q);
			Op op = Algebra.compile(q);
			op.visit(visitor);
		}
		watch.stop();
		System.out.println("Elapsed" + watch.elapsed(TimeUnit.SECONDS));

		collector.stats();
	}
}
