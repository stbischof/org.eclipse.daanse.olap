/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2018 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * jhyde, 12 September, 2002
 * ---- All changes after Fork in 2023 ------------------------
 * 
 * Project: Eclipse daanse
 * 
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.daanse.olap.api.element.Property;
import org.eclipse.daanse.olap.api.formatter.MemberPropertyFormatter;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.element.PropertyBase;
import org.eclipse.daanse.olap.query.component.FormulaImpl;

public final class StandardProperty extends PropertyBase {

	private StandardProperty(String name, Datatype type, boolean internal, boolean member, boolean cell,
			String description) {
		super(name, type, internal, member, cell, description);
	}

	/**
	 * For properties which have synonyms, maps from the synonym to the property.
	 */
	private static final Map<String, StandardProperty> mapSynonymsProperty = new HashMap<>();

	/**
	 * Map of upper-case names to property definitions, for case-insensitive match.
	 * Also contains synonyms.
	 */
	private static final Map<String, StandardProperty> mapUpperNameToProperty = new HashMap<>();

	private static final Map<String, StandardProperty> mapNameToProperty = new HashMap<>();

	/**
	 * Definition of the internal property which holds the parsed format string (an
	 * object of type {@link Expression}).
	 */
	public static final StandardProperty FORMAT_EXP_PARSED = new StandardProperty("$format_exp", Datatype.TYPE_OTHER,
			true, false, false, null);

	/**
	 * Definition of the internal property which holds the aggregation type. This is
	 * automatically set for stored measures, based upon their SQL aggregation.
	 */
	public static final StandardProperty AGGREGATION_TYPE = new StandardProperty("$aggregation_type",
			Datatype.TYPE_OTHER, true, false, false, null);

	/**
	 * Definition of the internal property which holds a member's name.
	 */
	public static final StandardProperty NAME = new StandardProperty("$name", Datatype.TYPE_STRING, true,
			false, false, null);

	/**
	 * Definition of the internal property which holds a member's caption.
	 */
	public static final StandardProperty CAPTION = new StandardProperty("$caption", Datatype.TYPE_STRING, true, false,
			false, null);

	/**
	 * Definition of the internal property which returns a calculated member's
	 * {@link FormulaImpl} object.
	 */
	public static final StandardProperty FORMULA = new StandardProperty("$formula", Datatype.TYPE_OTHER, true, false,
			false, null);

	/**
	 * Definition of the internal property which describes whether a calculated
	 * member belongs to a query or a cube.
	 */
	public static final StandardProperty MEMBER_SCOPE = new StandardProperty("$member_scope", Datatype.TYPE_OTHER, true,
			true, false, null);

	/**
	 * Definition of the property which holds the name of the current catalog.
	 */
	public static final StandardProperty CATALOG_NAME = new StandardProperty("CATALOG_NAME", Datatype.TYPE_STRING,
			false, true, false, "Optional. The name of the catalog to which this member belongs. "
					+ "NULL if the provider does not support catalogs.");

	/**
	 * Definition of the property which holds the name of the current schema.
	 */
	public static final StandardProperty SCHEMA_NAME = new StandardProperty("SCHEMA_NAME", Datatype.TYPE_STRING, false,
			true, false, "Optional. The name of the schema to which this member belongs. "
					+ "NULL if the provider does not support schemas.");

	/**
	 * Definition of the property which holds the name of the current cube.
	 */
	public static final StandardProperty CUBE_NAME = new StandardProperty("CUBE_NAME", Datatype.TYPE_STRING, false,
			true, false, "Required. Name of the cube to which this member belongs.");

	/**
	 * Definition of the property which holds the unique name of the current
	 * dimension.
	 */
	public static final StandardProperty DIMENSION_UNIQUE_NAME = new StandardProperty("DIMENSION_UNIQUE_NAME",
			Datatype.TYPE_STRING, false, true, false,
			"Required. Unique name of the dimension to which this member "
					+ "belongs. For providers that generate unique names by "
					+ "qualification, each component of this name is delimited.");

	/**
	 * Definition of the property which holds the unique name of the current
	 * hierarchy.
	 */
	public static final StandardProperty HIERARCHY_UNIQUE_NAME = new StandardProperty("HIERARCHY_UNIQUE_NAME",
			Datatype.TYPE_STRING, false, true, false,
			"Required. Unique name of the hierarchy. If the member belongs "
					+ "to more than one hierarchy, there is one row for each hierarchy "
					+ "to which it belongs. For providers that generate unique names "
					+ "by qualification, each component of this name is delimited.");

	/**
	 * Definition of the property which holds the unique name of the current level.
	 */
	public static final StandardProperty LEVEL_UNIQUE_NAME = new StandardProperty("LEVEL_UNIQUE_NAME",
			Datatype.TYPE_STRING, false, true, false,
			"Required. Unique name of the level to which the member belongs. "
					+ "For providers that generate unique names by qualification, "
					+ "each component of this name is delimited.");

	/**
	 * Definition of the property which holds the ordinal of the current level.
	 */
	public static final StandardProperty LEVEL_NUMBER = new StandardProperty("LEVEL_NUMBER", Datatype.TYPE_STRING,
			false, true, false,
			"Required. The distance of the member from the root of the " + "hierarchy. The root level is zero.");

	/**
	 * Definition of the property which holds the ordinal of the current member.
	 */
	public static final StandardProperty MEMBER_ORDINAL = new StandardProperty("MEMBER_ORDINAL", Datatype.TYPE_NUMERIC,
			false, true, false,
			"Required. Ordinal number of the member. Sort rank of the member "
					+ "when members of this dimension are sorted in their natural sort "
					+ "order. If providers do not have the concept of natural "
					+ "ordering, this should be the rank when sorted by MEMBER_NAME.");

	/**
	 * Definition of the property which holds the name of the current member.
	 */
	public static final StandardProperty MEMBER_NAME = new StandardProperty("MEMBER_NAME", Datatype.TYPE_STRING, false,
			true, false, "Required. Name of the member.");

	/**
	 * Definition of the property which holds the unique name of the current member.
	 */
	public static final StandardProperty MEMBER_UNIQUE_NAME = new StandardProperty("MEMBER_UNIQUE_NAME",
			Datatype.TYPE_STRING, false, true, false, "Required. Unique name of the member. For providers that "
					+ "generate unique names by qualification, each component of " + "this name is delimited.");

	/**
	 * Definition of the property which holds the type of the member.
	 */
	public static final StandardProperty MEMBER_TYPE = new StandardProperty("MEMBER_TYPE", Datatype.TYPE_NUMERIC, false,
			true, false,
			"Required. Type of the member. Can be one of the following values: "
					+ "MDMEMBER_TYPE_REGULAR, MDMEMBER_TYPE_ALL, " + "MDMEMBER_TYPE_FORMULA, MDMEMBER_TYPE_MEASURE, "
					+ "MDMEMBER_TYPE_UNKNOWN. MDMEMBER_TYPE_FORMULA takes precedence "
					+ "over MDMEMBER_TYPE_MEASURE. Therefore, if there is a formula "
					+ "(calculated) member on the Measures dimension, it is listed as " + "MDMEMBER_TYPE_FORMULA.");

	/**
	 * Definition of the property which holds the GUID of the member
	 */
	public static final StandardProperty MEMBER_GUID = new StandardProperty("MEMBER_GUID", Datatype.TYPE_STRING, false,
			true, false, "Optional. Member GUID. NULL if no GUID exists.");

	/**
	 * Definition of the property which holds the label or caption associated with
	 * the member, or the member's name if no caption is defined.
	 *
	 *
	 * "CAPTION" is a synonym for this property.
	 */
	public static final StandardProperty MEMBER_CAPTION = new StandardProperty("MEMBER_CAPTION", Datatype.TYPE_STRING,
			false, true, false, "Required. A label or caption associated with the member. Used "
					+ "primarily for display purposes. If a caption does not exist, " + "MEMBER_NAME is returned.");

	/**
	 * Definition of the property which holds the number of children this member
	 * has.
	 */
	public static final StandardProperty CHILDREN_CARDINALITY = new StandardProperty("CHILDREN_CARDINALITY",
			Datatype.TYPE_NUMERIC, false, true, false,
			"Required. Number of children that the member has. This can be an "
					+ "estimate, so consumers should not rely on this to be the exact "
					+ "count. Providers should return the best estimate possible.");

	/**
	 * Definition of the property which holds the distance from the root of the
	 * hierarchy of this member's parent.
	 */
	public static final StandardProperty PARENT_LEVEL = new StandardProperty("PARENT_LEVEL", Datatype.TYPE_NUMERIC,
			false, true, false, "Required. The distance of the member's parent from the root level "
					+ "of the hierarchy. The root level is zero.");

	/**
	 * Definition of the property which holds the Name of the current catalog.
	 */
	public static final StandardProperty PARENT_UNIQUE_NAME = new StandardProperty("PARENT_UNIQUE_NAME",
			Datatype.TYPE_STRING, false, true, false,
			"Required. Unique name of the member's parent. NULL is returned "
					+ "for any members at the root level. For providers that generate "
					+ "unique names by qualification, each component of this name is " + "delimited.");

	/**
	 * Definition of the property which holds the number of parents that this member
	 * has. Generally 1, or 0 for root members.
	 */
	public static final StandardProperty PARENT_COUNT = new StandardProperty("PARENT_COUNT", Datatype.TYPE_NUMERIC,
			false, true, false, "Required. Number of parents that this member has.");

	/**
	 * Definition of the property which holds the description of this member.
	 */
	public static final StandardProperty DESCRIPTION_PROPERTY = new StandardProperty("DESCRIPTION",
			Datatype.TYPE_STRING, false, true, false, "Optional. A human-readable description of the member.");

	/**
	 * Definition of the internal property which holds the name of the system
	 * property which determines whether to show a member (especially a measure or
	 * calculated member) in a user interface such as JPivot.
	 */
	public static final StandardProperty VISIBLE = new StandardProperty("$visible", Datatype.TYPE_BOOLEAN, true, false,
			false, null);

	/**
	 * Definition of the property which holds the name of the class which formats
	 * cell values of this member.
	 *
	 *
	 * The class must implement the {@link org.eclipse.daanse.olap.api.formatter.CellFormatter} interface.
	 *
	 *
	 * Despite its name, this is a member property.
	 */
	public static final StandardProperty CELL_FORMATTER = new StandardProperty("CELL_FORMATTER", Datatype.TYPE_STRING,
			false, true, false, "Name of the class which formats cell values of this member.");

	/**
	 * Definition of the property which holds the name of the scripting language in
	 * which a scripted cell formatter is implemented, e.g. 'JavaScript'.
	 *
	 *
	 * Despite its name, this is a member property.
	 */
	public static final StandardProperty CELL_FORMATTER_SCRIPT_LANGUAGE = new StandardProperty(
			"CELL_FORMATTER_SCRIPT_LANGUAGE", Datatype.TYPE_STRING,

			false, true, false,
			"Name of the scripting language in which a scripted cell formatter" + "is implemented, e.g. 'JavaScript'.");

	/**
	 * Definition of the property which holds the script with which to format cell
	 * values of this member.
	 *
	 *
	 * Despite its name, this is a member property.
	 */
	public static final StandardProperty CELL_FORMATTER_SCRIPT = new StandardProperty("CELL_FORMATTER_SCRIPT",
			Datatype.TYPE_STRING,

			false, true, false, "Name of the class which formats cell values of this member.");

	// Cell properties

	public static final StandardProperty BACK_COLOR = new StandardProperty("BACK_COLOR", Datatype.TYPE_STRING, false,
			false, true, "The background color for displaying the VALUE or FORMATTED_VALUE "
					+ "property. For more information, see FORE_COLOR and BACK_COLOR " + "Contents.");

	public static final StandardProperty CELL_EVALUATION_LIST = new StandardProperty("CELL_EVALUATION_LIST",
			Datatype.TYPE_STRING, false, false, true,
			"The semicolon-delimited list of evaluated formulas applicable to "
					+ "the cell, in order from lowest to highest solve order. For more "
					+ "information about solve order, see Understanding Pass Order and " + "Solve Order");

	public static final StandardProperty CELL_ORDINAL = new StandardProperty("CELL_ORDINAL", Datatype.TYPE_NUMERIC,
			false, false, true, "The ordinal number of the cell in the dataset.");

	public static final StandardProperty FORE_COLOR = new StandardProperty("FORE_COLOR", Datatype.TYPE_STRING, false,
			false, true, "The foreground color for displaying the VALUE or FORMATTED_VALUE "
					+ "property. For more information, see FORE_COLOR and BACK_COLOR " + "Contents.");

	public static final StandardProperty FONT_NAME = new StandardProperty("FONT_NAME", Datatype.TYPE_STRING, false,
			false, true, "The font to be used to display the VALUE or FORMATTED_VALUE " + "property.");

	public static final StandardProperty FONT_SIZE = new StandardProperty("FONT_SIZE", Datatype.TYPE_STRING, false,
			false, true, "Font size to be used to display the VALUE or FORMATTED_VALUE " + "property.");

	public static final StandardProperty FONT_FLAGS = new StandardProperty("FONT_FLAGS", Datatype.TYPE_NUMERIC, false,
			false, true,
			"The bitmask detailing effects on the font. The value is the "
					+ "result of a bitwise OR operation of one or more of the "
					+ "following constants: MDFF_BOLD  = 1, MDFF_ITALIC = 2, "
					+ "MDFF_UNDERLINE = 4, MDFF_STRIKEOUT = 8. For example, the value "
					+ "5 represents the combination of bold (MDFF_BOLD) and underline "
					+ "(MDFF_UNDERLINE) font effects.");

	/**
	 * Definition of the property which holds the formatted value of a cell.
	 */
	public static final StandardProperty FORMATTED_VALUE = new StandardProperty("FORMATTED_VALUE", Datatype.TYPE_STRING,
			false, false, true, "The character string that represents a formatted display of the " + "VALUE property.");

	/**
	 * Definition of the property which holds the format string used to format cell
	 * values.
	 */
	public static final StandardProperty FORMAT_STRING = new StandardProperty("FORMAT_STRING", Datatype.TYPE_STRING,
			false, false, true, "The format string used to create the FORMATTED_VALUE property "
					+ "value. For more information, see FORMAT_STRING Contents.");

	public static final StandardProperty NON_EMPTY_BEHAVIOR = new StandardProperty("NON_EMPTY_BEHAVIOR",
			Datatype.TYPE_STRING, false, false, true,
			"The measure used to determine the behavior of calculated members " + "when resolving empty cells.");

	/**
	 * Definition of the property which determines the solve order of a calculated
	 * member with respect to other calculated members.
	 */
	public static final StandardProperty SOLVE_ORDER = new StandardProperty("SOLVE_ORDER", Datatype.TYPE_NUMERIC, false,
			false, true, "The solve order of the cell.");

	/**
	 * Definition of the property which holds the value of a cell. Is usually
	 * numeric (since most measures are numeric) but is occasionally another type.
	 *
	 *
	 * It is also applicable to members.
	 */
	public static final StandardProperty VALUE = new StandardProperty("VALUE", Datatype.TYPE_NUMERIC, false, true, true,
			"The unformatted value of the cell.");

	/**
	 * Definition of the property which holds the datatype of a cell. Valid values
	 * are "String", "Numeric", "Integer". The property's value derives from the
	 * "datatype" attribute of the "Measure" element; if the datatype attribute is
	 * not specified, the datatype is "Numeric" by default, except measures whose
	 * aggregator is "Count", whose datatype is "Integer".
	 */
	public static final StandardProperty DATATYPE = new StandardProperty("DATATYPE", Datatype.TYPE_STRING, false, false,
			true, "The datatype of the cell.");

	/**
	 * Definition of the property which holds the level depth of a member.
	 *
	 *
	 * Caution: Level depth of members in parent-child hierarchy isn't from their
	 * levels. It's calculated from the underlying data dynamically.
	 *
	 * <p>This property is declared and not registered among the standard properties, so no name
	 * resolves to it and no member answers it; a client reads the depth of a member from the member
	 * itself. Registering it would add it to what a metadata query lists for every catalog, which is a
	 * decision of its own.
	 */
	public static final Property DEPTH = new StandardProperty("DEPTH", Datatype.TYPE_NUMERIC, true, true, false,
			"The level depth of a member");

	/**
	 * Definition of the property which holds the DISPLAY_INFO required by XML/A.
	 * Caution: This property's value is calculated based on a specified MDX query,
	 * so it's value is dynamic at runtime.
	 */
	public static final Property DISPLAY_INFO = new StandardProperty("DISPLAY_INFO", Datatype.TYPE_NUMERIC, false, true,
			false, "Display instruction of a member for XML/A");

	/**
	 * Definition of the property which holds the member key of the current member.
	 */
	public static final StandardProperty MEMBER_KEY = new StandardProperty("MEMBER_KEY", Datatype.TYPE_STRING, false,
			true, false, "Member key.");
	/**
	 * Definition of the property which holds the key of the current member.
	 */
	public static final StandardProperty KEY = new StandardProperty("KEY", Datatype.TYPE_STRING, false, true, false,
			"Key.");

	/**
	 * Definition of the internal property which holds the scenario object
	 * underlying a member of the scenario hierarchy.
	 */
	public static final StandardProperty SCENARIO = new StandardProperty("$scenario", Datatype.TYPE_OTHER, true, true,
			false, null);

	/**
	 * Definition of the property which holds the DISPLAY_FOLDER. For measures, a
	 * client tool may use this folder to display measures in groups. This property
	 * has no meaning for other members.
	 */
	public static final StandardProperty DISPLAY_FOLDER = new StandardProperty("DISPLAY_FOLDER", Datatype.TYPE_STRING,
			false, true, false, "Folder in which to display a measure");

	/**
	 * Definition of the property which holds the translation expressed as an LCID.
	 * Only valid for property translations.
	 */
	public static final Property LANGUAGE = new StandardProperty("LANGUAGE", Datatype.TYPE_NUMERIC, false, false, true,
			"The translation expressed as an LCID. Only valid for property translations.");

	/**
	 * Definition of the property which holds the format string.
	 */
	public static final StandardProperty FORMAT_EXP = new StandardProperty("FORMAT_EXP", Datatype.TYPE_STRING, true,
			true, false, null);

	/**
	 * Definition of the property which holds the format string.
	 */
	public static final StandardProperty ACTION_TYPE = new StandardProperty("ACTION_TYPE", Datatype.TYPE_NUMERIC, false,
			false, true, null);

	/**
	 * Definition of the property that holds the number of fact rows that
	 * contributed to this cell. If the cell is not drillable, returns -1.
	 *
	 *
	 * Note that this property may be expensive to compute for some cubes.
	 *
	 */
	public static final StandardProperty DRILLTHROUGH_COUNT = new StandardProperty("DRILLTHROUGH_COUNT",
			Datatype.TYPE_NUMERIC, false, false, true,
			"Number of fact rows that contributed to this cell. If the cell is " + "not drillable, value is -1.");

	public static final StandardProperty UPDATEABLE = new StandardProperty("UPDATEABLE",
			Datatype.TYPE_NUMERIC, false, false, true,
			"If the cell is " + "not updateable");

	/**
	 * The various property names which define a format string.
	 */
	public static final Set<String> FORMAT_PROPERTIES =
			Set.of("format", "format_string", "FORMAT", FORMAT_STRING.getName());

	@Override
	public MemberPropertyFormatter getFormatter() {
		return null;
	}

	public static List<StandardProperty> STANDARD_PROPERTIES = List.of(FORMAT_EXP_PARSED, AGGREGATION_TYPE,
			NAME, CAPTION, FORMULA, CATALOG_NAME, SCHEMA_NAME, CUBE_NAME, DIMENSION_UNIQUE_NAME,
			HIERARCHY_UNIQUE_NAME, LEVEL_UNIQUE_NAME, LEVEL_NUMBER, MEMBER_UNIQUE_NAME, MEMBER_NAME, MEMBER_TYPE,
			MEMBER_GUID, MEMBER_CAPTION, MEMBER_ORDINAL, CHILDREN_CARDINALITY, PARENT_LEVEL, PARENT_UNIQUE_NAME,
			PARENT_COUNT, DESCRIPTION_PROPERTY, VISIBLE, CELL_FORMATTER, CELL_FORMATTER_SCRIPT,
			CELL_FORMATTER_SCRIPT_LANGUAGE, BACK_COLOR, CELL_EVALUATION_LIST, CELL_ORDINAL, FORE_COLOR, FONT_NAME,
			FONT_SIZE, FONT_FLAGS, FORMAT_STRING, FORMATTED_VALUE, NON_EMPTY_BEHAVIOR, SOLVE_ORDER, VALUE, DATATYPE,
			MEMBER_KEY, KEY, SCENARIO, DISPLAY_FOLDER, FORMAT_EXP, ACTION_TYPE, DRILLTHROUGH_COUNT, UPDATEABLE);

	static {
		// Populate synonyms.
		mapSynonymsProperty.put("CAPTION", MEMBER_CAPTION);
		mapSynonymsProperty.put("FORMAT", FORMAT_STRING);

		for (StandardProperty property : STANDARD_PROPERTIES) {
			mapNameToProperty.put(property.getName(), property);
			// Populate map of upper-case property names.
			mapUpperNameToProperty.put(property.getName().toUpperCase(), property);
		}

		// Add synonyms.
		for (Map.Entry<String, StandardProperty> entry : mapSynonymsProperty.entrySet()) {
			mapUpperNameToProperty.put(entry.getKey().toUpperCase(), entry.getValue());
		}
	}

	/**
	 * Looks up a Property with a given name.
	 *
	 * @param name      Name of property
	 * @param matchCase Whether to perform case-sensitive match
	 * @return Property with given name, or null if not found.
	 */
	public static StandardProperty lookup(String name, boolean matchCase) {
		if (matchCase) {
			StandardProperty property = mapNameToProperty.get(name);
			if (property != null) {
				return property;
			}
			return mapSynonymsProperty.get(name);
		} else {
			// No need to check synonyms separately - the map contains them.
			return mapUpperNameToProperty.get(name.toUpperCase());
		}
	}

}
