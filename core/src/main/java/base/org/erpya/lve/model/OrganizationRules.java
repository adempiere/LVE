/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program.	If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.erpya.lve.model;

import java.util.Arrays;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_C_Invoice;
import org.compiere.model.I_C_InvoiceLine;
import org.compiere.model.MClient;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPriceList;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.erpya.lve.util.LVEUtil;
import org.erpya.lve.util.OrganizationRulesUtil;

/**
 * Write here your change comment
 * @author Yamel Senih ysenih@erpya.com
 *
 */
public class OrganizationRules implements ModelValidator {

	/** Logger */
	private static CLogger log = CLogger.getCLogger(OrganizationRules.class);
	/** Client */
	private int clientId = -1;
	
	@Override
	public void initialize(ModelValidationEngine engine, MClient client) {
		// client = null for global validator
		if (client != null) {
			clientId = client.getAD_Client_ID();
			log.info(client.toString());
		} else {
			log.info("Initializing global validator: " + this.toString());
		}
		//	Add Persistence for IsDefault values
		engine.addModelChange(I_C_Invoice.Table_Name, this);
		engine.addModelChange(I_C_InvoiceLine.Table_Name, this);
	}
	
	@Override
	public int getAD_Client_ID() {
		return clientId;
	}

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
		log.info("AD_User_ID=" + AD_User_ID);
		return null;
	}
	
	@Override
	public String modelChange(PO entity, int type) throws Exception {
		if(type == TYPE_BEFORE_NEW
				|| type == TYPE_BEFORE_CHANGE) {
			if(entity.get_TableName().equals(I_C_InvoiceLine.Table_Name)) {
				//	For Invoice Line
				if(entity.is_new()
						|| entity.is_ValueChanged(I_C_InvoiceLine.COLUMNNAME_C_OrderLine_ID)) {
					MInvoiceLine invoiceLine = (MInvoiceLine) entity;
					MInvoice invoice = (MInvoice) invoiceLine.getC_Invoice();
					MDocType documentType = MDocType.get(entity.getCtx(), invoice.getC_DocTypeTarget_ID());
					int invoiceCurrencyId = MOrgInfo.get(entity.getCtx(), entity.getAD_Org_ID(), entity.get_TrxName()).get_ValueAsInt(LVEUtil.COLUMNNAME_LVE_InvoiceCurrency_ID);
					if(!invoice.isReversal()
							&& documentType.get_ValueAsBoolean(LVEUtil.COLUMNNAME_IsInvoicedWithOrgCurrency)
							&& invoiceCurrencyId > 0) {
						OrganizationRulesUtil.recalculateInvoiceLineRate(invoice, invoiceLine);
					}
				}
			} else if(entity.get_TableName().equals(I_C_Invoice.Table_Name)) {
				//	For Invoice
				if(entity.is_new()
						|| entity.is_ValueChanged(I_C_Invoice.COLUMNNAME_M_PriceList_ID)) {
					MInvoice invoice = (MInvoice) entity;
					MDocType documentType = MDocType.get(entity.getCtx(), invoice.getC_DocTypeTarget_ID());
					int invoiceCurrencyId = MOrgInfo.get(entity.getCtx(), entity.getAD_Org_ID(), entity.get_TrxName()).get_ValueAsInt(LVEUtil.COLUMNNAME_LVE_InvoiceCurrency_ID);
					if(!invoice.isReversal()
							&& invoice.getM_PriceList_ID() > 0
							&& documentType.get_ValueAsBoolean(LVEUtil.COLUMNNAME_IsInvoicedWithOrgCurrency)
							&& invoiceCurrencyId > 0) {
						MCurrency newCurrency = MCurrency.get(invoice.getCtx(), invoiceCurrencyId);
						MPriceList newPriceList = MPriceList.getDefault(invoice.getCtx(), invoice.isSOTrx(), newCurrency.getISO_Code());
						if(newPriceList == null) {
							throw new AdempiereException("@M_PriceList_ID@ @IsMandatory@ @C_Currency_ID@ " + newCurrency.getISO_Code());
						}
						//	Convert
						invoice.setM_PriceList_ID(newPriceList.getM_PriceList_ID());
						invoice.setC_Currency_ID(newCurrency.getC_Currency_ID());
						//	Recalculate Lines
						Arrays.asList(invoice.getLines(true)).forEach(invoiceLine -> {
							OrganizationRulesUtil.recalculateInvoiceLineRate(invoice, invoiceLine);
							invoiceLine.saveEx();
						});
					}
				}
			}
		}
		return null;
	}

	@Override
	public String docValidate(PO entity, int timing) {
		return null;
	}
}
