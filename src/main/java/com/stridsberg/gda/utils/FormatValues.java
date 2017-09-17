package com.stridsberg.gda.utils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * The <code>FormatValues</code> class <br>
 * <br>
 * This class formats / trims String values.
 */

public class FormatValues {

	/**
	 * @param models
	 * @return List of Trimmed String valued models.
	 */
	public <T> List<T> getFormatedList(List<T> models) {
		for (Object model : models) {
			for (Field field : model.getClass().getDeclaredFields()) {
				try {
					field.setAccessible(true);
					Object value = field.get(model);
					String fieldName = field.getName();
					if (value != null && !fieldName.equalsIgnoreCase("serialVersionUID")) {
						if (value instanceof String) {
							String trimmed = (String) value;
							field.set(model, trimmed.trim());
						}

					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return models;
	}

	/**
	 * @param <T>
	 * @param model
	 * @return String trimmed model.
	 */
	public <T> T getFormatedObject(T model) {
		for (Field field : model.getClass().getDeclaredFields()) {
			try {
				field.setAccessible(true);
				Object value = field.get(model);
				String fieldName = field.getName();
				if (value != null && !fieldName.equalsIgnoreCase("serialVersionUID")) {
					if (value instanceof String) {
						String trimmed = (String) value;
						field.set(model, trimmed.trim());
					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return model;
	}

	public String formatValue(String value) {
		if (value == null)
			return null;
		if (value.length() <= 0)
			return null;
		return value.trim();

	}
}
