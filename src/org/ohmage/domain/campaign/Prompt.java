/*******************************************************************************
 * Copyright 2012 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.domain.campaign;

import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.config.grammar.custom.ConditionValuePair;
import org.ohmage.domain.campaign.Response.NoResponse;
import org.ohmage.exception.DomainException;
import org.ohmage.util.StringUtils;

/**
 * Representation of prompt configuration data.
 * 
 * @author Joshua Selsky
 * @author John Jenkins
 */
public abstract class Prompt extends SurveyItem {
	public static final String JSON_KEY_UNIT = "unit";
	public static final String JSON_KEY_TEXT = "text";
	public static final String JSON_KEY_ABBREVIATED_TEXT = "abbreviated_text";
	public static final String JSON_KEY_EXPLANATION_TEXT = "explanation_text";
	public static final String JSON_KEY_SKIPPABLE = "skippable";
	public static final String JSON_KEY_SKIP_LABEL = "skip_label";
	public static final String JSON_KEY_PROMPT_TYPE = "prompt_type";
	public static final String JSON_KEY_DISPLAY_TYPE = "display_type";
	public static final String JSON_KEY_DISPLAY_LABEL = "display_label";

	private final String unit;
	
	private final String text;
	private final String abbreviatedText;
	private final String explanationText;
	
	private final boolean skippable;
	private final String skipLabel;
	
	/**
	 * The display types to be used by data consumers to help them better 
	 * understand how the data is composed.
	 * 
	 * @author John Jenkins
	 */
	public static enum DisplayType {
		// Most data consumers will already have a fair context on what 
		// primitives like, integer, double, and string are. They will either 
		// have enough context about the data to know that it is an integer 
		// that represents the number of, say, naps a person took per day or
		// they won't have any context in which case just telling them that it
		// is an integer value is good enough. These intermediate values seem
		// to attempt to give context without giving a good definition on what
		// the data actually is. Is a measurement an integer or double, or 
		// could it be something like a JSONObject representing a WiFi scan 
		// where each key is the WiFi point's unique identifier and each value
		// is a double representing the strength of the signal. Ultimately, I
		// feel that these values are not sufficient to an end user and that
		// values like integer, double, JSONObject create a more beneficial
		// representation.
		MEASUREMENT,
		EVENT,
		COUNT,
		CATEGORY,
		METADATA;
		
		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}
	private final DisplayType displayType;
	private final String displayLabel;
	
	/**
	 * The type of the prompt.
	 * 
	 * @author John Jenkins
	 */
	public static enum Type {
		TIMESTAMP,
		NUMBER,
		/**
		 * TODO: This should be a number or timestamp depending on the 
		 * situation, and its use should be avoided.
		 */
		HOURS_BEFORE_NOW,
		TEXT,
		SINGLE_CHOICE,
		SINGLE_CHOICE_CUSTOM,
		MULTI_CHOICE,
		MULTI_CHOICE_CUSTOM,
		PHOTO,
		REMOTE_ACTIVITY;
		
		/**
		 * Returns an all-lower-case version of the type.
		 * 
		 * @return An all-lower-case version of the type.
		 */
		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}
	private final Type type;
	
	/**
	 * This class represents a label and an optional value for a key in the XML
	 * properties for a prompt type.
	 * 
	 * @author John Jenkins
	 */
	public static final class LabelValuePair {
		public static final String JSON_KEY_LABEL = "label";
		public static final String JSON_KEY_VALUE = "value";
		
		private final String label;
		private final Number value;
		
		/**
		 * Creates a new label and, optional, value pair.
		 * 
		 * @param label The label.
		 * 
		 * @param value The value.
		 * 
		 * @throws DomainException Thrown if the 'label' is null or whitespace
		 * 						   only.
		 */
		public LabelValuePair(
				final String label, 
				final Number value) 
				throws DomainException {
			
			if(StringUtils.isEmptyOrWhitespaceOnly(label)) {
				throw new DomainException(
						"The label is null or whitespace only.");
			}
			
			this.label = label;
			this.value = value;
		}
		
		/**
		 * Returns the label.
		 * 
		 * @return The label.
		 */
		public String getLabel() {
			return label;
		}
		
		/**
		 * Returns the value.
		 * 
		 * @return The value.
		 */
		public Number getValue() {
			return value;
		}
		
		/**
		 * Returns a JSONObject that represents this label-value pair.
		 * 
		 * @return A JSONObject that represents this label-value pair.
		 * 
		 * @throws JSONException There was a problem creating the JSONObject.
		 */
		public JSONObject toJson() throws JSONException {
			JSONObject result = new JSONObject();
			
			result.put(JSON_KEY_LABEL, label);
			result.put(JSON_KEY_VALUE, value);
			
			return result;
		}

		/**
		 * Creates a unique hash code for this label-value pair.
		 * 
		 * @return A unique hash code for this label-value pair.
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((label == null) ? 0 : label.hashCode());
			return result;
		}

		/**
		 * Determines if this label-value pair are equal to another object.
		 * 
		 * @param obj The object to compare with this one.
		 * 
		 * @return True if this label-value pair equal the other object; false,
		 * 		   otherwise.
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			LabelValuePair other = (LabelValuePair) obj;
			if (label == null) {
				if (other.label != null)
					return false;
			} else if (!label.equals(other.label))
				return false;
			return true;
		}
	}

	/**
	 * Creates a new Prompt object.
	 * 
	 * @param id The prompt's unique identifier.
	 * 
	 * @param condition The condition determining when this prompt should be
	 * 					displayed.
	 * 
	 * @param unit The unit value for this prompt.
	 * 
	 * @param text The text to be displayed to the user for this prompt.
	 * 
	 * @param abbreviatedText An abbreviated version of the text to be 
	 * 						  displayed to the user for this prompt.
	 * 
	 * @param explanationText A more-verbose version of the text to be 
	 * 						  displayed to the user for this prompt.
	 * 
	 * @param skippable Whether or not this prompt may be skipped.
	 * 
	 * @param skipLabel The text to show to the user indicating that the prompt
	 * 					may be skipped.
	 * 
	 * @param displayType This prompt's {@link DisplayType}.
	 * 
	 * @param displayLabel The display label for this prompt.
	 * 
	 * @param type This prompt's {@link Type}.
	 * 
	 * @param index This prompt's index in its container's list of survey 
	 * 				items.
	 * 
	 * @throws DomainException Thrown if any of the required parameters are 
	 * 						   missing or invalid. 
	 */
	public Prompt(
			final String id, 
			final String condition, 
			final String unit,
			final String text, 
			final String abbreviatedText, 
			final String explanationText,
			final boolean skippable, 
			final String skipLabel,
			final DisplayType displayType, 
			final String displayLabel,
			final Type type, 
			final int index) 
			throws DomainException {
		
		super(id, condition, index);
		
		if(StringUtils.isEmptyOrWhitespaceOnly(text)) {
			throw new DomainException("The text cannot be null.");
		}
		if(skippable && StringUtils.isEmptyOrWhitespaceOnly(skipLabel)) {
			throw new DomainException(
					"The prompt is skippable, but the skip label is null.");
		}
		if(displayType == null) {
			throw new DomainException("The display type cannot be null.");
		}
		if(StringUtils.isEmptyOrWhitespaceOnly(displayLabel)) {
			throw new DomainException("The display label cannot be null.");
		}
		
		this.unit = unit;
		
		this.text = text;
		this.abbreviatedText = abbreviatedText;
		this.explanationText = explanationText;
		
		this.skippable = skippable;
		this.skipLabel = skipLabel;
		
		this.displayType = displayType;
		this.displayLabel = displayLabel;
		
		this.type = type;
	}
	
	/**
	 * Returns the prompt's unit.
	 * 
	 * @return The prompt's unit.
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * Returns the prompt's text.
	 * 
	 * @return The prompt's text.
	 */
	public String getText() {
		return text;
	}
	
	/**
	 * Returns the prompt's abbreviated text.
	 * 
	 * @return The prompt's abbreviated text.
	 */
	public String getAbbreviatedText() {
		return abbreviatedText;
	}
	
	/**
	 * Returns the prompt's explanation text.
	 * 
	 * @return The prompt's explanation text.
	 */
	public String getExplanationText() {
		return explanationText;
	}
	
	/**
	 * Returns whether or not this prompt may be skipped.
	 * 
	 * @return Whether or not this prompt may be skipped.
	 */
	public boolean skippable() {
		return skippable;
	}
	
	/**
	 * Returns the prompt's skip label.
	 * 
	 * @return The prompt's skip label.
	 */
	public String getSkipLabel() {
		return skipLabel;
	}
	
	/**
	 * Returns the prompt's display type.
	 * 
	 * @return The prompt's {@link DisplayType}.
	 * 
	 * @see DisplayType
	 */
	public DisplayType getDisplayType() {
		return displayType;
	}

	/**
	 * Returns the prompt's display label.
	 * 
	 * @return the prompt's display label.
	 */
	public String getDisplayLabel() {
		return displayLabel;
	}
	
	/**
	 * Returns the prompt's type.
	 * 
	 * @return The prompt's {@link Type}
	 * 
	 * @see Type
	 */
	public Type getType() {
		return type;
	}
	
	/**
	 * Returns the number of survey items contained by this prompt including
	 * itself. Prompts do not contain other survey items, so this will always
	 * return 1, indicating itself.
	 * 
	 * @return 1
	 */
	@Override
	public final int getNumSurveyItems() {
		return 1;
	}
	
	/**
	 * Returns the number of prompts contained by this prompt including itself.
	 * Prompts do not contain other prompts, so this will always be 1, 
	 * indicating itself.
	 * 
	 * @return 1
	 */
	@Override
	public final int getNumPrompts() {
		return 1;
	}
	
	/**
	 * Creates a JSONObject that represents this prompt.
	 * 
	 * @return A JSONObject representing this prompt or null if there is an
	 * 		   error.
	 * 
	 * @throws JSONException There was a problem creating the JSONObject.
	 */
	@Override
	public JSONObject toJson() throws JSONException {
		JSONObject result = super.toJson();
		
		result.put(JSON_KEY_UNIT, unit);
		result.put(JSON_KEY_TEXT, text);
		result.put(JSON_KEY_ABBREVIATED_TEXT, abbreviatedText);
		result.put(JSON_KEY_EXPLANATION_TEXT, explanationText);
		result.put(JSON_KEY_SKIPPABLE, skippable);
		result.put(JSON_KEY_SKIP_LABEL, skipLabel);
		result.put(JSON_KEY_PROMPT_TYPE, type.toString());
		result.put(JSON_KEY_DISPLAY_TYPE, displayType.toString());
		result.put(JSON_KEY_DISPLAY_LABEL, displayLabel);
		
		return result;
	}
	
	/**
	 * Validates that a condition-value pair are valid for the given prompt.
	 * All implementing prompts should call their super validateCondition() 
	 * which will handle cases when the prompt is skipped or not displayed.
	 * 
	 * @param pair The condition-value pair to validate.
	 * 
	 * @throws DomainException The value was not applicable for the prompt or
	 * 						   was invalid for some constraint on the prompt.
	 */
	public abstract void validateConditionValuePair(
			final ConditionValuePair pair)
			throws DomainException;
	
	/**
	 * Validates that some Object is a valid response value for this prompt. If
	 * so, an Object of the type that the implementing prompt expects is 
	 * returned; otherwise, null is returned.<br />
	 * <br />
	 * For example, the NumberPrompt may receive a Number, an Integer, a Long,
	 * or a Short and will convert any of those into a Long and return it. It
	 * may also take a String, which it will attempt to convert into a Long 
	 * value and return it. If it is unable to convert the value into its
	 * appropriate type, in this instance a Long object, it will return null.
	 * 
	 * @param value The value to be validated.
	 * 
	 * @return The type-appropriate object for the implementing prompt or a
	 * 		   {@link NoResponse} object.
	 * 
	 * @throws DomainException The value is not a type-appropriate object for
	 * 						   the corresponding prompt, a NoResponse object,
	 * 						   or a String that can be decoded as either of 
	 * 						   those two.
	 */
	public abstract Object validateValue(
			final Object value) 
			throws DomainException;
	
	/**
	 * Creates a PromptResponse for this prompt based on some given value.
	 * 
	 * @param value The value on which to base the PromptResponse.
	 * 
	 * @param repeatableSetIteration If this prompt is part of a repeatable 
	 * 								 set, this indicates on which iteration of
	 * 								 that repeatable set this response was
	 * 								 generated.
	 * 
	 * @return A PromptResponse object representing a response to this prompt.
	 * 
	 * @throws DomainException Thrown if the value is not valid for the prompt.
	 */
	public abstract PromptResponse createResponse(
			final Integer repeatableSetIteration, 
			final Object value)
			throws DomainException;

	/**
	 * Returns a hash code value for this prompt.
	 * 
	 * @return A hash code value representing this prompt.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((abbreviatedText == null) ? 0 : abbreviatedText.hashCode());
		result = prime * result
				+ ((displayLabel == null) ? 0 : displayLabel.hashCode());
		result = prime * result
				+ ((displayType == null) ? 0 : displayType.hashCode());
		result = prime * result
				+ ((explanationText == null) ? 0 : explanationText.hashCode());
		result = prime * result
				+ ((skipLabel == null) ? 0 : skipLabel.hashCode());
		result = prime * result + (skippable ? 1231 : 1237);
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((unit == null) ? 0 : unit.hashCode());
		return result;
	}

	/**
	 * Determines if this prompt is logically equivalent to another object.
	 * 
	 * @return True if this prompt is logically equivalent to another object;
	 * 		   false otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Prompt other = (Prompt) obj;
		if (abbreviatedText == null) {
			if (other.abbreviatedText != null)
				return false;
		} else if (!abbreviatedText.equals(other.abbreviatedText))
			return false;
		if (displayLabel == null) {
			if (other.displayLabel != null)
				return false;
		} else if (!displayLabel.equals(other.displayLabel))
			return false;
		if (displayType != other.displayType)
			return false;
		if (explanationText == null) {
			if (other.explanationText != null)
				return false;
		} else if (!explanationText.equals(other.explanationText))
			return false;
		if (skipLabel == null) {
			if (other.skipLabel != null)
				return false;
		} else if (!skipLabel.equals(other.skipLabel))
			return false;
		if (skippable != other.skippable)
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		if (type != other.type)
			return false;
		if (unit == null) {
			if (other.unit != null)
				return false;
		} else if (!unit.equals(other.unit))
			return false;
		return true;
	}
	
	/**
	 * Checks if the value of the pair is a {@link NoResponse} value and, if 
	 * so, verifies that that is a valid value for this prompt. If it is a
	 * {@link NoResponse} value, then true will be returned or an exception 
	 * will be thrown indicating that it wasn't valid for this prompt. If it is
	 * not a {@link NoResponse} value, false is returned.
	 * 
	 * @param pair The condition-value pair to check.
	 * 
	 * @return True if this is a {@link NoResponse} value; false, otherwise.
	 * 
	 * @throws DomainException The value of the pair was a {@link NoResponse}
	 * 						   value and isn't valid for this prompt.
	 */
	protected final boolean checkNoResponseConditionValuePair(
			final ConditionValuePair pair)
			throws DomainException {
		
		try {
			NoResponse noResponse = NoResponse.valueOf(pair.getValue());
			
			switch(noResponse) {
			case SKIPPED:
				if(skippable()) {
					return true;
				}
				else {
					throw new DomainException(
							"The response '" + 
								getId() +
								"' cannot be skipped, so the condition is invalid.");
				}
			
			case NOT_DISPLAYED:
				return true;
				
			default:
				throw new DomainException("Unknown 'no response' value.");
			}
				
		}
		// It is not a NoResponse value.
		catch(IllegalArgumentException e) {
			return false;
		}
	}
}