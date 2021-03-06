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

import java.net.MalformedURLException;
import java.util.List;

import org.olap4j.OlapException;
import org.olap4j.Position;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Measure;
import org.semanticweb.yars.nx.Node;

/**
 * 
 * An LinkedDataCubesEngine is basically an OLAP Engine on Linked Data. It
 * implements Metadata Queries such as getCubes() and OLAP Queries such as
 * getOlapResult().
 * 
 * Implements methods of XmlaOlap4jDatabaseMetadata, returning the specified
 * columns as nodes.
 * 
 * @author b-kaempgen
 * 
 */
public interface LinkedDataCubesEngine {
	
	/**
	 * Puts engine to its default init state.
	 */
	public void rollback();

	public List<Node[]> getDatabases(Restrictions restrictions)
			throws OlapException;

	public List<Node[]> getCatalogs(Restrictions restrictions)
			throws OlapException;

	public List<Node[]> getSchemas(Restrictions restrictions)
			throws OlapException;

	/**
	 * 
	 * Get Cubes from the triple store.
	 * 
	 * Schema: ?CATALOG_NAME ?SCHEMA_NAME ?CUBE_NAME ?CUBE_TYPE ?CUBE_CAPTION
	 * ?DESCRIPTION
	 * 
	 * Here, the restrictions are strict restrictions without patterns.
	 * 
	 * ==Task: Show proper captions== Problem: Where to take captions from?
	 * rdfs:label Problem: There might be several rdfs:label -> only English
	 * When creating such dsds, we could give the dsd an english label Also, we
	 * need an english label for dimension, hierarchy, level, members Cell
	 * Values will be numeric and not require language
	 * 
	 * @return Node[]{}
	 */
	public List<Node[]> getCubes(Restrictions restrictions)
			throws OlapException;

	/**
	 * Get possible dimensions (component properties) for each cube from the
	 * triple store.
	 * 
	 * Schema: ?CATALOG_NAME ?SCHEMA_NAME ?CUBE_NAME ?DIMENSION_NAME
	 * ?DIMENSION_UNIQUE_NAME ?DIMENSION_CAPTION ?DIMENSION_ORDINAL
	 * ?DIMENSION_TYPE ?DESCRIPTION
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
			throws OlapException;

	/**
	 * 
	 * Measures: ?CATALOG_NAME ?SCHEMA_NAME ?CUBE_NAME ?MEASURE_UNIQUE_NAME
	 * ?MEASURE_NAME ?MEASURE_CAPTION ?DATA_TYPE ?MEASURE_IS_VISIBLE
	 * ?MEASURE_AGGREGATOR ?EXPRESSION
	 * 
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
			throws OlapException;

	/**
	 * 
	 * Hierarchies: ?CATALOG_NAME ?SCHEMA_NAME ?CUBE_NAME ?DIMENSION_UNIQUE_NAME
	 * ?HIERARCHY_UNIQUE_NAME ?HIERARCHY_NAME ?HIERARCHY_CAPTION ?DESCRIPTION
	 * ?HIERARCHY_MAX_LEVEL_NUMBER
	 * 
	 * Return hierarchies
	 * 
	 * @param context
	 * @param metadataRequest
	 * @param restrictions
	 * @return
	 */
	public List<Node[]> getHierarchies(Restrictions restrictions)
			throws OlapException;

	/**
	 * 
	 * Levels: ?CATALOG_NAME ?SCHEMA_NAME ?CUBE_NAME ?DIMENSION_UNIQUE_NAME
	 * ?HIERARCHY_UNIQUE_NAME ?LEVEL_UNIQUE_NAME ?LEVEL_CAPTION ?LEVEL_NAME
	 * ?DESCRIPTION ?LEVEL_NUMBER ?LEVEL_CARDINALITY ?LEVEL_TYPE
	 * 
	 * @param context
	 * @param metadataRequest
	 * @param restrictions
	 * @return
	 */
	public List<Node[]> getLevels(Restrictions restrictions)
			throws OlapException;

	/**
	 * 
	 * Members: ?CATALOG_NAME ?SCHEMA_NAME ?CUBE_NAME ?DIMENSION_UNIQUE_NAME
	 * ?HIERARCHY_UNIQUE_NAME ?LEVEL_UNIQUE_NAME ?LEVEL_NUMBER
	 * ?MEMBER_UNIQUE_NAME ?MEMBER_NAME ?MEMBER_CAPTION ?MEMBER_TYPE
	 * ?PARENT_UNIQUE_NAME ?PARENT_LEVEL
	 * 
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
	 * For caption of members, we use
	 * http://www.w3.org/2004/02/skos/core#notation skos:notation, since members
	 * are in rdf represented as skos:Concept and this is the proper way to give
	 * them a representation.
	 * 
	 * @return Node[]{?memberURI ?name}
	 * @throws MalformedURLException
	 */
	public List<Node[]> getMembers(Restrictions restrictions)
			throws OlapException;

	public List<Node[]> getSets(Restrictions restrictions) throws OlapException;

	/**
	 * In the current Olap4LD implementation, an OLAP query is issued from the
	 * input of a subcube query tuple: set of levels for each inquired dimension
	 * set of queried meassures set of set of members for each fixed dimension
	 * cube
	 * 
	 * @return
	 */
	@Deprecated
	public List<Node[]> executeOlapQuery(Cube cube, List<Level> slicesrollups,
			List<Position> dices, List<Measure> projections)
			throws OlapException;

	/**
	 * In the extended olap4ld implementation, an OLAP query is issued from the
	 * input of a logical OLAP operator query plan: a tree of logical OLAP
	 * operators that are then translated into a physical OLAP operator query
	 * plan depending on the implementation.
	 * 
	 * The question is, how does the result of executeOlapQuery relate to the
	 * metadata given by metadata queries? Olap4j/olap4ld have a relational
	 * metadata schema. At least, the result should map somehow to the queried
	 * cube(s). Two possibilities
	 * 
	 * 1) We assume drill-across and other operators, so that the result may map
	 * to a metadata result after executing the LogicalOlapQuery plan.
	 * 
	 * 2) We assume no drill-across but only virtual cubes, so that the result
	 * always is mapped to a specific cube.
	 * 
	 * @param queryplan
	 * @return a relational representation of resulting observations in the
	 *         resulting data cube: List<Node[]>. Every Node[] contains for each
	 *         dimension in the dimension list of the metadata a member and for
	 *         each measure in the measure list a value.
	 */
	public List<Node[]> executeOlapQuery(LogicalOlapQueryPlan queryplan)
			throws OlapException;

	public PhysicalOlapQueryPlan getExecplan()
			throws OlapException;

	/**
	 * During adding of OV engine added.
	 * @param observationquery
	 * @param b
	 * @return
	 */
	public List<Node[]> executeSparqlSelectQuery(String observationquery, boolean b);

	/**
	 * During adding of OV engine added.
	 * @param constructquery
	 */
	public void executeSparqlConstructQuery(String constructquery);
}