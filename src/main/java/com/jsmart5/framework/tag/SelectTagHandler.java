/*
 * JSmart5 - Java Web Development Framework
 * Copyright (c) 2014, Jeferson Albino da Silva, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this library. If not, see <http://www.gnu.org/licenses/>.
*/

package com.jsmart5.framework.tag;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.JspTag;

import com.jsmart5.framework.json.JsonAjax;
import com.jsmart5.framework.manager.SmartTagHandler;
import com.jsmart5.framework.tag.css3.Bootstrap;
import com.jsmart5.framework.tag.html.Div;
import com.jsmart5.framework.tag.html.Label;
import com.jsmart5.framework.tag.html.Select;

import static com.jsmart5.framework.tag.JsConstants.*;

public final class SelectTagHandler extends SmartTagHandler {
	
	private static final String SMALL = "small";

	private static final String LARGE = "large";

	private String value;

	private boolean ajax;

	private boolean multiple;

	private boolean disabled;

	private Integer tabIndex;
	
	private String label;
	
	private String leftAddOn;
	
	private String rightAddOn;
	
	private String size;

	private boolean async = false;

	private List<OptionTagHandler> options;

	private SmartTagHandler childAddOn;

	public SelectTagHandler() {
		options = new ArrayList<OptionTagHandler>();
	}

	@Override
	public boolean beforeTag() throws JspException, IOException {
		JspTag parent = getParent();
		if (parent instanceof GridTagHandler) {

			((GridTagHandler) parent).addTag(this);
			return false;
		}
		return true;
	}

	@Override
	public void validateTag() throws JspException {
		if (size != null && !size.equals(SMALL) && !size.equals(LARGE)) {
			throw new JspException("Invalid size value for select tag. Valid values are " + SMALL + ", " + LARGE);
		}
	}

	@Override
	public void executeTag() throws JspException, IOException {

		// Just to call nested tags
		JspFragment body = getJspBody();
		if (body != null) {
			body.invoke(null);
		}
		
		if (id == null) {
			id = getRandonId();
		}

		Div formGroup = null;
		Div inputGroup = null;
		
		JspTag parent = getParent();
		if (label != null || parent instanceof FormTagHandler) {
			formGroup = new Div();
			formGroup.addAttribute("class", Bootstrap.FORM_GROUP);
			
			if (parent instanceof FormTagHandler) {
				String size = ((FormTagHandler) parent).getSize();

				if (FormTagHandler.LARGE.equalsIgnoreCase(size)) {
					formGroup.addAttribute("class", Bootstrap.FORM_GROUP_LARGE);

				} else if (FormTagHandler.SMALL.equalsIgnoreCase(size)) {
					formGroup.addAttribute("class", Bootstrap.FORM_GROUP_SMALL);
				}
			}
		}

		if (label != null) {
			Label labelTag = new Label();
			labelTag.addAttribute("for", id)
					.addText((String) getTagValue(label));
			formGroup.addTag(labelTag);
		}

		if (leftAddOn != null || rightAddOn != null) {
			inputGroup = new Div();
			inputGroup.addAttribute("class", Bootstrap.INPUT_GROUP);

			if (SMALL.equals(size)) {
				inputGroup.addAttribute("class", Bootstrap.INPUT_GROUP_SMALL);
			} else if (LARGE.equals(size)) {
				inputGroup.addAttribute("class", Bootstrap.INPUT_GROUP_LARGE);
			}

			if (formGroup != null) {
				formGroup.addTag(inputGroup);
			}
		}
		
		if (leftAddOn != null) {
			if (childAddOn != null && leftAddOn.equalsIgnoreCase(childAddOn.getId())) {
				StringWriter sw = new StringWriter();
				childAddOn.setOutputWriter(sw);
				childAddOn.executeTag();
				inputGroup.addText(sw.toString());
			} else {
				Div div = new Div();
				div.addAttribute("class", Bootstrap.INPUT_GROUP_ADDON)
					.addText((String) getTagValue(leftAddOn));
				inputGroup.addTag(div);
			}
		}

		Select select = new Select();
		select.addAttribute("id", id)
			 .addAttribute("style", style)
			 .addAttribute("class", Bootstrap.FORM_CONTROL)
			 .addAttribute("name", getTagName((multiple ? J_ARRAY : J_TAG), value))
			 .addAttribute("tabindex", tabIndex)
			 .addAttribute("disabled", disabled || isEditRowTagEnabled() ? "disabled" : null)
			 .addAttribute("multiple", multiple ? "multiple" : null);
		
		if (SMALL.equals(size)) {
			select.addAttribute("class", Bootstrap.INPUT_SMALL);
		} else if (LARGE.equals(size)) {
			select.addAttribute("class", Bootstrap.INPUT_LARGE);
		}
		
		// Add the style class at last
		select.addAttribute("class", styleClass);

		appendFormValidator(select);
		appendRest(select);
		appendEvent(select);

		if (inputGroup != null) {
			inputGroup.addTag(select);
		} else if (formGroup != null) {
			formGroup.addTag(select);
		}

		if (rightAddOn != null) {
			if (childAddOn != null && rightAddOn.equalsIgnoreCase(childAddOn.getId())) {
				StringWriter sw = new StringWriter();
				childAddOn.setOutputWriter(sw);
				childAddOn.executeTag();
				inputGroup.addText(sw.toString());
			} else {
				Div div = new Div();
				div.addAttribute("class", Bootstrap.INPUT_GROUP_ADDON)
					.addText((String) getTagValue(rightAddOn));
				inputGroup.addTag(div);
			}
		}

		if (!ajaxTags.isEmpty()) {
			for (AjaxTagHandler ajax : ajaxTags) {
				appendScript(ajax.getFunction(id));
			}
		}

		if (ajax) {
			appendScript(getFunction());
		}

		for (OptionTagHandler option : options) {
			StringWriter sw = new StringWriter();
			option.setName(value);
			option.setOutputWriter(sw);
			option.executeTag();
			select.addText(sw.toString());
		}

		if (formGroup != null) {
			printOutput(formGroup.getHtml());
		} else if (inputGroup != null) {
			printOutput(inputGroup.getHtml());
		} else {
			printOutput(select.getHtml());
		}
	}

	private StringBuilder getFunction() {
		StringBuilder builder = new StringBuilder();
		builder.append("$('#").append(id).append("').bind('").append(EVENT_CHANGE).append("', function(){");

		JsonAjax jsonAjax = new JsonAjax();
		jsonAjax.setId(id);
		jsonAjax.setAsync(async);
		jsonAjax.setMethod("post");

		builder.append(JSMART_SELECT.format(getNewJsonValue(jsonAjax)));

		builder.append("});");
		return builder;
	}
	
	void setChildAddOn(SmartTagHandler childAddOn) {
		this.childAddOn = childAddOn;
	}

	void addOption(OptionTagHandler option) {
		this.options.add(option);
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setAjax(boolean ajax) {
		this.ajax = ajax;
	}

	public void setMultiple(boolean multiple) {
		this.multiple = multiple;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public void setTabIndex(Integer tabIndex) {
		this.tabIndex = tabIndex;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setLeftAddOn(String leftAddOn) {
		this.leftAddOn = leftAddOn;
	}

	public void setRightAddOn(String rightAddOn) {
		this.rightAddOn = rightAddOn;
	}

	public void setAsync(boolean async) {
		this.async = async;
	}

	public void setSize(String size) {
		this.size = size;
	}
}
