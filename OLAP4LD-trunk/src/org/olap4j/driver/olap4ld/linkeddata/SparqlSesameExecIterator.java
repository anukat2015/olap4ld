package org.olap4j.driver.olap4ld.linkeddata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.olap4j.driver.olap4ld.Olap4ldUtil;
import org.olap4j.driver.olap4ld.helper.Olap4ldLinkedDataUtil;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

public class SparqlSesameExecIterator implements ExecIterator {

	private SailRepository repo;
	private String query;
	private List<Node[]> result;
	private Iterator<Node[]> iterator;

	public SparqlSesameExecIterator(SailRepository repo, String query) {
		this.repo = repo;
		this.query = query;
		
		this.result = sparql();
		
		this.iterator = result.iterator();
	}
	
	/**
	 * Simply copied over from embedded sesame.
	 * @param query
	 * @param caching
	 * @return
	 */
	private List<Node[]> sparql() {

		Olap4ldUtil._log.info("SPARQL query: " + query);

		List<Node[]> myBindings = new ArrayList<Node[]>();

		try {
			RepositoryConnection con = repo.getConnection();

			ByteArrayOutputStream boas = new ByteArrayOutputStream();
			// FileOutputStream fos = new
			// FileOutputStream("/home/benedikt/Workspaces/Git-Repositories/olap4ld/OLAP4LD-trunk/resources/result.srx");

			SPARQLResultsXMLWriter sparqlWriter = new SPARQLResultsXMLWriter(
					boas);

			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL,
					query);
			tupleQuery.evaluate(sparqlWriter);

			ByteArrayInputStream bais = new ByteArrayInputStream(
					boas.toByteArray());

			// String xmlwriterstreamString =
			// Olap4ldLinkedDataUtil.convertStreamToString(bais);
			// System.out.println(xmlwriterstreamString);
			// Transform sparql xml to nx
			InputStream nx = Olap4ldLinkedDataUtil.transformSparqlXmlToNx(bais);
			String test2 = Olap4ldLinkedDataUtil.convertStreamToString(nx);
			Olap4ldUtil._log.info("NX output: " + test2);
			nx.reset();

			NxParser nxp = new NxParser(nx);

			Node[] nxx;
			while (nxp.hasNext()) {
				try {
					nxx = nxp.next();
					myBindings.add(nxx);
				} catch (Exception e) {
					Olap4ldUtil._log
							.warning("NxParser: Could not parse properly: "
									+ e.getMessage());
				}
				;
			}

			con.close();
			boas.close();
			// do something interesting with the values here...
			// con.close();
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedQueryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (QueryEvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TupleQueryResultHandlerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return myBindings;
	}

	public boolean hasNext() {
		return iterator.hasNext();
	}

	/**
	 * Simply use iterator.
	 */
	public Object next() {
		return iterator.next();
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
	}

	@Override
	public void init() throws Exception {
		;
	}

	@Override
	public void close() throws Exception {
		;
	}

	@Override
	public void accept(Visitor v) throws QueryException {
		// Needed in case we have as arguments further exec iterators.
		;
	}

}