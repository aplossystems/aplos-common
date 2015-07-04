package com.aplos.common;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItems;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.SelectItem;

@FacesConverter("selectItemsConverter")
public class SelectItemsConverter implements Converter {

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		UISelectItems selectItems = AplosAbstractBeanConverter.getSelectItems(component);
		SelectItem[] selectItemAry = AplosAbstractBeanConverter.getValueSelectItems(context, selectItems);
		for(SelectItem item:selectItemAry) {
			if (!item.isNoSelectionOption()) {
				Object itemValue = item.getValue();
				String convertedItemValue = getAsString(context, component, itemValue);
				if (value == null ? convertedItemValue == null : value.equals(convertedItemValue)) {
					return itemValue;
				}
			}
		}
		return null;
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		if (value == null) {
			return "";
		}

		return value.toString();
	}
}
