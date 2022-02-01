/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 or later of the                                  *
 * GNU General Public License as published                                    *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2019 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package org.erpya.lve.util;

import java.math.BigDecimal;
import java.util.Optional;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MConversionRate;
import org.compiere.model.MCurrency;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.util.Env;

/**
 * Added for handle custom values for ADempiere core
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class OrganizationRulesUtil {
	
	/**
	 * Recaulculate Invoice Line from Order
	 * @param invoice
	 * @param invoiceLine
	 * @return
	 */
	public static final void recalculateInvoiceLineRate(MInvoice invoice, MInvoiceLine invoiceLine) {
		if(invoiceLine.getC_OrderLine_ID() > 0) {
			if(!invoiceLine.isProcessed()) {
				MOrderLine orderLine = (MOrderLine) invoiceLine.getC_OrderLine();
				MOrder order = orderLine.getParent();
				if(invoice.getC_Currency_ID() != order.getC_Currency_ID()
						&& !invoice.isReversal()) {
					int conversionTypeId = invoice.getC_ConversionType_ID();
					if(conversionTypeId <= 0) {
						conversionTypeId = order.getC_ConversionType_ID();
					}
					BigDecimal orderPriceList = Optional.ofNullable(orderLine.getPriceList()).orElse(Env.ZERO);
					BigDecimal orderPriceActual = Optional.ofNullable(orderLine.getPriceActual()).orElse(Env.ZERO);
					BigDecimal conversionRate = Optional.ofNullable(MConversionRate.getRate (order.getC_Currency_ID(),
		                    invoice.getC_Currency_ID(), invoice.getDateAcct(), conversionTypeId, invoice.getAD_Client_ID(),
		                    invoice.getAD_Org_ID()))
		            		.orElse(Env.ZERO);
					if(conversionRate.compareTo(Env.ZERO) == 0) {
						throw new AdempiereException(MConversionRate.getErrorMessage(invoice.getCtx(), "ErrorConvertingInvoiceCurrencyToBaseCurrency",
								order.getC_Currency_ID(), invoice.getC_Currency_ID(), conversionTypeId, invoice.getDateAcct(), invoice.get_TrxName()));
					}
					MCurrency currencyTo = MCurrency.get (invoice.getCtx(), invoice.getC_Currency_ID());
					BigDecimal invoicePriceList = orderPriceList.multiply(conversionRate).setScale(currencyTo.getStdPrecision(), BigDecimal.ROUND_HALF_UP);
					BigDecimal invoicePriceActual = orderPriceActual.multiply(conversionRate).setScale(currencyTo.getStdPrecision(), BigDecimal.ROUND_HALF_UP);
					//	Set Price
					invoiceLine.setPriceList(invoicePriceList);
					invoiceLine.setPrice(invoicePriceActual);
					invoiceLine.setLineNetAmt();
					invoiceLine.setTaxAmt();
				}
			}
		}
	}
}
