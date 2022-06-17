package nl.cochez.query_processing.metadata;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.apache.jena.query.QueryException;

import com.google.common.base.Stopwatch;

/**
 * 
 * 
 * 
 * @author cochez
 *
 */
public class MainIsomorphisms {

	public static void main(String[] args) throws IOException, QueryException {
		Stopwatch watch = Stopwatch.createStarted();

		InputStream in = new FileInputStream("/home/cochez/papers/propertyGraphApproxQueries/wikidataQueries/2017-06-12_2017-07-09_organic.tsv.gz");
		IsomorpismClusteringQueryCollector collector = new IsomorpismClusteringQueryCollector();

		IterateQueriesFromWikidataLog.processFromFile(new GZIPInputStream(in), collector);

		watch.stop();
		System.out.println("Elapsed" + watch.elapsed(TimeUnit.SECONDS));

		collector.stats();

	}

}
