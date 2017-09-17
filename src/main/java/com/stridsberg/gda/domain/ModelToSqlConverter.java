package com.stridsberg.gda.domain;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The <code>ModelToSqlConverter</code> class <br>
 * <br>
 * Deserializes the model and creates the sql string for update and
 * insert.<br>
 * Also generates the object array for the values from the model.
 */
public class ModelToSqlConverter {

	private Object model;

	private List<Object> params, keyParams, modelList;

	private String fieldNameString, questionMarkString, sqlString, conditionString, updateString;

	private boolean update;

	private String[] keys;

	private Object[][] multiParams;

	/**
	 * Initializes a newly created <code>ModelToSqlConverter</code>
	 *
	 * @param model
	 *            The model representing the ASW file.
	 * @param keys
	 *            The keys for locating the right row in ASW file.
	 */
	@SuppressWarnings("unchecked") ModelToSqlConverter(Object model, String... keys) {
		this.model = model;
		this.keys = keys;
		init();
		update = keys != null && keys.length > 0;
		if (model instanceof List<?>) {
			modelList = (List<Object>) model;
			convertModelListToSqlAndParams();
		} else {
			convertModelToSqlAndParams();
		}

	}

	private void init() {
		params = new ArrayList<>();
		keyParams = new ArrayList<>();
		questionMarkString = "VALUES (";
		fieldNameString = "(";
		updateString = "";
		conditionString = "WHERE ";
	}

	/**
	 * Main method for Converting Model into SQL String and to value
	 * parameters.
	 */
	private void convertModelToSqlAndParams() {

		for (Field field : model.getClass().getDeclaredFields()) {
			try {
				field.setAccessible(true);
				Object value = field.get(model);
				String fieldName = field.getName();
				// Class<?> clazz = field.getType();
				if (value != null && !fieldName.equalsIgnoreCase("serialVersionUID")) {
					if (!update) {
						// value = value == null &&
						// clazz.isAssignableFrom(String.class) ? "" : value
						// ==
						// null
						// && clazz.isAssignableFrom(BigDecimal.class) ? new
						// BigDecimal("0") : value;
						addQuestionMark();
						addNameToSql(fieldName);
						addValueToObjectArray(value);
					} else {

						if (isKey(fieldName)) {
							conditionString += fieldName + " = ?,";
							keyParams.add(value);

						} else {
							addParamAndNameToSql(fieldName);
							addValueToObjectArray(value);
						}

					}
				}

			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		sqlString = update ? getUpdateSql() : getInsertSql();
		for (Object key : keyParams) {
			addValueToObjectArray(key);
		}
	}

	/**
	 * Main method for Converting Model into SQL String and to value
	 * parameters.
	 */
	private void convertModelListToSqlAndParams() {
		int row = 0;
		boolean isKey = false;
		boolean firstModel = true;
		Field[] fields = modelList.get(0).getClass().getDeclaredFields();
		multiParams = new Object[modelList.size()][fields.length];
		for (Object model : modelList) {
			int col = 0;
			keyParams = new ArrayList<>();
			for (Field field : fields) {
				try {
					field.setAccessible(true);
					Object value = field.get(model);
					String fieldName = field.getName();
					if (!fieldName.equalsIgnoreCase("serialVersionUID")) {
						if (!update) {
							if (fieldNameString.indexOf(fieldName) == -1) {
								addQuestionMark();
								addNameToSql(fieldName);
							}
							value = value == null ? formatNullValueToObject(model, field) : value;
							addValueToMultiParams(value, row, col);
						} else {
							if (value != null) {
								isKey = isKey(fieldName);
								if (isKey) {
									if (firstModel) {
										conditionString += fieldName + " = ?,";
									}
									keyParams.add(value);
								} else {
									if (firstModel)
										addParamAndNameToSql(fieldName);
									addValueToMultiParams(value, row, col);
								}
							}
						}
					}

				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (!isKey)
					col++;
			}
			firstModel = false;
			for (Object key : keyParams) {
				addValueToMultiParams(key, row, col);
				col++;
			}
			row++;
		}
		sqlString = update ? getUpdateSql() : getInsertSql();

	}

	/**
	 * Sets a default value to a null valued variable, due to the no nulls
	 * allowed in ASW.
	 * 
	 * @param model
	 * @param field
	 * @return Default value object.
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private Object formatNullValueToObject(Object model, Field field)
			throws IllegalArgumentException, IllegalAccessException {
		Object formatedValue = null;
		try {
			formatedValue = "";
			field.set(model, formatedValue);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			formatedValue = new BigDecimal("0");
			field.set(model, formatedValue);
		}
		return formatedValue;
	}

	/**
	 * @param columnName
	 */
	private void addNameToSql(String columnName) {
		fieldNameString += columnName + ",";
	}

	/**
	 * @param value
	 */
	private void addValueToObjectArray(Object value) {
		params.add(value);
	}

	/**
	 * @param value
	 * @param row
	 * @param col
	 */
	private void addValueToMultiParams(Object value, int row, int col) {
		multiParams[row][col] = value;
	}

	public Object[][] getMultiValueParams() {
		return removeNullsFromMultiArray(multiParams);
	}

	/**
	 * Adds question mark to string
	 */
	private void addQuestionMark() {
		questionMarkString += "?,";
	}

	/**
	 * @param columnName
	 */
	private void addParamAndNameToSql(String columnName) {
		updateString += columnName + "= ?,";
	}

	/**
	 * @return the update SQL string.
	 */
	private String getUpdateSql() {
		return updateString.substring(0, updateString.lastIndexOf(",")) + " "
				+ conditionString.substring(0, conditionString.lastIndexOf(","));
	}

	/**
	 * @return the insert SQL string.
	 */
	private String getInsertSql() {
		return fieldNameString.substring(0, fieldNameString.lastIndexOf(",")) + ") "
				+ questionMarkString.substring(0, questionMarkString.lastIndexOf(",")) + ")";
	}

	private Object[][] removeNullsFromMultiArray(Object[][] multiValuedArray) {
		for (int i = 0; i < multiValuedArray.length; i++) {
			ArrayList<Object> list = new ArrayList<Object>(); // creates a
																// list to
																// store the
																// elements
																// !=
																// null
			for (int j = 0; j < multiValuedArray[i].length; j++) {
				if (multiValuedArray[i][j] != null) {
					list.add(multiValuedArray[i][j]); // elements != null
														// will be added to
														// the list.
				}
			}
			multiValuedArray[i] = list.toArray(new Object[list.size()]); // all
																			// elements
																			// from
																			// list
																			// to
																			// an
																			// array.
		}
		return multiValuedArray;
	}

	/**
	 * Checks if the field name is a key.
	 * 
	 * @param fieldName
	 * @return true if the field is a key.
	 */
	private boolean isKey(String fieldName) {
		// boolean isKey = false;
		// for (String key : keys) {
		// if (fieldName.equalsIgnoreCase(key)) {
		// isKey = true;
		// }
		// }
		
		return Arrays.asList(keys).stream().anyMatch(s -> s.equalsIgnoreCase(fieldName));
	}

	/**
	 * @return the params
	 */
	public Object[] getParams() {
		return params.toArray();
	}

	/**
	 * @return the sqlString
	 */
	public String getSqlString() {
		return sqlString;
	}

	/**
	 * @param params
	 *            the params to set
	 */
	public void setParams(List<Object> params) {
		this.params = params;
	}

	/**
	 * @param sqlString
	 *            the sqlString to set
	 */
	public void setSqlString(String sqlString) {
		this.sqlString = sqlString;
	}

}
