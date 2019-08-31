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
package org.erpya.lve.model;

import java.util.Arrays;

import org.compiere.model.I_C_BPartner;
import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.erpya.lve.util.AllocationManager;
import org.erpya.lve.util.ColumnsAdded;
import org.erpya.lve.util.DocumentTypeSequence;

/**
 * 	Add Default Model Validator for Location Venezuela
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/LVE/issues/2>
 * 		@see FR [ 2 ] Add standard LVE values</a>
 */
public class LVE implements ModelValidator {

	/**
	 * Constructor
	 */
	public LVE() {
		super();
	}

	/** Logger */
	private static CLogger log = CLogger
			.getCLogger(LVE.class);
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
		// Add Timing change in C_Order and C_Invoice
		engine.addDocValidate(MInvoice.Table_Name, this);
		
		engine.addModelChange(MInvoice.Table_Name, this);
		engine.addModelChange(MBPartner.Table_Name, this);
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
	public String docValidate(PO po, int timing) {
		//	
		if(timing == TIMING_BEFORE_COMPLETE) {
			if (po.get_TableName().equals(MInvoice.Table_Name)) {
				MInvoice invoice = (MInvoice) po;
				if(invoice.isReversal()) {
					invoice.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_IsFiscalDocument, false);
				} else {
					MDocType documentType = (MDocType) invoice.getC_DocTypeTarget();
					invoice.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_IsFiscalDocument,
							documentType.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsFiscalDocument));
					//	Set Control No
					if(!documentType.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsSetControlNoOnPrint)
							&& Util.isEmpty(invoice.get_ValueAsString(ColumnsAdded.COLUMNNAME_ControlNo))) {
						DocumentTypeSequence sequence = new DocumentTypeSequence(documentType);
						invoice.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_ControlNo, sequence.getControlNo());
					}
				}
				//	Save
				invoice.saveEx();
			}
		} else if(timing == TIMING_AFTER_COMPLETE)	{
			MInvoice invoice = (MInvoice) po;
			if(!invoice.isReversal()) {
				MDocType documentType = MDocType.get(invoice.getCtx(), invoice.getC_DocTypeTarget_ID());
				if(documentType.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsAllocateInvoice)) {
					AllocationManager allocationManager = new AllocationManager(invoice);
					Arrays.asList(invoice.getLines())
						.stream()
						.filter(invoiceLine -> invoiceLine.get_ValueAsInt(ColumnsAdded.COLUMNNAME_InvoiceToAllocate_ID) != 0)
						.forEach(invoiceLine -> {
							allocationManager.addAllocateDocument(invoiceLine.get_ValueAsInt(ColumnsAdded.COLUMNNAME_InvoiceToAllocate_ID), invoiceLine.getLineTotalAmt(), Env.ZERO, Env.ZERO);
						});
					//	Create Allocation
					allocationManager.createAllocation();
				}
			}
		}
		//
		return null;
	}

	@Override
	public String modelChange(PO po, int type) throws Exception {
		if(type == TYPE_BEFORE_NEW 
				|| type == TYPE_BEFORE_CHANGE) {
			log.fine(" TYPE_BEFORE_NEW || TYPE_BEFORE_CHANGE");
			if (po.get_TableName().equals(MInvoice.Table_Name)) {
				MInvoice invoice = (MInvoice) po;
				if(invoice.get_ValueAsInt(ColumnsAdded.COLUMNNAME_InvoiceToAllocate_ID) != 0) {
					for(MInvoiceLine line : invoice.getLines()) {
						if(line.get_ValueAsInt(ColumnsAdded.COLUMNNAME_InvoiceToAllocate_ID) == 0) {
							line.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_InvoiceToAllocate_ID, invoice.get_Value(ColumnsAdded.COLUMNNAME_InvoiceToAllocate_ID));
							line.saveEx();
						}
					}
				}
			} else if(po.get_TableName().equals(MBPartner.Table_Name)) {
				MBPartner bp = (MBPartner) po;
				if(type == TYPE_BEFORE_NEW
						|| bp.is_ValueChanged(I_C_BPartner.COLUMNNAME_Value)) {
					String taxId = bp.getTaxID();
					//	For Tax ID
					if(taxId == null) {
						bp.setTaxID(bp.getValue());
					}
				}	
			}
		} 
		return null;
	}
}
