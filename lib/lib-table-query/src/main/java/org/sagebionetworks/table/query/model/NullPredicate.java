package org.sagebionetworks.table.query.model;

/**
 * This matches &ltnull predicate&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class NullPredicate extends IsPredicate {
	
	ColumnReference columnReferenceLHS;
	Boolean not;

	public NullPredicate(ColumnReference columnReferenceLHS, Boolean not) {
		super(columnReferenceLHS, not);
	}

	@Override
	public String getCompareValue() {
		return "NULL";
	}
}
