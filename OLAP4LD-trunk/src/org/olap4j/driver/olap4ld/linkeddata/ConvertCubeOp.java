package org.olap4j.driver.olap4ld.linkeddata;


/**
 * This operator removes a Dimension from a Cube, a result of a lower
 * LogicalOlapOp.
 * 
 * @author benedikt
 * 
 */
public class ConvertCubeOp implements LogicalOlapOp {

	public LogicalOlapOp inputOp1;
	public LogicalOlapOp inputOp2;
	public String conversionfunction;
	public String domainUri;
	public ConversionCorrespondence conversioncorrespondence;

	/**
	 * 
	 * @param inputOp
	 * @param conversionfunction Currently, conversion function is a Linked-Data-Fu program. 
	 * The goal is to represent it in terms of multidimensional elements. 
	 * @param domainUri
	 */
	public ConvertCubeOp(LogicalOlapOp inputOp,
			String conversionfunction, String domainUri) {
		this.inputOp1 = inputOp;
		this.inputOp2 = null;
		this.conversionfunction = conversionfunction;
		this.domainUri = domainUri;
	}
	
	/**
	 * 
	 * @param inputOp
	 * @param conversionfunction Currently, conversion function is a Linked-Data-Fu program. 
	 * The goal is to represent it in terms of multidimensional elements. 
	 * @param domainUri
	 */
	public ConvertCubeOp(LogicalOlapOp inputOp,
			ConversionCorrespondence conversioncorrespondence, String domainUri) {
		this.inputOp1 = inputOp;
		this.inputOp2 = null;
		this.conversioncorrespondence = conversioncorrespondence;
		this.domainUri = domainUri;
	}

	public ConvertCubeOp(LogicalOlapOp inputOp1, LogicalOlapOp inputOp2,
			String conversionfunction, String domainUri) {
		this.inputOp1 = inputOp1;
		this.inputOp2 = inputOp2;
		this.conversionfunction = conversionfunction;
		this.domainUri = domainUri;
	}

	public String toString() {
		if (inputOp2 == null) {
			return "Convert-context (" + inputOp1.toString() + ", "
					+ conversionfunction + conversioncorrespondence.toString() + ")";
		} else {
			return "Convert-context (" + inputOp1.toString() + ", " + inputOp2.toString() + ", "
					+ conversionfunction + conversioncorrespondence.toString() + ")";
		}
	}

	@Override
	public void accept(LogicalOlapOperatorQueryPlanVisitor v)
			throws QueryException {

		v.visit(this);

		if (v instanceof Olap2SparqlSesameDerivedDatasetVisitor) {
			// Nothing more to visit;
		} else {
			// visit the input op
			inputOp1.accept(v);
			
			if (inputOp2 != null) {
				inputOp2.accept(v);
			}
		}
	}
}
