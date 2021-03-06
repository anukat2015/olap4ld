/*
//$Id: MetadataTest.java 482 2012-01-05 23:27:27Z jhyde $
//
//Licensed to Julian Hyde under one or more contributor license
//agreements. See the NOTICE file distributed with this work for
//additional information regarding copyright ownership.
//
//Julian Hyde licenses this file to you under the Apache License,
//Version 2.0 (the "License"); you may not use this file except in
//compliance with the License. You may obtain a copy of the License at:
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
 */
package org.olap4j;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;

import junit.framework.TestCase;

import org.olap4j.CellSetFormatterTest.Format;
import org.olap4j.layout.RectangularCellSetFormatter;
import org.olap4j.layout.TraditionalCellSetFormatter;
import org.olap4j.mdx.SelectNode;
import org.olap4j.mdx.parser.MdxParser;
import org.olap4j.mdx.parser.MdxParserFactory;
import org.olap4j.mdx.parser.MdxValidator;
import org.olap4j.test.TestContext;

/**
 * Unit test for olap4j metadata methods.
 * 
 * @version $Id: MetadataTest.java 482 2012-01-05 23:27:27Z jhyde $
 */
public class LdOlap4jQueryTest extends TestCase {
	private final TestContext testContext = TestContext.instance();
	private final TestContext.Tester tester = testContext.getTester();
	private Connection connection;
	private OlapConnection olapConnection;
	private OlapStatement stmt;
	private MdxParserFactory parserFactory;
	private MdxParser parser;

	public LdOlap4jQueryTest() throws SQLException {
	}

	protected void setUp() throws SQLException {
		connection = tester.createConnection();
		connection.getCatalog();
		olapConnection = tester.getWrapper().unwrap(connection,
				OlapConnection.class);
		olapConnection.getMetaData();

		// Create a statement based upon the object model.
		// One can simply keep open the statement and issue new queries.
		OlapConnection olapconnection = (OlapConnection) connection;
		this.stmt = null;
		try {
			stmt = olapconnection.createStatement();
		} catch (OlapException e) {
			System.out.println("Validation failed: " + e);
			return;
		}

		this.parserFactory = olapconnection.getParserFactory();
		this.parser = parserFactory.createMdxParser(olapconnection);
	}

	protected void tearDown() throws Exception {
		if (connection != null && !connection.isClosed()) {
			connection.close();
			connection = null;
		}
	}

	/**
	 * Query 1.1
	 */
	public void testSSB_Q_1_1() {
		executeStatement("WITH MEMBER [Measures].[Revenue] as '[Measures].[Measures].[Measures].[rdfh:lo_extendedprice] * [Measures].[Measures].[Measures].[rdfh:lo_discount]' "
				+ "SELECT {[Measures].[Revenue]} ON COLUMNS, "
				+ "{[rdfh:lo_orderdate].[rdfh:lo_orderdateCodeList].[rdfh:lo_orderdateYearLevel].[rdfh:lo_orderdateYear1993]} ON ROWS "
				+ "FROM [rdfh-inst:dsd] "
				+ "WHERE CrossJoin(Filter(Members([rdfh:lo_quantity]), "
				+ "Cast(Name(CurrentMember([rdfh:lo_quantity])) as NUMERIC) < 25), "
				+ "Filter(Members([rdfh:lo_discount]), "
				+ "Cast(Name(CurrentMember([rdfh:lo_discount])) as NUMERIC) >= 1 "
				+ "and Cast(Name(CurrentMember([rdfh:lo_discount])) as NUMERIC) <= 3))");
	}

	/**
	 * Query 1.2
	 */
	public void testSSB_Q_1_2() {
		executeStatement("WITH MEMBER [Measures].[Revenue] as '[Measures].[Measures].[Measures].[rdfh:lo_extendedprice] * [Measures].[Measures].[Measures].[rdfh:lo_discount]' "
				+ "SELECT {[Measures].[Revenue]} ON COLUMNS, "
				+ "{[rdfh:lo_orderdate].[rdfh:lo_orderdateCodeList].[rdfh:lo_orderdateYearMonthNumLevel].[rdfh:lo_orderdateYearMonthNum199401]} ON ROWS "
				+ "FROM [rdfh-inst:dsd] "
				+ "WHERE CrossJoin(Filter(Members([rdfh:lo_quantity]), "
				+ "Cast(Name(CurrentMember([rdfh:lo_quantity])) as NUMERIC) >= 26 and Cast(Name(CurrentMember([rdfh:lo_quantity])) as NUMERIC) <= 35), "
				+ "Filter(Members([rdfh:lo_discount]), "
				+ "Cast(Name(CurrentMember([rdfh:lo_discount])) as NUMERIC) >= 4 "
				+ "and Cast(Name(CurrentMember([rdfh:lo_discount])) as NUMERIC) <= 6)) ");
	}

	/**
	 * Query 1.3
	 */
	public void testSSB_Q_1_3() {
		executeStatement("WITH MEMBER [Measures].[Revenue] as '[Measures].[Measures].[Measures].[rdfh:lo_extendedprice] * [Measures].[Measures].[Measures].[rdfh:lo_discount]' "
				+ "SELECT {[Measures].[Revenue]} ON COLUMNS, "
				+ "{[rdfh:lo_orderdate].[rdfh:lo_orderdateWeeknuminyearCodeList].[rdfh:lo_orderdateWeeknuminyearLevel].[rdfh:lo_orderdateWeeknuminyear19946]} ON ROWS "
				+ "FROM [rdfh-inst:dsd] "
				+ "WHERE CrossJoin(Filter(Members([rdfh:lo_quantity]), "
				+ "Cast(Name(CurrentMember([rdfh:lo_quantity])) as NUMERIC) >= 26 and Cast(Name(CurrentMember([rdfh:lo_quantity])) as NUMERIC) <= 35), "
				+ "Filter(Members([rdfh:lo_discount]), "
				+ "Cast(Name(CurrentMember([rdfh:lo_discount])) as NUMERIC) >= 5 "
				+ "and Cast(Name(CurrentMember([rdfh:lo_discount])) as NUMERIC) <= 7))");
	}

	/**
	 * Query 2.1
	 */
	public void testSSB_Q_2_1() {
		executeStatement("SELECT {[Measures].[Measures].[Measures].[rdfh:lo_revenue]} ON COLUMNS, "
				+ "CrossJoin(Members([rdfh:lo_orderdate].[rdfh:lo_orderdateCodeList].[rdfh:lo_orderdateYearLevel]), "
				+ "{[rdfh:lo_partkey].[rdfh:lo_partkeyCodeList].[rdfh:lo_partkeyCategoryLevel].[rdfh:lo_partkeyCategoryMFGR-12]}) ON ROWS "
				+ "FROM [rdfh-inst:dsd] "
				+ "WHERE {[rdfh:lo_suppkey].[rdfh:lo_suppkeyCodeList].[rdfh:lo_suppkeyRegionLevel].[rdfh:lo_suppkeyRegionAMERICA]}");

	}

	/**
	 * Query 2.2
	 */
	public void testSSB_Q_2_2() {
		executeStatement("SELECT {[Measures].[Measures].[Measures].[rdfh:lo_revenue]} ON COLUMNS, "
				+ "CrossJoin(Members([rdfh:lo_orderdate].[rdfh:lo_orderdateCodeList].[rdfh:lo_orderdateYearLevel]), "
				+ "{[rdfh:lo_partkey].[rdfh:lo_partkeyCodeList].[rdfh:lo_partkeyCategoryLevel].[rdfh:lo_partkeyCategoryMFGR-12]}) ON ROWS "
				+ "FROM [rdfh-inst:dsd] "
				+ "WHERE {[rdfh:lo_suppkey].[rdfh:lo_suppkeyCodeList].[rdfh:lo_suppkeyRegionLevel].[rdfh:lo_suppkeyRegionAMERICA]}");
	}

	private void executeStatement(String mdxString) {
		// Execute the statement.
		CellSet cset;
		try {

			SelectNode select = parser.parseSelect(mdxString);
			MdxValidator validator = parserFactory
					.createMdxValidator(olapConnection);
			select = validator.validateSelect(select);
			cset = stmt.executeOlapQuery(select);

			// String s = TestContext.toString(cset);
			String resultString = toString(cset, Format.RECTANGULAR);

			System.out.println("Output:");
			System.out.println(resultString);

		} catch (OlapException e) {
			System.out.println("Execution failed: " + e);
		}
	}

	/**
	 * Converts a {@link CellSet} to text.
	 * 
	 * @param cellSet
	 *            Query result
	 * @param format
	 *            Format
	 * @return Result as text
	 */
	static String toString(CellSet cellSet, Format format) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		switch (format) {
		case TRADITIONAL:
			new TraditionalCellSetFormatter().format(cellSet, pw);
			break;
		case COMPACT_RECTANGULAR:
		case RECTANGULAR:
			new RectangularCellSetFormatter(
					format == Format.COMPACT_RECTANGULAR).format(cellSet, pw);
			break;
		}
		pw.flush();
		return sw.toString();
	}
}

// End MetadataTest.java
