package org.sagebionetworks.table.query.model;

public class OrderByClause implements SQLElement {
	
	SortSpecificationList sortSpecificationList;

	public OrderByClause(SortSpecificationList sortSpecificationList) {
		super();
		this.sortSpecificationList = sortSpecificationList;
	}

	public SortSpecificationList getSortSpecificationList() {
		return sortSpecificationList;
	}

	@Override
	public void toSQL(StringBuilder builder) {
		builder.append("ORDER BY ");
		sortSpecificationList.toSQL(builder);
	}
	

}
