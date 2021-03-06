/*
//
// Licensed to Benedikt Kämpgen under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Benedikt Kämpgen licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
 */
package org.olap4j.driver.olap4ld.linkeddata;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.olap4j.OlapException;
import org.olap4j.Position;
import org.olap4j.driver.olap4ld.Olap4ldUtil;
import org.olap4j.driver.olap4ld.helper.Olap4ldLinkedDataUtil;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Measure;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Variable;
import org.semanticweb.yars.nx.parser.NxParser;

/**
 * The EmbeddedSesameEngine manages an embedded Sesame repository (triple store)
 * while executing metadata or olap queries.
 * 
 * @author b-kaempgen
 * 
 */
public class QcrumbEngine implements LinkedDataCubesEngine {

	// Meta data attributes
	private static final String DATASOURCEDESCRIPTION = "OLAP data from the statistical Linked Data cloud.";

	private static final String PROVIDERNAME = "The community.";

	private static String URL;

	private static final String DATASOURCEINFO = "Data following the Linked Data principles.";

	private static final String TABLE_CAT = "LdCatalogSchema";

	private static final String TABLE_SCHEM = "LdCatalogSchema";

	public String DATASOURCENAME;

	public String DATASOURCEVERSION;

	// Helper attributes

	/**
	 * Map of locations that have been loaded into the embedded triple store.
	 */
	private HashMap<Integer, Boolean> loadedMap = new HashMap<Integer, Boolean>();

	/**
	 * The Sesame repository (triple store).
	 */
	private SailRepository repo;

	public QcrumbEngine(URL serverUrlObject,
			ArrayList<String> datastructuredefinitions,
			ArrayList<String> datasets, String databasename) {

		// We actually do not need that.
		URL = serverUrlObject.toString();

		if (databasename.equals("EMBEDDEDSESAME")) {
			DATASOURCENAME = databasename;
			DATASOURCEVERSION = "1.0";
		}

		// Store
		this.repo = new SailRepository(new MemoryStore());
		try {
			repo.initialize();

			// do something interesting with the values here...
			// con.close();
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Those given URIs we load and add to the location map
		for (String string : datastructuredefinitions) {
			try {
				String location = askForLocation(string);
				if (!isStored(location)) {
					loadInStore(location);
				}
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for (String string : datasets) {
			try {
				String location = askForLocation(string);
				if (!isStored(location)) {
					loadInStore(location);
				}
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns from String in order to retrieve information about these URIs
	 * 
	 * 
	 * Properly access the triple store: For dsd and ds we query separate
	 * graphs.
	 * 
	 * @param uris
	 * @return fromResult
	 */
	@Deprecated
	private String askForFrom(boolean isDsdQuery) {
		return "";
	}

	/**
	 * Helper Method for asking for location
	 * 
	 * @param uri
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private String askForLocation(String uri) throws MalformedURLException {

		Olap4ldUtil._log.config("Ask for location: " + uri + "...");

		URL url;
		url = new URL(uri);

		HttpURLConnection.setFollowRedirects(false);
		HttpURLConnection connection;
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestProperty("Accept", "application/rdf+xml");
			String header = connection.getHeaderField("location");
			// TODO: Could be that we need to check whether bogus comes out
			// (e.g., Not found).
			if (header != null) {

				// Header may be a absolute or relative URL
				if (header.startsWith("http:") || header.startsWith("https:")) {
					// absolute URL
					uri = header;
				} else {
					/*
					 * relative URL May be: Gleiche Domäne, Gleiche Ressource,
					 * ein Pfad-Segment-Aufwärts, gleiches Pfad-Segment (see
					 * http://de.wikipedia.org/wiki/Uniform_Resource_Locator#
					 * Relative_URL)
					 */
					if (header.startsWith("/")) {
						String domain = url.getHost();
						String protocol = url.getProtocol();
						uri = protocol + "://" + domain + header;
					} else if (header.startsWith("../")
							|| header.startsWith("./")) {
						// Header only contains the local uri
						// Cut all off until first / from the back
						int index = uri.lastIndexOf("/");
						uri = uri.substring(0, index + 1) + header;
					} else {
						int index = uri.lastIndexOf("/");
						uri = uri.substring(0, index + 1) + header;
					}
				}
			}
			// We should remove # uris
			if (uri.contains("#")) {
				int index = uri.lastIndexOf("#");
				uri = uri.substring(0, index);
			}
		} catch (IOException e) {
			throw new MalformedURLException(e.getMessage());
		}

		Olap4ldUtil._log.config("... result: " + uri);
		return uri;
	}

	/**
	 * I think, caching some sparql results would be very useful.
	 * 
	 * I create a map between hash value of sparql query and the Nodes.
	 * 
	 * If the value is available, I return it.
	 * 
	 * However, when to empty the cache? I empty the cache if I populate a new
	 * cube.
	 * 
	 * @param query
	 * @param caching
	 *            (not used)
	 * @return
	 */
	private List<Node[]> executeSparqlSelectQuery(String query, boolean caching) {

		Olap4ldUtil._log.config("SPARQL query: " + query);

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
			Olap4ldUtil._log.config("NX output: " + test2);
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

	/**
	 * Loads location into store and mark it as stored.
	 * 
	 * @param location
	 */
	private void loadInStore(String location) {
		SailRepositoryConnection con = null;
		try {
			Olap4ldUtil._log.config("Load in store: " + location);

			con = repo.getConnection();
			URL locationurl = new URL(location);

			if (location.endsWith(".rdf") || location.endsWith(".xml")) {
				con.add(locationurl, locationurl.toString(), RDFFormat.RDFXML);
			} else if (location.endsWith(".ttl")) {
				con.add(locationurl, locationurl.toString(), RDFFormat.TURTLE);
			} else {

				// Guess file format
				RDFFormat format = RDFFormat.forFileName(location);
				if (format != null) {
					con.add(locationurl, locationurl.toString(), format);
				} else {
					// Heuristics
					// Check first line
					InputStream is;
					HttpURLConnection connection = (HttpURLConnection) locationurl
							.openConnection();

					// Error
					if (connection.getResponseCode() >= 400) {
						is = connection.getErrorStream();

						BufferedReader rd = new BufferedReader(
								new InputStreamReader(is));

						String response = "";
						String line;
						while ((line = rd.readLine()) != null) {
							response += line;
						}
						Olap4ldUtil._log.config("Error response: " + response);
						rd.close();
						is.close();
					} else {
						is = connection.getInputStream();

						BufferedReader in = new BufferedReader(
								new InputStreamReader(
										connection.getInputStream()));

						String inputLine;
						// Read first line only.
						// while ((inputLine = in.readLine()) != null) {
						// }
						inputLine = in.readLine();
						if (inputLine != null
								&& (inputLine
										.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
										|| inputLine
												.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>") || inputLine
											.startsWith("<"))) {
							con.add(locationurl, locationurl.toString(),
									RDFFormat.RDFXML);
							Olap4ldUtil._log
									.config("Had to guess format to be RDFXML: "
											+ location);
						} else {
							con.add(locationurl, locationurl.toString(),
									RDFFormat.TURTLE);
							Olap4ldUtil._log
									.config("Had to guess format to be Turtle: "
											+ location);
						}
						in.close();
						is.close();
					}

				}
			}

		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RDFParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		loadedMap.put(location.hashCode(), true);

		if (con != null) {
			try {
				con.close();
			} catch (RepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// Log content
		String query = "select * where {?s ?p ?o}";
		Olap4ldUtil._log.config("Check loaded data: " + query);
		executeSparqlSelectQuery(query, false);
	}

	/**
	 * 
	 * @param location
	 * @return boolean value say whether location already loaded
	 */
	private boolean isStored(String location) {
		return this.loadedMap.containsKey(location.hashCode());
	}

	/**
	 * Check whether we query for "Measures".
	 * 
	 * @param dimensionUniqueName
	 * @param hierarchyUniqueName
	 * @param levelUniqueName
	 * @return
	 */
	private boolean isMeasureQueriedForExplicitly(Node dimensionUniqueName,
			Node hierarchyUniqueName, Node levelUniqueName) {
		// If one is set, it should not be Measures, not.
		// Watch out: no square brackets are needed.
		boolean explicitlyStated = (dimensionUniqueName != null && dimensionUniqueName
				.equals(Olap4ldLinkedDataUtil.MEASURE_DIMENSION_NAME))
				|| (hierarchyUniqueName != null && hierarchyUniqueName
						.equals(Olap4ldLinkedDataUtil.MEASURE_DIMENSION_NAME))
				|| (levelUniqueName != null && levelUniqueName
						.equals(Olap4ldLinkedDataUtil.MEASURE_DIMENSION_NAME));

		return explicitlyStated;

	}

	/**
	 * 
	 * @return
	 */
	public List<Node[]> getDatabases(Restrictions restrictions) {

		/*
		 * DISCOVER_DATASOURCES(new MetadataColumn("DataSourceName"), new
		 * MetadataColumn("DataSourceDescription"), new MetadataColumn("URL"),
		 * new MetadataColumn("DataSourceInfo"), new MetadataColumn(
		 * "ProviderName"), new MetadataColumn("ProviderType"), new
		 * MetadataColumn("AuthenticationMode")),
		 */

		List<Node[]> results = new ArrayList<Node[]>();
		Node[] bindingNames = new Node[] { new Variable("?DATA_SOURCE_NAME"),
				new Variable("?DATA_SOURCE_DESCRIPTION"),
				new Variable("?PROVIDER_NAME"), new Variable("?URL"),
				new Variable("?DATA_SOURCE_INFO") };
		results.add(bindingNames);

		Node[] triple = new Node[] { new Literal(DATASOURCENAME),
				new Literal(DATASOURCEDESCRIPTION), new Literal(PROVIDERNAME),
				new Literal(URL), new Literal(DATASOURCEINFO) };
		results.add(triple);
		return results;
	}

	public List<Node[]> getCatalogs(Restrictions restrictions) {
		/*
		 * DBSCHEMA_CATALOGS( new MetadataColumn("CATALOG_NAME"), new
		 * MetadataColumn( "DESCRIPTION"), new MetadataColumn("ROLES"), new
		 * MetadataColumn("DATE_MODIFIED"))
		 */
		List<Node[]> results = new ArrayList<Node[]>();

		Node[] bindingNames = new Node[] { new Variable("?TABLE_CAT") };
		results.add(bindingNames);

		Node[] triple = new Node[] { new Literal(TABLE_CAT) };
		results.add(triple);

		return results;
	}

	/**
	 * 
	 * @return
	 */
	public List<Node[]> getSchemas(Restrictions restrictions) {
		List<Node[]> results = new ArrayList<Node[]>();
		/*
		 * DBSCHEMA_SCHEMATA(new MetadataColumn( "CATALOG_NAME"), new
		 * MetadataColumn("SCHEMA_NAME"), new MetadataColumn("SCHEMA_OWNER"))
		 */
		Node[] bindingNames = new Node[] { new Variable("?TABLE_SCHEM"),
				new Variable("?TABLE_CAT") };
		results.add(bindingNames);

		Node[] triple = new Node[] { new Literal(TABLE_SCHEM),
				new Literal(TABLE_CAT),
				// No owner
				new Literal("") };
		results.add(triple);

		return results;
	}

	/**
	 * 
	 * 
	 * 
	 * Get Cubes from the triple store.
	 * 
	 * Here, the restrictions are strict restrictions without patterns.
	 * 
	 * 
	 * 
	 * @return Node[]{}
	 */
	public List<Node[]> getCubes(Restrictions restrictions)
			throws OlapException {

		checkSufficientInformationGathered(restrictions);

		String additionalFilters = createFilterForRestrictions(restrictions);

		String querytemplate = Olap4ldLinkedDataUtil
				.readInQueryTemplate("sesame_getCubes_regular.txt");
		querytemplate = querytemplate.replace("{{{STANDARDFROM}}}",
				askForFrom(true));
		querytemplate = querytemplate.replace("{{{TABLE_CAT}}}", TABLE_CAT);
		querytemplate = querytemplate.replace("{{{TABLE_SCHEM}}}", TABLE_SCHEM);
		querytemplate = querytemplate.replace("{{{FILTERS}}}",
				additionalFilters);

		List<Node[]> result = executeSparqlSelectQuery(querytemplate, true);

		/*
		 * Check on restrictions that the interface makes:
		 * 
		 * Restrictions are strong restrictions, no fuzzy, since those wild
		 * cards have been eliminated before.
		 */
		// List<Node[]> result = applyRestrictions(cubeUris, restrictions);
		return result;

	}

	/**
	 * Depending on the restrictions, we check whether sufficient Linked Data
	 * has been gathered. If not, we gather more or throw exception.
	 * 
	 * @param restrictions
	 */
	private void checkSufficientInformationGathered(Restrictions restrictions)
			throws OlapException {
		// For now, if only cube is asked for, we load ds and dsd and run checks

		try {
			if (restrictions.cubeNamePattern != null) {
				Node uri = restrictions.cubeNamePattern;

				if (!isStored(uri.toString())) {
					// We also store URI in map
					String location = askForLocation(uri.toString());

					// Here, we always load, since we want to update.
					if (!isStored(location)) {

						// We load the entire cube
						loadCube(uri.toString(), location);

						// We need to materialise implicit information
						runNormalizationAlgorithm();

						// Now that we presumably have loaded all necessary
						// data, we check integrity constraints
						checkIntegrityConstraints();

						// Own checks:
						SailRepositoryConnection con = repo.getConnection();

						String prefixbindings = "PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#> PREFIX skos:    <http://www.w3.org/2004/02/skos/core#> PREFIX qb:      <http://purl.org/linked-data/cube#> PREFIX xsd:     <http://www.w3.org/2001/XMLSchema#> PREFIX owl:     <http://www.w3.org/2002/07/owl#> ";

						// Dataset should have at least one
						// observation
						String testquery = prefixbindings
								+ "ASK { ?obs qb:dataSet ?CUBE_NAME FILTER (?CUBE_NAME = <"
								+ uri + ">)}";
						BooleanQuery booleanQuery = con.prepareBooleanQuery(
								QueryLanguage.SPARQL, testquery);
						if (booleanQuery.evaluate() == false) {
							throw new OlapException(
									"Failed own check: Dataset should have at least one observation. ");
						}

						// XXX Possible other checks
						// No dimensions
						// No aggregation function
						// Code list empty
						// No member

						// Important!
						con.close();

					}
				}
			}
		} catch (MalformedURLException e) {
			throw new OlapException("Problem with malformed url: "
					+ e.getMessage());
		} catch (RepositoryException e) {
			throw new OlapException("Problem with repository: "
					+ e.getMessage());
		} catch (MalformedQueryException e) {
			throw new OlapException("Problem with malformed query: "
					+ e.getMessage());
		} catch (QueryEvaluationException e) {
			throw new OlapException("Problem with query evaluation: "
					+ e.getMessage());
		}
	}

	/**
	 * We load all data for a cube.
	 * 
	 * @param location
	 */
	private void loadCube(String uri, String location) throws OlapException {
		try {
			loadInStore(location);
			// For quicker check, also set loaded uri
			loadedMap.put(uri.hashCode(), true);

			// For everything else: Check whether really cube
			RepositoryConnection con;
			con = repo.getConnection();

			// qb:structure is more robust than a qb:DataSet.
			String testquery = "PREFIX qb: <http://purl.org/linked-data/cube#> ASK { ?CUBE_NAME qb:structure ?dsd. FILTER (?CUBE_NAME = <"
					+ uri + ">)}";
			BooleanQuery booleanQuery = con.prepareBooleanQuery(
					QueryLanguage.SPARQL, testquery);
			boolean isDataset = booleanQuery.evaluate();
			con.close();

			if (!isDataset) {
				throw new OlapException(
						"A cube should be a qb:DataSet and serve via qb:structure a qb:DataStructureDefinition, also this one "
								+ uri + "!");
			} else {

				// If loading ds, also load dsd. Ask for DSD URI and
				// load
				String query = "PREFIX qb: <http://purl.org/linked-data/cube#> SELECT ?dsd WHERE {<"
						+ uri + "> qb:structure ?dsd}";
				List<Node[]> dsd = executeSparqlSelectQuery(query, true);
				// There should be a dsd
				// Note in spec:
				// "Every qb:DataSet has exactly one associated qb:DataStructureDefinition."
				if (dsd.size() <= 1) {
					throw new OlapException(
							"A cube should serve a data structure definition!");
				} else {
					// Get the second
					String dsduri = dsd.get(1)[0].toString();

					location = askForLocation(dsduri);

					// Since ds and dsd location can be the same,
					// check
					// whether loaded already
					if (!isStored(location)) {
						loadInStore(location);
						// For quicker check, also add dsduri
						loadedMap.put(dsduri.hashCode(), true);
					}
				}

				// If loading ds, also load measures
				query = "PREFIX qb: <http://purl.org/linked-data/cube#> SELECT ?measure WHERE {<"
						+ uri
						+ "> qb:structure ?dsd. ?dsd qb:component ?comp. ?comp qb:measure ?measure}";
				List<Node[]> measures = executeSparqlSelectQuery(query, true);
				// There should be a dsd
				// Note in spec:
				// "Every qb:DataSet has exactly one associated qb:DataStructureDefinition."
				if (measures.size() <= 1) {
					throw new OlapException("A cube should serve a measure!");
				} else {
					boolean first = true;
					for (Node[] nodes : measures) {
						if (first) {
							first = false;
							continue;
						}
						String measureuri = nodes[0].toString();

						location = askForLocation(measureuri);

						// Since ds and dsd location can be the
						// same,
						// check
						// whether loaded already
						if (!isStored(location)) {
							loadInStore(location);
							// For quicker check, also add dsduri
							loadedMap.put(measureuri.hashCode(), true);
						}
					}
				}

				// If loading ds, also load dimensions
				query = "PREFIX qb: <http://purl.org/linked-data/cube#> SELECT ?dimension WHERE {<"
						+ uri
						+ "> qb:structure ?dsd. ?dsd qb:component ?comp. ?comp qb:dimension ?dimension}";
				List<Node[]> dimensions = executeSparqlSelectQuery(query, true);
				// There should be a dsd
				// Note in spec:
				// "Every qb:DataSet has exactly one associated qb:DataStructureDefinition."
				if (dimensions.size() <= 1) {
					throw new OlapException("A cube should serve a dimension!");
				} else {
					boolean first = true;
					for (Node[] nodes : dimensions) {
						if (first) {
							first = false;
							continue;
						}
						String dimensionuri = nodes[0].toString();

						location = askForLocation(dimensionuri);

						// Since ds and dsd location can be the
						// same,
						// check
						// whether loaded already
						if (!isStored(location)) {
							loadInStore(location);
							// For quicker check, also add dsduri
							loadedMap.put(dimensionuri.hashCode(), true);
						}
					}
				}

				// If loading ds, also load codelists
				query = "PREFIX qb: <http://purl.org/linked-data/cube#> SELECT ?codelist WHERE {<"
						+ uri
						+ "> qb:structure ?dsd. ?dsd qb:component ?comp. ?comp qb:dimension ?dimension. ?dimension qb:codeList ?codelist}";
				List<Node[]> codelists = executeSparqlSelectQuery(query, true);
				// There should be a dsd
				// Note in spec:
				// "Every qb:DataSet has exactly one associated qb:DataStructureDefinition."
				if (codelists.size() <= 1) {
					;
				} else {
					boolean first = true;
					for (Node[] nodes : codelists) {
						if (first) {
							first = false;
							continue;
						}

						String codelisturi = nodes[0].toString();

						location = askForLocation(codelisturi);

						// Since ds and dsd location can be the
						// same,
						// check
						// whether loaded already
						if (!isStored(location)) {
							loadInStore(location);
							// For quicker check, also add dsduri
							loadedMap.put(codelisturi.hashCode(), true);
						}
					}
				}
			}
		} catch (RepositoryException e) {
			throw new OlapException("Problem with repository: "
					+ e.getMessage());
		} catch (QueryEvaluationException e) {
			throw new OlapException("Problem with query evaluation: "
					+ e.getMessage());
		} catch (MalformedQueryException e) {
			throw new OlapException("Problem with malformed query: "
					+ e.getMessage());
		} catch (MalformedURLException e) {
			throw new OlapException("Problem with malformed url: "
					+ e.getMessage());
		}
	}

	private void checkIntegrityConstraints() throws OlapException {

		try {
			// Now, we check the integrity constraints
			RepositoryConnection con;
			con = repo.getConnection();

			String testquery;
			BooleanQuery booleanQuery;

			boolean error = false;
			String overview = "";

			// Each integrity constraint query assumes the following set of
			// prefix
			// bindings:
			String prefixbindings = "PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#> PREFIX skos:    <http://www.w3.org/2004/02/skos/core#> PREFIX qb:      <http://purl.org/linked-data/cube#> PREFIX xsd:     <http://www.w3.org/2001/XMLSchema#> PREFIX owl:     <http://www.w3.org/2002/07/owl#> ";

			// IC-1. Unique DataSet. Every qb:Observation
			// has exactly one associated qb:DataSet. <=
			// takes too long since every observation tested
			// testquery = prefixbindings
			// +
			// "ASK {  {        ?obs a qb:Observation .    FILTER NOT EXISTS { ?obs qb:dataSet ?dataset1 . } } UNION {        ?obs a qb:Observation ;       qb:dataSet ?dataset1, ?dataset2 .    FILTER (?dataset1 != ?dataset2)  }}";
			// BooleanQuery booleanQuery = con.prepareBooleanQuery(
			// QueryLanguage.SPARQL, testquery);
			// if (booleanQuery.evaluate() == true) {
			// error = true;
			// overview +=
			// "Failed specification check: IC-1. Unique DataSet. Every qb:Observation has exactly one associated qb:DataSet. \n";
			// } else {
			// overview +=
			// "Successful specification check: IC-1. Unique DataSet. Every qb:Observation has exactly one associated qb:DataSet. \n";
			// }

			// IC-2. Unique DSD. Every qb:DataSet has
			// exactly one associated
			// qb:DataStructureDefinition. <= tested before
			testquery = prefixbindings
					+ "ASK {  {        ?dataset a qb:DataSet .    FILTER NOT EXISTS { ?dataset qb:structure ?dsd . }  } UNION {    ?dataset a qb:DataSet ;       qb:structure ?dsd1, ?dsd2 .    FILTER (?dsd1 != ?dsd2)  }}";
			booleanQuery = con.prepareBooleanQuery(QueryLanguage.SPARQL,
					testquery);
			if (booleanQuery.evaluate() == true) {
				error = true;
				overview += "Failed specification check: IC-2. Unique DSD. Every qb:DataSet has exactly one associated qb:DataStructureDefinition.  \n";
			} else {
				overview += "Successful specification check: IC-2. Unique DSD. Every qb:DataSet has exactly one associated qb:DataStructureDefinition. \n";
			}

			// IC-3. DSD includes measure
			// XXX: Not fully like in spec
			testquery = prefixbindings
					+ "ASK {  ?dsd a qb:DataStructureDefinition .  FILTER NOT EXISTS { ?dsd qb:component [qb:componentProperty [a qb:MeasureProperty]] }}";
			booleanQuery = con.prepareBooleanQuery(QueryLanguage.SPARQL,
					testquery);
			if (booleanQuery.evaluate() == true) {
				error = true;
				overview += "Failed specification check: IC-3. DSD includes measure. Every qb:DataStructureDefinition must include at least one declared measure. \n";
			} else {
				overview += "Successful specification check: IC-3. DSD includes measure. Every qb:DataStructureDefinition must include at least one declared measure. \n";
			}

			// IC-4. Dimensions have range
			testquery = prefixbindings
					+ "ASK { ?dim a qb:DimensionProperty . FILTER NOT EXISTS { ?dim rdfs:range [] }}";
			booleanQuery = con.prepareBooleanQuery(QueryLanguage.SPARQL,
					testquery);
			if (booleanQuery.evaluate() == true) {
				error = true;
				overview += "Failed specification check: IC-4. Dimensions have range. Every dimension declared in a qb:DataStructureDefinition must have a declared rdfs:range.\n";
			} else {
				overview += "Successful specification check: IC-4. Dimensions have range. Every dimension declared in a qb:DataStructureDefinition must have a declared rdfs:range. \n";
			}

			// IC-5. Concept dimensions have code lists
			testquery = prefixbindings
					+ "ASK { ?dim a qb:DimensionProperty ; rdfs:range skos:Concept . FILTER NOT EXISTS { ?dim qb:codeList [] }}";
			booleanQuery = con.prepareBooleanQuery(QueryLanguage.SPARQL,
					testquery);
			if (booleanQuery.evaluate() == true) {
				error = true;
				overview += "Failed specification check: IC-5. Concept dimensions have code lists. Every dimension with range skos:Concept must have a qb:codeList.  \n";
			} else {
				overview += "Successful specification check: IC-5. Concept dimensions have code lists. Every dimension with range skos:Concept must have a qb:codeList.  \n";
			}

			// IC-6. Only attributes may be optional <= not
			// important right now. We do not regard
			// attributes.
			testquery = prefixbindings
					+ "ASK {  ?dsd qb:component ?componentSpec .  ?componentSpec qb:componentRequired \"false\"^^xsd:boolean ;                 qb:componentProperty ?component .  FILTER NOT EXISTS { ?component a qb:AttributeProperty }} ";
			booleanQuery = con.prepareBooleanQuery(QueryLanguage.SPARQL,
					testquery);
			if (booleanQuery.evaluate() == true) {
				error = true;
				overview += "Failed specification check: IC-6. Only attributes may be optional. The only components of a qb:DataStructureDefinition that may be marked as optional, using qb:componentRequired are attributes.\n";
			} else {
				overview += "Successful specification check: IC-6. Only attributes may be optional. The only components of a qb:DataStructureDefinition that may be marked as optional, using qb:componentRequired are attributes. \n";
			}

			// IC-7. Slice Keys must be declared <= not
			// important right now. We do not regard slices.
			testquery = prefixbindings
					+ "ASK {    ?sliceKey a qb:SliceKey .    FILTER NOT EXISTS { [a qb:DataStructureDefinition] qb:sliceKey ?sliceKey }}";
			booleanQuery = con.prepareBooleanQuery(QueryLanguage.SPARQL,
					testquery);
			if (booleanQuery.evaluate() == true) {
				error = true;
				overview += "Failed specification check: IC-7. Slice Keys must be declared. Every qb:SliceKey must be associated with a qb:DataStructureDefinition. \n";
			} else {
				overview += "Successful specification check: IC-7. Slice Keys must be declared. Every qb:SliceKey must be associated with a qb:DataStructureDefinition. \n";
			}

			// IC-8. Slice Keys consistent with DSD
			testquery = prefixbindings
					+ "ASK {  ?slicekey a qb:SliceKey;      qb:componentProperty ?prop .  ?dsd qb:sliceKey ?sliceKey .  FILTER NOT EXISTS { ?dsd qb:component [qb:componentProperty ?prop] }}";
			booleanQuery = con.prepareBooleanQuery(QueryLanguage.SPARQL,
					testquery);
			if (booleanQuery.evaluate() == true) {
				error = true;
				overview += "Failed specification check: IC-8. Slice Keys consistent with DSD. Every qb:componentProperty on a qb:SliceKey must also be declared as a qb:component of the associated qb:DataStructureDefinition. \n";
			} else {
				overview += "Successful specification check: IC-8. Slice Keys consistent with DSD. Every qb:componentProperty on a qb:SliceKey must also be declared as a qb:component of the associated qb:DataStructureDefinition.  \n";
			}

			// IC-9. Unique slice structure
			testquery = prefixbindings
					+ "ASK {  {    ?slice a qb:Slice .    FILTER NOT EXISTS { ?slice qb:sliceStructure ?key } } UNION {    ?slice a qb:Slice ;           qb:sliceStructure ?key1, ?key2;    FILTER (?key1 != ?key2)  }}";
			booleanQuery = con.prepareBooleanQuery(QueryLanguage.SPARQL,
					testquery);
			if (booleanQuery.evaluate() == true) {
				error = true;
				overview += "Failed specification check: IC-9. Unique slice structure. Each qb:Slice must have exactly one associated qb:sliceStructure.  \n";
			} else {
				overview += "Successful specification check: IC-1. IC-9. Unique slice structure. Each qb:Slice must have exactly one associated qb:sliceStructure.  \n";
			}

			// IC-10. Slice dimensions complete
			testquery = prefixbindings
					+ "ASK {  ?slice qb:sliceStructure [qb:componentProperty ?dim] .  FILTER NOT EXISTS { ?slice ?dim [] }}";
			booleanQuery = con.prepareBooleanQuery(QueryLanguage.SPARQL,
					testquery);
			if (booleanQuery.evaluate() == true) {
				error = true;
				overview += "Failed specification check: IC-10. Slice dimensions complete. Every qb:Slice must have a value for every dimension declared in its qb:sliceStructure. \n";
			} else {
				overview += "Successful specification check: IC-10. Slice dimensions complete. Every qb:Slice must have a value for every dimension declared in its qb:sliceStructure. \n";
			}

			// IC-11. All dimensions required <= takes too
			// long
			// testquery = prefixbindings
			// +
			// "ASK {    ?obs qb:dataSet/qb:structure/qb:component/qb:componentProperty ?dim .    ?dim a qb:DimensionProperty;    FILTER NOT EXISTS { ?obs ?dim [] }}";
			// booleanQuery = con.prepareBooleanQuery(QueryLanguage.SPARQL,
			// testquery);
			// if (booleanQuery.evaluate() == true) {
			// error = true;
			// overview +=
			// "Failed specification check: IC-11. All dimensions required. Every qb:Observation has a value for each dimension declared in its associated qb:DataStructureDefinition.  \n";
			// } else {
			// overview +=
			// "Successful specification check: IC-11. All dimensions required. Every qb:Observation has a value for each dimension declared in its associated qb:DataStructureDefinition.  \n";
			// }

			// IC-12. No duplicate observations <= takes too
			// long
			// testquery = prefixbindings
			// +
			// "ASK {  FILTER( ?allEqual )  {    SELECT (MIN(?equal) AS ?allEqual) WHERE {        ?obs1 qb:dataSet ?dataset .        ?obs2 qb:dataSet ?dataset .        FILTER (?obs1 != ?obs2)        ?dataset qb:structure/qb:component/qb:componentProperty ?dim .        ?dim a qb:DimensionProperty .        ?obs1 ?dim ?value1 .        ?obs2 ?dim ?value2 .        BIND( ?value1 = ?value2 AS ?equal)    } GROUP BY ?obs1 ?obs2  }}";
			// booleanQuery = con.prepareBooleanQuery(QueryLanguage.SPARQL,
			// testquery);
			// if (booleanQuery.evaluate() == true) {
			// error = true;
			// overview +=
			// "Failed specification check: IC-12. No duplicate observations. No two qb:Observations in the same qb:DataSet may have the same value for all dimensions. \n";
			// } else {
			// overview +=
			// "Successful specification check: IC-12. No duplicate observations. No two qb:Observations in the same qb:DataSet may have the same value for all dimensions. \n";
			// }

			// IC-13. Required attributes <= We do not
			// regard attributes
			testquery = prefixbindings
					+ "ASK { ?obs qb:dataSet/qb:structure/qb:component ?component .   ?component qb:componentRequired \"true\"^^xsd:boolean ;               qb:componentProperty ?attr .    FILTER NOT EXISTS { ?obs ?attr [] }}";
			booleanQuery = con.prepareBooleanQuery(QueryLanguage.SPARQL,
					testquery);
			if (booleanQuery.evaluate() == true) {
				error = true;
				overview += "Failed specification check: IC-13. Required attributes. Every qb:Observation has a value for each declared attribute that is marked as required. \n";
			} else {
				overview += "Successful specification check: IC-13. Required attributes. Every qb:Observation has a value for each declared attribute that is marked as required.  \n";
			}

			// IC-14. All measures present
			testquery = prefixbindings
					+ "ASK { ?obs qb:dataSet/qb:structure ?dsd . FILTER NOT EXISTS { ?dsd qb:component/qb:componentProperty qb:measureType } ?dsd qb:component/qb:componentProperty ?measure . ?measure a qb:MeasureProperty; FILTER NOT EXISTS { ?obs ?measure [] }}";
			booleanQuery = con.prepareBooleanQuery(QueryLanguage.SPARQL,
					testquery);
			if (booleanQuery.evaluate() == true) {
				error = true;
				overview += "Failed specification check: IC-14. All measures present. In a qb:DataSet which does not use a Measure dimension then each individual qb:Observation must have a value for every declared measure. \n";
			} else {
				overview += "Successful specification check: IC-14. All measures present. In a qb:DataSet which does not use a Measure dimension then each individual qb:Observation must have a value for every declared measure. \n";
			}

			// IC-15. Measure dimension consistent <= We do
			// not support measureType, yet.
			testquery = prefixbindings
					+ "ASK {    ?obs qb:dataSet/qb:structure ?dsd ;         qb:measureType ?measure .    ?dsd qb:component/qb:componentProperty qb:measureType .    FILTER NOT EXISTS { ?obs ?measure [] }}";
			booleanQuery = con.prepareBooleanQuery(QueryLanguage.SPARQL,
					testquery);
			if (booleanQuery.evaluate() == true) {
				error = true;
				overview += "Failed specification check: IC-15. Measure dimension consistent. In a qb:DataSet which uses a Measure dimension then each qb:Observation must have a value for the measure corresponding to its given qb:measureType. \n";
			} else {
				overview += "Successful specification check: IC-15. Measure dimension consistent. In a qb:DataSet which uses a Measure dimension then each qb:Observation must have a value for the measure corresponding to its given qb:measureType. \n";
			}

			// IC-16. Single measure on measure dimension
			// observation
			testquery = prefixbindings
					+ "ASK {    ?obs qb:dataSet/qb:structure ?dsd ;         qb:measureType ?measure ;         ?omeasure [] .    ?dsd qb:component/qb:componentProperty qb:measureType ;         qb:component/qb:componentProperty ?omeasure .    ?omeasure a qb:MeasureProperty .        FILTER (?omeasure != ?measure)}";
			booleanQuery = con.prepareBooleanQuery(QueryLanguage.SPARQL,
					testquery);
			if (booleanQuery.evaluate() == true) {
				error = true;
				overview += "Failed specification check: IC-16. Single measure on measure dimension observation. In a qb:DataSet which uses a Measure dimension then each qb:Observation must only have a value for one measure (by IC-15 this will be the measure corresponding to its qb:measureType). \n";
			} else {
				overview += "Successful specification check: IC-16. Single measure on measure dimension observation. In a qb:DataSet which uses a Measure dimension then each qb:Observation must only have a value for one measure (by IC-15 this will be the measure corresponding to its qb:measureType).  \n";
			}

			// IC-17. All measures present in measures dimension cube
			testquery = prefixbindings
					+ "ASK { {      SELECT ?numMeasures (COUNT(?obs2) AS ?count) WHERE {         {             SELECT ?dsd (COUNT(?m) AS ?numMeasures) WHERE {                 ?dsd qb:component/qb:componentProperty ?m.                  ?m a qb:MeasureProperty .              } GROUP BY ?dsd          }                  ?obs1 qb:dataSet/qb:structure ?dsd;                qb:dataSet ?dataset ;                qb:measureType ?m1 .              ?obs2 qb:dataSet ?dataset ;                qb:measureType ?m2 .          FILTER NOT EXISTS {              ?dsd qb:component/qb:componentProperty ?dim .              FILTER (?dim != qb:measureType)              ?dim a qb:DimensionProperty .              ?obs1 ?dim ?v1 .              ?obs2 ?dim ?v2.              FILTER (?v1 != ?v2)          }                } GROUP BY ?obs1 ?numMeasures        HAVING (?count != ?numMeasures)  }}";
			booleanQuery = con.prepareBooleanQuery(QueryLanguage.SPARQL,
					testquery);
			if (booleanQuery.evaluate() == true) {
				error = true;
				overview += "Failed specification check: IC-17. All measures present in measures dimension cube. In a qb:DataSet which uses a Measure dimension then if there is a Observation for some combination of non-measure dimensions then there must be other Observations with the same non-measure dimension values for each of the declared measures. \n";
			} else {
				overview += "Successful specification check: IC-17. All measures present in measures dimension cube. In a qb:DataSet which uses a Measure dimension then if there is a Observation for some combination of non-measure dimensions then there must be other Observations with the same non-measure dimension values for each of the declared measures. \n";
			}

			// Important!
			con.close();

			// Logging
			Olap4ldUtil._log.config("Integrity constraints overview: "
					+ overview);

			if (error) {
				throw new UnsupportedOperationException(
						"Integrity constraints overview: \n" + overview);
			}

		} catch (RepositoryException e) {
			throw new OlapException("Problem with repository: "
					+ e.getMessage());
		} catch (MalformedQueryException e) {
			throw new OlapException("Problem with malformed query: "
					+ e.getMessage());
		} catch (QueryEvaluationException e) {
			throw new OlapException("Problem with query evaluation: "
					+ e.getMessage());
		}
	}

	private void runNormalizationAlgorithm() throws OlapException {

		try {
			RepositoryConnection con;

			con = repo.getConnection();

			// First, we run normalization algorithm
			String updateQuery = "PREFIX rdf:            <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX qb:             <http://purl.org/linked-data/cube#> INSERT { ?o rdf:type qb:Observation .} WHERE {    [] qb:observation ?o .}; INSERT { ?o rdf:type qb:Observation .} WHERE { ?o qb:dataSet [] .}; INSERT {    ?s rdf:type qb:Slice . } WHERE {  [] qb:slice ?s.}; INSERT {    ?cs qb:componentProperty ?p .    ?p  rdf:type qb:DimensionProperty .} WHERE {    ?cs qb:dimension ?p .}; INSERT {    ?cs qb:componentProperty ?p .    ?p  rdf:type qb:MeasureProperty .} WHERE {    ?cs qb:measure ?p .};INSERT {    ?cs qb:componentProperty ?p .    ?p  rdf:type qb:AttributeProperty .} WHERE {    ?cs qb:attribute ?p .}";
			Update updateQueryQuery = con.prepareUpdate(QueryLanguage.SPARQL,
					updateQuery);
			updateQueryQuery.execute();

			// # Dataset attachments
			updateQuery = "PREFIX qb:             <http://purl.org/linked-data/cube#> INSERT {    ?obs  ?comp ?value} WHERE {    ?spec    qb:componentProperty ?comp ;            qb:componentAttachment qb:DataSet .    ?dataset qb:structure [qb:component ?spec];             ?comp ?value .    ?obs     qb:dataSet ?dataset.};";
			con.prepareUpdate(QueryLanguage.SPARQL, updateQuery);
			updateQueryQuery = con.prepareUpdate(QueryLanguage.SPARQL,
					updateQuery);
			updateQueryQuery.execute();

			// # Slice attachments
			updateQuery = "PREFIX qb:             <http://purl.org/linked-data/cube#> INSERT {    ?obs  ?comp ?value} WHERE {    ?spec    qb:componentProperty ?comp;             qb:componentAttachment qb:Slice .    ?dataset qb:structure [qb:component ?spec];             qb:slice ?slice .    ?slice ?comp ?value;           qb:observation ?obs .};";
			con.prepareUpdate(QueryLanguage.SPARQL, updateQuery);
			updateQueryQuery = con.prepareUpdate(QueryLanguage.SPARQL,
					updateQuery);
			updateQueryQuery.execute();

			// # Dimension values on slices
			updateQuery = "PREFIX qb:             <http://purl.org/linked-data/cube#> INSERT {    ?obs  ?comp ?value} WHERE {    ?spec    qb:componentProperty ?comp .    ?comp a  qb:DimensionProperty .    ?dataset qb:structure [qb:component ?spec];             qb:slice ?slice .    ?slice ?comp ?value;           qb:observation ?obs .}";
			con.prepareUpdate(QueryLanguage.SPARQL, updateQuery);
			updateQueryQuery = con.prepareUpdate(QueryLanguage.SPARQL,
					updateQuery);
			updateQueryQuery.execute();

			// Important!
			con.close();
		} catch (RepositoryException e) {
			throw new OlapException("Problem with repository: "
					+ e.getMessage());
		} catch (MalformedQueryException e) {
			throw new OlapException("Problem with malformed query: "
					+ e.getMessage());
		} catch (UpdateExecutionException e) {
			throw new OlapException("Problem with update execution: "
					+ e.getMessage());
		}
	}

	/**
	 * Get possible dimensions (component properties) for each cube from the
	 * triple store.
	 * 
	 * Approach: I create the output from Linked Data, and then I filter it
	 * using the restrictions.
	 * 
	 * I have to also return the Measures dimension for each cube.
	 * 
	 * @return Node[]{?dsd ?dimension ?compPropType ?name}
	 * @throws MalformedURLException
	 */
	public List<Node[]> getDimensions(Restrictions restrictions)
			throws OlapException {

		checkSufficientInformationGathered(restrictions);

		String additionalFilters = createFilterForRestrictions(restrictions);

		List<Node[]> result = new ArrayList<Node[]>();

		// Create header
		Node[] header = new Node[] { new Variable("?CATALOG_NAME"),
				new Variable("?SCHEMA_NAME"), new Variable("?CUBE_NAME"),
				new Variable("?DIMENSION_NAME"),
				new Variable("?DIMENSION_UNIQUE_NAME"),
				new Variable("?DIMENSION_CAPTION"),
				new Variable("?DIMENSION_ORDINAL"),
				new Variable("?DIMENSION_TYPE"), new Variable("?DESCRIPTION") };
		result.add(header);

		if (!isMeasureQueriedForExplicitly(restrictions.dimensionUniqueName,
				restrictions.hierarchyUniqueName, restrictions.levelUniqueName)) {

			// Get all dimensions
			String querytemplate = Olap4ldLinkedDataUtil
					.readInQueryTemplate("sesame_getDimensions_regular.txt");
			querytemplate = querytemplate.replace("{{{STANDARDFROM}}}",
					askForFrom(true));
			querytemplate = querytemplate.replace("{{{TABLE_CAT}}}", TABLE_CAT);
			querytemplate = querytemplate.replace("{{{TABLE_SCHEM}}}",
					TABLE_SCHEM);
			querytemplate = querytemplate.replace("{{{FILTERS}}}",
					additionalFilters);

			List<Node[]> myresult = executeSparqlSelectQuery(querytemplate, true);
			// Add all of result2 to result
			boolean first = true;
			for (Node[] nodes : myresult) {
				if (first) {
					first = false;
					continue;
				}
				result.add(nodes);
			}
		}

		// We try to find measures
		if (true) {

			// In this case, we do ask for a measure dimension.
			String querytemplate = Olap4ldLinkedDataUtil
					.readInQueryTemplate("sesame_getDimensions_measure_dimension.txt");
			querytemplate = querytemplate.replace("{{{STANDARDFROM}}}",
					askForFrom(true));
			querytemplate = querytemplate.replace("{{{TABLE_CAT}}}", TABLE_CAT);
			querytemplate = querytemplate.replace("{{{TABLE_SCHEM}}}",
					TABLE_SCHEM);
			querytemplate = querytemplate.replace("{{{FILTERS}}}",
					additionalFilters);

			List<Node[]> myresult = executeSparqlSelectQuery(querytemplate, true);

			// List<Node[]> result2 = applyRestrictions(memberUris2,
			// restrictions);

			// Add all of result2 to result
			boolean first = true;
			for (Node[] nodes : myresult) {
				if (first) {
					first = false;
					continue;
				}
				result.add(nodes);
			}
		}

		return result;
	}

	/**
	 * Every measure also needs to be listed as member. When I create the dsd, I
	 * add obsValue as a dimension, but also as a measure. However, members of
	 * the measure dimension would typically all be named differently from the
	 * measure (e.g., obsValue5), therefore, we do not find a match. The problem
	 * is, that getMembers() has to return the measures. So, either, in the dsd,
	 * we need to add a dimension with the measure as a member, or, the query
	 * for the members should return for measures the measure property as
	 * member.
	 * 
	 * 
	 * Here, all the measure properties are returned.
	 * 
	 * @param context
	 * @param metadataRequest
	 * @param restrictions
	 * @return
	 */
	public List<Node[]> getMeasures(Restrictions restrictions)
			throws OlapException {

		checkSufficientInformationGathered(restrictions);

		String additionalFilters = createFilterForRestrictions(restrictions);

		// ///////////QUERY//////////////////////////
		/*
		 * TODO: How to consider equal measures?
		 */

		// Boolean values need to be returned as "true" or "false".
		// Get all measures
		String querytemplate = Olap4ldLinkedDataUtil
				.readInQueryTemplate("sesame_getMeasures.txt");
		querytemplate = querytemplate.replace("{{{STANDARDFROM}}}",
				askForFrom(true));
		querytemplate = querytemplate.replace("{{{TABLE_CAT}}}", TABLE_CAT);
		querytemplate = querytemplate.replace("{{{TABLE_SCHEM}}}", TABLE_SCHEM);
		querytemplate = querytemplate.replace("{{{FILTERS}}}",
				additionalFilters);

		List<Node[]> result = executeSparqlSelectQuery(querytemplate, true);

		// List<Node[]> result = applyRestrictions(measureUris, restrictions);
		return result;
	}

	/**
	 * 
	 * Return hierarchies
	 * 
	 * @param context
	 * @param metadataRequest
	 * @param restrictions
	 * @return
	 */
	public List<Node[]> getHierarchies(Restrictions restrictions)
			throws OlapException {

		checkSufficientInformationGathered(restrictions);

		String additionalFilters = createFilterForRestrictions(restrictions);

		List<Node[]> result = new ArrayList<Node[]>();

		// Create header
		Node[] header = new Node[] { new Variable("?CATALOG_NAME"),
				new Variable("?SCHEMA_NAME"), new Variable("?CUBE_NAME"),
				new Variable("?DIMENSION_UNIQUE_NAME"),
				new Variable("?HIERARCHY_UNIQUE_NAME"),
				new Variable("?HIERARCHY_NAME"),
				new Variable("?HIERARCHY_CAPTION"),
				new Variable("?DESCRIPTION") };
		result.add(header);

		if (!isMeasureQueriedForExplicitly(restrictions.dimensionUniqueName,
				restrictions.hierarchyUniqueName, restrictions.levelUniqueName)) {

			// Get all hierarchies with codeLists
			String querytemplate = Olap4ldLinkedDataUtil
					.readInQueryTemplate("sesame_getHierarchies_regular.txt");
			querytemplate = querytemplate.replace("{{{STANDARDFROM}}}",
					askForFrom(true));
			querytemplate = querytemplate.replace("{{{TABLE_CAT}}}", TABLE_CAT);
			querytemplate = querytemplate.replace("{{{TABLE_SCHEM}}}",
					TABLE_SCHEM);
			querytemplate = querytemplate.replace("{{{FILTERS}}}",
					additionalFilters);

			List<Node[]> myresult = executeSparqlSelectQuery(querytemplate, true);

			boolean first = true;
			for (Node[] nodes : myresult) {
				if (first) {
					first = false;
					continue;
				}
				result.add(nodes);
			}

		}

		// List<Node[]> result = applyRestrictions(hierarchyResults,
		// restrictions);

		// Try to find measure dimensions.
		if (true) {

			// In this case, we do ask for a measure hierarchy.
			String querytemplate = Olap4ldLinkedDataUtil
					.readInQueryTemplate("sesame_getHierarchies_measure_dimension.txt");
			querytemplate = querytemplate.replace("{{{STANDARDFROM}}}",
					askForFrom(true));
			querytemplate = querytemplate.replace("{{{TABLE_CAT}}}", TABLE_CAT);
			querytemplate = querytemplate.replace("{{{TABLE_SCHEM}}}",
					TABLE_SCHEM);
			querytemplate = querytemplate.replace("{{{FILTERS}}}",
					additionalFilters);

			List<Node[]> myresult = executeSparqlSelectQuery(querytemplate, true);

			// List<Node[]> result2 = applyRestrictions(memberUris2,
			// restrictions);

			// Add all of result2 to result
			boolean first = true;
			for (Node[] nodes : myresult) {
				if (first) {
					first = false;
					continue;
				}
				result.add(nodes);
			}
		}

		// Get dimension hierarchies without codeList, but only if hierarchy is
		// not set and different from dimension unique name
		/*
		 * * Note in spec:
		 * "Every dimension declared in a qb:DataStructureDefinition must have a declared rdfs:range."
		 * Note in spec:
		 * "Every dimension with range skos:Concept must have a qb:codeList." <=
		 * This means, we do not necessarily need a code list in many cases.
		 * But, if we have a code list, then: "If a dimension property has a
		 * qb:codeList, then the value of the dimension property on every
		 * qb:Observation must be in the code list."
		 */
		if (!isMeasureQueriedForExplicitly(restrictions.dimensionUniqueName,
				restrictions.hierarchyUniqueName, restrictions.levelUniqueName)) {

			String querytemplate = Olap4ldLinkedDataUtil
					.readInQueryTemplate("sesame_getHierarchies_without_codelist.txt");
			querytemplate = querytemplate.replace("{{{STANDARDFROM}}}",
					askForFrom(true));
			querytemplate = querytemplate.replace("{{{TABLE_CAT}}}", TABLE_CAT);
			querytemplate = querytemplate.replace("{{{TABLE_SCHEM}}}",
					TABLE_SCHEM);
			querytemplate = querytemplate.replace("{{{FILTERS}}}",
					additionalFilters);

			List<Node[]> myresult = executeSparqlSelectQuery(querytemplate, true);

			// List<Node[]> result3 = applyRestrictions(memberUris3,
			// restrictions);

			// Add all of result2 to result
			boolean first = true;
			for (Node[] nodes : myresult) {
				if (first) {
					first = false;
					continue;
				}
				result.add(nodes);
			}
		}

		return result;
	}

	/**
	 * 
	 * @param context
	 * @param metadataRequest
	 * @param restrictions
	 * @return
	 */
	public List<Node[]> getLevels(Restrictions restrictions)
			throws OlapException {

		checkSufficientInformationGathered(restrictions);

		String additionalFilters = createFilterForRestrictions(restrictions);

		List<Node[]> result = new ArrayList<Node[]>();

		// Create header
		Node[] header = new Node[] { new Variable("?CATALOG_NAME"),
				new Variable("?SCHEMA_NAME"), new Variable("?CUBE_NAME"),
				new Variable("?DIMENSION_UNIQUE_NAME"),
				new Variable("?HIERARCHY_UNIQUE_NAME"),
				new Variable("?LEVEL_UNIQUE_NAME"),
				new Variable("?LEVEL_CAPTION"), new Variable("?LEVEL_NAME"),
				new Variable("?DESCRIPTION"), new Variable("?LEVEL_NUMBER"),
				new Variable("?LEVEL_CARDINALITY"), new Variable("?LEVEL_TYPE") };
		result.add(header);

		if (!isMeasureQueriedForExplicitly(restrictions.dimensionUniqueName,
				restrictions.hierarchyUniqueName, restrictions.levelUniqueName)) {

			// TODO: Add regularly modeled levels (without using xkos)
			String querytemplate = Olap4ldLinkedDataUtil
					.readInQueryTemplate("sesame_getLevels_regular.txt");
			querytemplate = querytemplate.replace("{{{STANDARDFROM}}}",
					askForFrom(true));
			querytemplate = querytemplate.replace("{{{TABLE_CAT}}}", TABLE_CAT);
			querytemplate = querytemplate.replace("{{{TABLE_SCHEM}}}",
					TABLE_SCHEM);
			querytemplate = querytemplate.replace("{{{FILTERS}}}",
					additionalFilters);

			List<Node[]> myresult = executeSparqlSelectQuery(querytemplate, true);
			// Add all of result2 to result
			boolean first = true;
			for (Node[] nodes : myresult) {
				if (first) {
					first = false;
					continue;
				}
				result.add(nodes);
			}

			// Get all levels of code lists using xkos
			// TODO: LEVEL_CARDINALITY is not solved, yet.
			querytemplate = Olap4ldLinkedDataUtil
					.readInQueryTemplate("sesame_getLevels_xkos.txt");
			querytemplate = querytemplate.replace("{{{STANDARDFROM}}}",
					askForFrom(true));
			querytemplate = querytemplate.replace("{{{TABLE_CAT}}}", TABLE_CAT);
			querytemplate = querytemplate.replace("{{{TABLE_SCHEM}}}",
					TABLE_SCHEM);
			querytemplate = querytemplate.replace("{{{FILTERS}}}",
					additionalFilters);

			myresult = executeSparqlSelectQuery(querytemplate, true);

			// Add all of result2 to result
			first = true;
			for (Node[] nodes : myresult) {
				if (first) {
					first = false;
					continue;
				}
				result.add(nodes);
			}

		}

		// Distinct for several measures per cube.
		// Add measures levels
		// Second, ask for the measures (which are also members), but only if
		// measure

		if (true) {

			// In this case, we do ask for a measure dimension.
			String querytemplate = Olap4ldLinkedDataUtil
					.readInQueryTemplate("sesame_getLevels_measure_dimension.txt");
			querytemplate = querytemplate.replace("{{{STANDARDFROM}}}",
					askForFrom(true));
			querytemplate = querytemplate.replace("{{{TABLE_CAT}}}", TABLE_CAT);
			querytemplate = querytemplate.replace("{{{TABLE_SCHEM}}}",
					TABLE_SCHEM);
			querytemplate = querytemplate.replace("{{{FILTERS}}}",
					additionalFilters);

			List<Node[]> myresult = executeSparqlSelectQuery(querytemplate, true);

			// List<Node[]> result2 = applyRestrictions(memberUris2,
			// restrictions);

			// Add all of result2 to result
			boolean first = true;
			for (Node[] nodes : myresult) {
				if (first) {
					first = false;
					continue;
				}
				result.add(nodes);
			}
		}

		// Add levels for dimensions without codelist, but only if hierarchy and
		// dimension names are equal
		if (!isMeasureQueriedForExplicitly(restrictions.dimensionUniqueName,
				restrictions.hierarchyUniqueName, restrictions.levelUniqueName)) {

			String querytemplate = Olap4ldLinkedDataUtil
					.readInQueryTemplate("sesame_getLevels_without_codelist.txt");
			querytemplate = querytemplate.replace("{{{STANDARDFROM}}}",
					askForFrom(true));
			querytemplate = querytemplate.replace("{{{TABLE_CAT}}}", TABLE_CAT);
			querytemplate = querytemplate.replace("{{{TABLE_SCHEM}}}",
					TABLE_SCHEM);
			querytemplate = querytemplate.replace("{{{FILTERS}}}",
					additionalFilters);

			// Second, ask for the measures (which are also members)
			List<Node[]> myresult = executeSparqlSelectQuery(querytemplate, true);

			// List<Node[]> result3 = applyRestrictions(memberUris3,
			// restrictions);

			// Add all of result3 to result
			boolean first = true;
			for (Node[] nodes : myresult) {
				if (first) {
					first = false;
					continue;
				}
				result.add(nodes);
			}
		}

		return result;
	}

	/**
	 * Important issues to remember: Every measure also needs to be listed as
	 * member. When I create the dsd, I add obsValue as a dimension, but also as
	 * a measure. However, members of the measure dimension would typically all
	 * be named differently from the measure (e.g., obsValue5), therefore, we do
	 * not find a match. The problem is, that getMembers() has to return the
	 * measures. So, either, in the dsd, we need to add a dimension with the
	 * measure as a member, or, the query for the members should return for
	 * measures the measure property as member.
	 * 
	 * The dimension/hierarchy/level of a measure should always be "Measures".
	 * 
	 * Typically, a measure should not have a codeList, since we can have many
	 * many members. If a measure does not have a codelist, the bounding would
	 * still work, since The componentProperty is existing, but no hierarchy...
	 * 
	 * For caption of members, we should eventually use
	 * http://www.w3.org/2004/02/skos/core#notation skos:notation, since members
	 * are in rdf represented as skos:Concept and this is the proper way to give
	 * them a representation.
	 * 
	 * Assumptions of this method:
	 * 
	 * The restrictions are set up only as follows 1) cube, dim, hier, level 2)
	 * cube, dim, hier, level, member, null 3) cube, dim, hier, level, member,
	 * treeOp
	 * 
	 * The members are only modelled as follows 1) Measure Member (member of the
	 * measure dimension) 2) Level Member (member of a regular dimension) 3) Top
	 * Concept Member (member via skos:topConcept) 4) Degenerated Member (member
	 * without code list)
	 * 
	 * @return Node[]{?memberURI ?name}
	 * @throws MalformedURLException
	 */
	public List<Node[]> getMembers(Restrictions restrictions)
			throws OlapException {

		checkSufficientInformationGathered(restrictions);

		List<Node[]> result = new ArrayList<Node[]>();
		List<Node[]> intermediaryresult = null;

		// Create header
		Node[] header = new Node[] { new Variable("?CATALOG_NAME"),
				new Variable("?SCHEMA_NAME"), new Variable("?CUBE_NAME"),
				new Variable("?DIMENSION_UNIQUE_NAME"),
				new Variable("?HIERARCHY_UNIQUE_NAME"),
				new Variable("?LEVEL_UNIQUE_NAME"),
				new Variable("?LEVEL_NUMBER"), new Variable("?MEMBER_NAME"),
				new Variable("?MEMBER_UNIQUE_NAME"),
				new Variable("?MEMBER_CAPTION"), new Variable("?MEMBER_TYPE"),
				new Variable("?PARENT_UNIQUE_NAME"),
				new Variable("?PARENT_LEVEL") };
		result.add(header);

		// Measure Member
		if (true) {
			intermediaryresult = getMeasureMembers(restrictions);

			addToResult(intermediaryresult, result);

		}

		// Regular members
		if (!isMeasureQueriedForExplicitly(restrictions.dimensionUniqueName,
				restrictions.hierarchyUniqueName, restrictions.levelUniqueName)) {

			intermediaryresult = getHasTopConceptMembers(restrictions);

			addToResult(intermediaryresult, result);
		}

		// Xkos members
		// Watch out: No square brackets
		if (!isMeasureQueriedForExplicitly(restrictions.dimensionUniqueName,
				restrictions.hierarchyUniqueName, restrictions.levelUniqueName)) {

			intermediaryresult = getXkosMembers(restrictions);

			addToResult(intermediaryresult, result);

		}

		// If we still do not have members, then we might have degenerated
		// members
		if (!isMeasureQueriedForExplicitly(restrictions.dimensionUniqueName,
				restrictions.hierarchyUniqueName, restrictions.levelUniqueName)) {
			// Members without codeList
			intermediaryresult = getDegeneratedMembers(restrictions);

			addToResult(intermediaryresult, result);

		}

		return result;
	}

	private List<Node[]> getMeasureMembers(Restrictions restrictions) {

		String additionalFilters = createFilterForRestrictions(restrictions);

		/*
		 * I would assume that if TREE_OP is set, we have a unique member given
		 * and either want its children, its siblings, its parent, self,
		 * ascendants, or descendants.
		 */
		if (restrictions.tree != null && (restrictions.tree & 8) != 8) {

			// Assumption 1: Treeop only uses Member
			if (restrictions.memberUniqueName == null) {
				throw new UnsupportedOperationException(
						"If a treeMask is given, we should also have a unique member name!");
			}

			if ((restrictions.tree & 1) == 1) {
				// CHILDREN
				Olap4ldUtil._log.config("TreeOp:CHILDREN");

			}
			if ((restrictions.tree & 2) == 2) {
				// SIBLINGS
				Olap4ldUtil._log.config("TreeOp:SIBLINGS");

				if (restrictions.cubeNamePattern != null) {
					additionalFilters += " FILTER (?CUBE_NAME = <"
							+ restrictions.cubeNamePattern + ">) ";
				}
			}
			if ((restrictions.tree & 4) == 4) {
				// PARENT
				Olap4ldUtil._log.config("TreeOp:PARENT");
			}
			if ((restrictions.tree & 16) == 16) {
				// DESCENDANTS
				Olap4ldUtil._log.config("TreeOp:DESCENDANTS");

			}
			if ((restrictions.tree & 32) == 32) {
				// ANCESTORS
				Olap4ldUtil._log.config("TreeOp:ANCESTORS");
			}

		} else {
			// TreeOp = Self or null
			Olap4ldUtil._log.config("TreeOp:SELF");

		}

		// Second, ask for the measures (which are also members)
		String querytemplate = Olap4ldLinkedDataUtil
				.readInQueryTemplate("sesame_getMembers_measure_members.txt");
		querytemplate = querytemplate.replace("{{{STANDARDFROM}}}",
				askForFrom(true));
		querytemplate = querytemplate.replace("{{{TABLE_CAT}}}", TABLE_CAT);
		querytemplate = querytemplate.replace("{{{TABLE_SCHEM}}}", TABLE_SCHEM);
		querytemplate = querytemplate.replace("{{{FILTERS}}}",
				additionalFilters);

		List<Node[]> memberUris2 = executeSparqlSelectQuery(querytemplate, true);

		return memberUris2;
	}

	/**
	 * Finds specific typical members.
	 * 
	 * @param restrictions
	 * 
	 * @return
	 */
	private List<Node[]> getXkosMembers(Restrictions restrictions) {

		String additionalFilters = createFilterForRestrictions(restrictions);

		/*
		 * I would assume that if TREE_OP is set, we have a unique member given
		 * and either want its children, its siblings, its parent, self,
		 * ascendants, or descendants.
		 */
		if (restrictions.tree != null && (restrictions.tree & 8) != 8) {

			// Assumption 1: Treeop only uses Member
			if (restrictions.memberUniqueName == null) {
				throw new UnsupportedOperationException(
						"If a treeMask is given, we should also have a unique member name!");
			}

			if ((restrictions.tree & 1) == 1) {
				// CHILDREN
				Olap4ldUtil._log.config("TreeOp:CHILDREN");

				// Here, we need a specific filter
				additionalFilters = " FILTER (?PARENT_UNIQUE_NAME = <"
						+ restrictions.memberUniqueName + ">) ";

			}
			if ((restrictions.tree & 2) == 2) {
				// SIBLINGS
				Olap4ldUtil._log.config("TreeOp:SIBLINGS");

			}
			if ((restrictions.tree & 4) == 4) {
				// PARENT
				Olap4ldUtil._log.config("TreeOp:PARENT");
			}
			if ((restrictions.tree & 16) == 16) {
				// DESCENDANTS
				Olap4ldUtil._log.config("TreeOp:DESCENDANTS");

			}
			if ((restrictions.tree & 32) == 32) {
				// ANCESTORS
				Olap4ldUtil._log.config("TreeOp:ANCESTORS");
			}

			throw new UnsupportedOperationException(
					"TreeOp and getLevelMember failed.");

		} else {
			// TreeOp = Self or null
			Olap4ldUtil._log.config("TreeOp:SELF");

		}

		String querytemplate = Olap4ldLinkedDataUtil
				.readInQueryTemplate("sesame_getMembers_xkos.txt");
		querytemplate = querytemplate.replace("{{{STANDARDFROM}}}",
				askForFrom(true));
		querytemplate = querytemplate.replace("{{{TABLE_CAT}}}", TABLE_CAT);
		querytemplate = querytemplate.replace("{{{TABLE_SCHEM}}}", TABLE_SCHEM);
		querytemplate = querytemplate.replace("{{{FILTERS}}}",
				additionalFilters);

		List<Node[]> memberUris2 = executeSparqlSelectQuery(querytemplate, true);

		return memberUris2;
	}

	/**
	 * Returns all hasTopConcept members of the cube.
	 * 
	 * @param dimensionUniqueName
	 * @param cubeNamePattern
	 * 
	 * @param cubeNamePattern
	 * @return
	 */
	private List<Node[]> getHasTopConceptMembers(Restrictions restrictions) {

		String additionalFilters = createFilterForRestrictions(restrictions);

		/*
		 * I would assume that if TREE_OP is set, we have a unique member given
		 * and either want its children, its siblings, its parent, self,
		 * ascendants, or descendants.
		 */
		if (restrictions.tree != null && (restrictions.tree & 8) != 8) {

			// Assumption 1: Treeop only uses Member
			if (restrictions.memberUniqueName == null) {
				throw new UnsupportedOperationException(
						"If a treeMask is given, we should also have a unique member name!");
			}

			if ((restrictions.tree & 1) == 1) {
				// CHILDREN
				Olap4ldUtil._log.config("TreeOp:CHILDREN");

			}
			if ((restrictions.tree & 2) == 2) {
				// SIBLINGS
				Olap4ldUtil._log.config("TreeOp:SIBLINGS");

			}
			if ((restrictions.tree & 4) == 4) {
				// PARENT
				Olap4ldUtil._log.config("TreeOp:PARENT");
			}
			if ((restrictions.tree & 16) == 16) {
				// DESCENDANTS
				Olap4ldUtil._log.config("TreeOp:DESCENDANTS");

			}
			if ((restrictions.tree & 32) == 32) {
				// ANCESTORS
				Olap4ldUtil._log.config("TreeOp:ANCESTORS");
			}

			throw new UnsupportedOperationException(
					"TreeOp and getLevelMember failed.");

		} else {
			// TreeOp = Self or null
			Olap4ldUtil._log.config("TreeOp:SELF");

			// First, ask for all members
			// Get all members of hierarchies without levels, that simply
			// define
			// skos:hasTopConcept members with skos:notation.
			String querytemplate = Olap4ldLinkedDataUtil
					.readInQueryTemplate("sesame_getMembers_topConcept.txt");
			querytemplate = querytemplate.replace("{{{STANDARDFROM}}}",
					askForFrom(true));
			querytemplate = querytemplate.replace("{{{TABLE_CAT}}}", TABLE_CAT);
			querytemplate = querytemplate.replace("{{{TABLE_SCHEM}}}",
					TABLE_SCHEM);
			querytemplate = querytemplate.replace("{{{FILTERS}}}",
					additionalFilters);

			List<Node[]> memberUris = executeSparqlSelectQuery(querytemplate, true);

			return memberUris;

		}

	}

	/**
	 * For degenerated dimensions, we have to assume that either dim, hier, or
	 * level are given.
	 * 
	 * @return
	 */
	private List<Node[]> getDegeneratedMembers(Restrictions restrictions) {

		String additionalFilters = createFilterForRestrictions(restrictions);

		if (restrictions.tree != null && (restrictions.tree & 8) != 8) {

			// Assumption 1: Treeop only uses Member
			if (restrictions.memberUniqueName == null) {
				throw new UnsupportedOperationException(
						"If a treeMask is given, we should also have a unique member name!");
			}

			if ((restrictions.tree & 1) == 1) {
				// CHILDREN
				Olap4ldUtil._log.config("TreeOp:CHILDREN");

			}
			if ((restrictions.tree & 2) == 2) {
				// SIBLINGS
				Olap4ldUtil._log.config("TreeOp:SIBLINGS");

			}
			if ((restrictions.tree & 4) == 4) {
				// PARENT
				Olap4ldUtil._log.config("TreeOp:PARENT");
			}
			if ((restrictions.tree & 16) == 16) {
				// DESCENDANTS
				Olap4ldUtil._log.config("TreeOp:DESCENDANTS");

			}
			if ((restrictions.tree & 32) == 32) {
				// ANCESTORS
				Olap4ldUtil._log.config("TreeOp:ANCESTORS");
			}

			throw new UnsupportedOperationException(
					"TreeOp and getLevelMember failed.");

		} else {
			// TreeOp = Self or null
			Olap4ldUtil._log.config("TreeOp:SELF");

		}

		String querytemplate = Olap4ldLinkedDataUtil
				.readInQueryTemplate("sesame_getMembers_degenerated.txt");
		querytemplate = querytemplate.replace("{{{STANDARDFROM}}}",
				askForFrom(true));
		querytemplate = querytemplate.replace("{{{TABLE_CAT}}}", TABLE_CAT);
		querytemplate = querytemplate.replace("{{{TABLE_SCHEM}}}", TABLE_SCHEM);
		querytemplate = querytemplate.replace("{{{FILTERS}}}",
				additionalFilters);

		List<Node[]> memberUris1 = executeSparqlSelectQuery(querytemplate, true);

		return memberUris1;

	}

	private boolean isResourceAndNotLiteral(Node resource) {
		return resource.toString().startsWith("http:");
	}

	private String createFilterForRestrictions(Restrictions restrictions) {
		// We need to create a filter for the specific restriction
		String cubeNamePatternFilter = (restrictions.cubeNamePattern != null) ? " FILTER (?CUBE_NAME = <"
				+ restrictions.cubeNamePattern + ">) "
				: "";
		String dimensionUniqueNameFilter = (restrictions.dimensionUniqueName != null && !restrictions.dimensionUniqueName
				.equals(Olap4ldLinkedDataUtil.MEASURE_DIMENSION_NAME)) ? " FILTER (?DIMENSION_UNIQUE_NAME = <"
				+ restrictions.dimensionUniqueName + ">) "
				: "";
		String hierarchyUniqueNameFilter = (restrictions.hierarchyUniqueName != null && !restrictions.hierarchyUniqueName
				.equals(Olap4ldLinkedDataUtil.MEASURE_DIMENSION_NAME)) ? " FILTER (?HIERARCHY_UNIQUE_NAME = <"
				+ restrictions.hierarchyUniqueName + ">) "
				: "";
		String levelUniqueNameFilter = (restrictions.levelUniqueName != null && !restrictions.levelUniqueName
				.equals(Olap4ldLinkedDataUtil.MEASURE_DIMENSION_NAME)) ? " FILTER (?LEVEL_UNIQUE_NAME = <"
				+ restrictions.levelUniqueName + ">) "
				: "";

		String memberUniqueNameFilter;
		if (restrictions.memberUniqueName != null) {
			Node resource = restrictions.memberUniqueName;
			if (isResourceAndNotLiteral(resource)) {
				memberUniqueNameFilter = " FILTER (?MEMBER_UNIQUE_NAME = <"
						+ resource + ">) ";
			} else {
				memberUniqueNameFilter = " FILTER (str(?MEMBER_UNIQUE_NAME) = \""
						+ resource + "\") ";
			}
		} else {
			memberUniqueNameFilter = "";
		}

		return cubeNamePatternFilter + dimensionUniqueNameFilter
				+ hierarchyUniqueNameFilter + levelUniqueNameFilter
				+ memberUniqueNameFilter;
	}

	/**
	 * Adds intermediary results to result.
	 * 
	 * @param intermediaryresult
	 * @param result
	 */
	private void addToResult(List<Node[]> intermediaryresult,
			List<Node[]> result) {
		boolean first = true;
		for (Node[] nodes : intermediaryresult) {
			if (first) {
				first = false;
				continue;
			}
			result.add(nodes);
		}
	}

	public List<Node[]> getSets(Restrictions restrictions) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Node[]> executeOlapQuery(LogicalOlapQueryPlan queryplan)
			throws OlapException {
		// Log logical query plan
		Olap4ldUtil._log.config("Logical query plan to string: "
				+ queryplan.toString());

		LogicalToPhysical logicaltophysical = new LogicalToPhysical(null);

		PhysicalOlapIterator newRoot;
		newRoot = logicaltophysical.compile(queryplan._root);

		Olap4ldUtil._log.config("bytes iterator " + newRoot);

		PhysicalOlapQueryPlan ap = new PhysicalOlapQueryPlan(newRoot);

		PhysicalOlapIterator resultIterator = ap.getIterator();

		List<Node[]> result = new ArrayList<Node[]>();
		while (resultIterator.hasNext()) {
			Object nextObject = resultIterator.next();
			// Will be Node[]
			Node[] node = (Node[]) nextObject;
			result.add(node);
		}
		return result;
	}

	@Override
	public List<Node[]> executeOlapQuery(Cube cube, List<Level> slicesrollups,
			List<Position> dices, List<Measure> projections)
			throws OlapException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Empties store and locationMap.
	 */
	public void rollback() {

		// Store
		try {
			// Rely on garbage collector
			// this.repo.initialize();
			// this.repo.shutDown();
			this.repo = new SailRepository(new MemoryStore());
			repo.initialize();

			// do something interesting with the values here...
			// con.close();
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		loadedMap.clear();
	}

	@Override
	public PhysicalOlapQueryPlan getExecplan() throws OlapException {
		// TODO Auto-generated method stub
		return null;
	}

}
