package net.networkdowntime.dbAnalyzer.dbModel;

import java.math.BigDecimal;

/**
 * Representation of the Column in a Database
 *  
 * @author Ryan.Wiles
 *
 */
public class Column {

	/**
	 * The name of the column
	 */
	private String name;

	/**
	 * Whether the field is nullable
	 */
	private boolean nullable;

	/**
	 * The default value of the column if any
	 */
	private String columnDefault;

	/**
	 * The data type of the column
	 */
	private String dataType;

	/**
	 * Contains the aggregation of other columns to show the formatted data type as used in DDL
	 */
	private String columnType;

	/**
	 * Identifies the position of the column in the table
	 */
	private int ordinalPosition;

	/**
	 * In MySql this is used for auto-increment
	 */
	private String extra; // used for auto_increment

	/**
	 * Any comments on the column
	 */
	private String comment;

	/**
	 * The length of the data type
	 */
	private BigDecimal length;

	/**
	 * If a floating point value, this indicates the number of digits
	 */
	private BigDecimal precision;

	/**
	 * If a floating point value, this indicates the number of decimal values
	 */
	private BigDecimal scale;

	public Column(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isNullable() {
		return nullable;
	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}

	public String getColumnDefault() {
		return columnDefault;
	}

	public void setColumnDefault(String columnDefault) {
		this.columnDefault = columnDefault;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	public String getColumnType() {
		return columnType;
	}

	public void setColumnType(String columnType) {
		this.columnType = columnType;
	}

	public int getOrdinalPosition() {
		return ordinalPosition;
	}

	public void setOrdinalPosition(int ordinalPosition) {
		this.ordinalPosition = ordinalPosition;
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public BigDecimal getLength() {
		return length;
	}

	public void setLength(BigDecimal length) {
		this.length = length;
	}

	public BigDecimal getPrecision() {
		return precision;
	}

	public void setPrecision(BigDecimal precision) {
		this.precision = precision;
	}

	public BigDecimal getScale() {
		return scale;
	}

	public void setScale(BigDecimal scale) {
		this.scale = scale;
	}

}
