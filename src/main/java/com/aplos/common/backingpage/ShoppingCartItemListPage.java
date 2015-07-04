package com.aplos.common.backingpage;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.aplos.common.annotations.AssociatedBean;
import com.aplos.common.beans.ShoppingCartItem;

@ManagedBean
@ViewScoped
@AssociatedBean(beanClass=ShoppingCartItem.class)
public class ShoppingCartItemListPage extends ListPage {
	private static final long serialVersionUID = -2056958945337610119L;
}
