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

import java.util.Arrays;

import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.core.domains.models.I_C_Order;
import org.compiere.model.MInvoice;
import org.compiere.model.Query;
import org.compiere.util.Util;
import org.spin.model.MWHDefinition;
import org.spin.model.MWHSetting;
import org.spin.model.MWHWithholding;
import org.spin.util.AbstractWithholdingSetting;

/**
 * 	Implementación de retención de I.V.A. para la localización de Venezuela
 * 	Esto aplica para cuando se completa un comprobante de IVA asociado a un 
 * 	un Punto de Ventas y que posee pagos y referencias de IVA
 * 	@author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 *  @contributor Carlos Parada, cparada@erpya.com, ERPCyA http://www.erpya.com
 */
public class POSWihholdingIVAProcess extends AbstractWithholdingSetting {

	/**	Current Invoice	*/
	private MInvoice withholdingDocument;
	/**	Wihholding document	*/
	private MWHWithholding withholding;
	/**	Event	*/
	private String event;
	
	public MInvoice getWithholdingDocument() {
		return withholdingDocument;
	}

	public POSWihholdingIVAProcess(MWHSetting setting) {
		super(setting);
	}
	
	public String getEvent() {
		return event;
	}

	@Override
	public boolean isValid() {
		if(getDocument().get_Table_ID() != I_C_Invoice.Table_ID) {
			return false;
		}
		if(getDocument().get_ValueAsInt(I_C_Order.COLUMNNAME_C_POS_ID) <= 0) {
			return false;
		}
		event = getSetting().getEventModelValidator();
		if(Util.isEmpty(event)) {
			return false;
		}
		withholdingDocument = (MInvoice) getDocument();
		if(getWithholding() == null) {
			return false;
		}
		//	Default
		return true;
	}
	
	@Override
	public String run() {
		if(getWithholding() != null) {
			withholding.setC_Invoice_ID(getWithholdingDocument().getC_Invoice_ID());
			withholding.saveEx();
			//	Set values from withholding
			int documentTypeId = withholding.getWHDocType();
			if(documentTypeId > 0) {
				withholdingDocument.setC_DocTypeTarget_ID(documentTypeId);
			}
			//	Set invoice to allocate for credit memo
			if(withholding.getSourceInvoice_ID() > 0) {
				withholdingDocument.set_ValueOfColumn(LVEUtil.COLUMNNAME_InvoiceToAllocate_ID, withholding.getSourceInvoice_ID());
			}
			withholdingDocument.saveEx();
			MWHDefinition whDefinition = (MWHDefinition) withholding.getWH_Definition();
			MWHSetting whSetting = (MWHSetting) withholding.getWH_Setting();
			Arrays.asList(withholdingDocument.getLines(true)).stream().findFirst().ifPresent(withholdingDocumentLine -> {
				if (whSetting.getC_Charge_ID() > 0) {
					withholdingDocumentLine.setC_Charge_ID(whSetting.getC_Charge_ID());
				} else if (whDefinition.getC_Charge_ID() > 0) {
					withholdingDocumentLine.setC_Charge_ID(whDefinition.getC_Charge_ID());
				}
				withholdingDocumentLine.saveEx();
			});
		}
		return null;
	}
	
	/**
	 * Get Withholding from POS
	 * @return
	 */
	private MWHWithholding getWithholding() {
		if(withholding != null) {
			return withholding;
		}
		if (withholdingDocument != null)  {
			withholding = new Query(getContext(), MWHWithholding.Table_Name, "SourceInvoice_ID IS NOT NULL "
					+ "AND SourceOrder_ID = ? "
					+ "AND WH_Definition_ID = ? "
					+ "AND Processed = 'Y' "
					+ "AND IsSimulation='N' "
					+ "AND C_Invoice_ID IS NULL "
					+ "AND DocStatus IN (?,?)" , getTransactionName())
					.setParameters(withholdingDocument.getC_Order_ID(), getDefinition().getWH_Definition_ID(), MWHWithholding.DOCSTATUS_Completed, MWHWithholding.DOCSTATUS_Closed)
					.first();
		}
		return withholding;
	}
}


