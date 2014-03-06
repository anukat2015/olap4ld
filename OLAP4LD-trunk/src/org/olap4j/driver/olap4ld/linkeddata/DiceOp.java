package org.olap4j.driver.olap4ld.linkeddata;

import java.util.List;
import java.util.Map;

import org.olap4j.driver.olap4ld.helper.Olap4ldLinkedDataUtil;
import org.semanticweb.yars.nx.Node;

/**
 * This operator removes facts from a result of a LogicalOlapOp that does not
 * comply with a condition.
 * 
 * @author benedikt
 * 
 */
public class DiceOp implements LogicalOlapOp {

	public LogicalOlapOp inputOp;
	public List<List<Node[]>> membercombinations;
	public List<Node[]> hierarchysignature;

	public DiceOp(LogicalOlapOp op, List<Node[]> hierarchysignature,
			List<List<Node[]>> membercombinations) {
		this.inputOp = op;
		this.membercombinations = membercombinations;
		this.hierarchysignature = hierarchysignature;
	}

	public String toString() {
		String positionsStringArray = "";

		// Every member same schema
		if (membercombinations == null || membercombinations.isEmpty()) {
			positionsStringArray = "{}";
		} else {
			Map<String, Integer> map = Olap4ldLinkedDataUtil
					.getNodeResultFields(membercombinations.get(0).get(0));
			Map<String, Integer> signaturemap = Olap4ldLinkedDataUtil
					.getNodeResultFields(hierarchysignature.get(0));

			// There is no header
			for (int i = 0; i < membercombinations.size(); i++) {
				List<Node[]> members = membercombinations.get(i);
				String positionStringArray[] = new String[members
				                  						.size()-1];
				// First is header
				for (int j = 1; j < members.size(); j++) {
					positionStringArray[j-1] = this.hierarchysignature.get(j)[signaturemap
							.get("?HIERARCHY_UNIQUE_NAME")]
							+ " = "
							+ members.get(j)[map.get("?MEMBER_UNIQUE_NAME")]
									.toString();
				}
				positionsStringArray += "("
						+ Olap4ldLinkedDataUtil.implodeArray(
								positionStringArray, ", ") + ")";
			}
		}

		return "Dice (" + inputOp.toString() + ", " + positionsStringArray
				+ ")";
	}

	@Override
	public void accept(LogicalOlapOperatorQueryPlanVisitor v)
			throws QueryException {
		v.visit(this);
		// visit the projection input op
		inputOp.accept(v);
	}

}
