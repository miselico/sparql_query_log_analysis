package nl.cochez.query_processing.metadata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;

public class IterateQueriesFromWikidataLog {
	public static void processFromFile(InputStream in, IQueryCollector collector) throws IOException {
		try (BufferedReader l = new BufferedReader(new BufferedReader(new InputStreamReader(in)))) {
			String line;
			// take headerline off
			l.readLine();
			while ((line = l.readLine()) != null) {
				String queryString = URLDecoder.decode(line.split("\t")[0], StandardCharsets.UTF_8);

				try {
					Query q = QueryFactory.create(queryString);
					collector.add(q);
				} catch (Exception e) {
					collector.reportFailure(queryString);
				}
			}
		}
	}
}
