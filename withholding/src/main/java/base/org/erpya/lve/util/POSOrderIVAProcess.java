/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                      *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                      *
 * This program is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU General Public License as published by              *
 * the Free Software Foundation, either version 3 of the License, or                 *
 * (at your option) any later version.                                               *
 * This program is distributed in the hope that it will be useful,                   *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                    *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                     *
 * GNU General Public License for more details.                                      *
 * You should have received a copy of the GNU General Public License                 *
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.erpya.lve.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.compiere.model.I_C_Order;
import org.compiere.model.MOrder;
import org.compiere.model.MPayment;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Util;
import org.spin.model.I_WH_Withholding;
import org.spin.model.MWHSetting;
import org.spin.model.MWHWithholding;
import org.spin.util.AbstractWithholdingSetting;

/**
 * 	Implementación de retención de I.V.A. para la localización de Venezuela
 * 	Esto aplica para cuando se completa una orden de ventas
 * 	generada desde un Punto de Ventas y que posee pagos y referencias de IVA
 * 	@author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 *  @contributor Carlos Parada, cparada@erpya.com, ERPCyA http://www.erpya.com
 */
public class POSOrderIVAProcess extends AbstractWithholdingSetting {

	/**	References	*/
	private List<Integer> paymentReferences = new ArrayList<Integer>();
	/**	Current order	*/
	private MOrder order;
	/**	Event	*/
	private String event;
	
	public MOrder getOrder() {
		return order;
	}

	public POSOrderIVAProcess(MWHSetting setting) {
		super(setting);
	}
	
	public String getEvent() {
		return event;
	}
	
	public List<Integer> getPaymentReferences() {
		return paymentReferences;
	}

	@Override
	public boolean isValid() {
		if(getDocument().get_ValueAsInt(I_C_Order.COLUMNNAME_C_POS_ID) <= 0) {
			return false;
		}
		event = getSetting().getEventModelValidator();
		if(Util.isEmpty(event)) {
			return false;
		}
		//	Validate Document
		if(getDocument().get_Table_ID() != I_C_Order.Table_ID) {
			return false;
		}
		order = (MOrder) getDocument();
		MTable paymentReferenceDefinition = MTable.get(getContext(), "C_POSPaymentReference");
		if(paymentReferenceDefinition == null) {
			return false;
		}
		fillReferences();
		if(paymentReferences.size() <= 0) {
			return false;
		}
		//	Default
		return true;
	}
	
	/**
	 * Fill References from order
	 */
	private void fillReferences() {
		paymentReferences = new Query(getContext(), "C_POSPaymentReference", 
				"C_Order_ID = ? AND TenderType = ? "
				+ "AND EXISTS(SELECT 1 FROM C_PaymentMethod pm "
				+ "WHERE pm.C_PaymentMethod_ID = C_POSPaymentReference.C_PaymentMethod_ID "
				+ "AND pm.IsWithholdingExempt = 'N' "
				+ "AND pm.WH_Type_ID = ?)", getTransactionName())
				.setParameters(order.getC_Order_ID(), MPayment.TENDERTYPE_CreditMemo, getSetting().getWH_Type_ID())
				.getIDsAsList();
	}
	
	@Override
	public String run() {
		MTable paymentReferenceDefinition = MTable.get(getContext(), "C_POSPaymentReference");
		if(paymentReferenceDefinition != null) {
			int invoiceId = order.getC_Invoice_ID();
			paymentReferences.forEach(paymentReferenceId -> {
				PO paymentReference = paymentReferenceDefinition.getPO(paymentReferenceId, getTransactionName());
				setWithholdingRate((BigDecimal) paymentReference.get_Value("Rate"));
				addBaseAmount((BigDecimal) paymentReference.get_Value("Base"));
				addWithholdingAmount((BigDecimal) paymentReference.get_Value("Amount"));
				setReturnValue(I_WH_Withholding.COLUMNNAME_C_Currency_ID, paymentReference.get_ValueAsInt("C_Currency_ID"));
				setReturnValue(I_WH_Withholding.COLUMNNAME_C_ConversionType_ID, paymentReference.get_ValueAsInt("C_ConversionType_ID"));
				addDescription(paymentReference.get_ValueAsString("Description"));
				setReturnValue(MWHWithholding.COLUMNNAME_IsManual, true);
				setReturnValue(I_WH_Withholding.COLUMNNAME_DateAcct, paymentReference.get_Value("PayDate"));
				if(invoiceId > 0) {
					setReturnValue(I_WH_Withholding.COLUMNNAME_SourceInvoice_ID, invoiceId);
				}
				saveResult();
				paymentReferenceDefinition.set_ValueOfColumn("IsPaid", true);
				paymentReferenceDefinition.set_ValueOfColumn("Processed", true);
				paymentReferenceDefinition.saveEx();
			});
		}
		return null;
	}
}


