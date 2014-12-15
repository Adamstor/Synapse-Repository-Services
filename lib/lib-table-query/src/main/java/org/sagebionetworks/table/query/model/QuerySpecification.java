package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.visitors.IsAggregateVisitor;
import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;

/**
 * This matches &ltquery specification&gt in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class QuerySpecification extends SQLElement {

	SetQuantifier setQuantifier;
	SqlDirective sqlDirective;
	SelectList selectList;
	TableExpression tableExpression;

	public QuerySpecification(SetQuantifier setQuantifier, SelectList selectList, TableExpression tableExpression) {
		this(null, setQuantifier, selectList, tableExpression);
	}

	public QuerySpecification(SqlDirective sqlDirective, SetQuantifier setQuantifier, SelectList selectList, TableExpression tableExpression) {
		this.sqlDirective = sqlDirective;
		this.setQuantifier = setQuantifier;
		this.selectList = selectList;
		this.tableExpression = tableExpression;
	}

	public SqlDirective getSqlDirective() {
		return sqlDirective;
	}
	public SetQuantifier getSetQuantifier() {
		return setQuantifier;
	}
	public SelectList getSelectList() {
		return selectList;
	}
	public TableExpression getTableExpression() {
		return tableExpression;
	}
	
	public void visit(Visitor visitor) {
		visit(selectList, visitor);
		visit(tableExpression, visitor);
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		visitor.append("SELECT");
		if (sqlDirective != null) {
			visitor.append(" ");
			visitor.append(sqlDirective.name());
		}
		if(setQuantifier != null){
			visitor.append(" ");
			visitor.append(setQuantifier.name());
		}
		visitor.append(" ");
		visit(selectList, visitor);
		visitor.append(" ");
		visit(tableExpression, visitor);
	}

	public void visit(IsAggregateVisitor visitor) {
		visit(tableExpression, visitor);
		visit(selectList, visitor);
	}
}
