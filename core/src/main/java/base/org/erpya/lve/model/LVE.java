/**************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                               *
 * This program is free software; you can redistribute it and/or modify it    		  *
 * under the terms version 2 or later of the GNU General Public License as published  *
 * by the Free Software Foundation. This program is distributed in the hope           *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied         *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                   *
 * See the GNU General Public License for more details.                               *
 * You should have received a copy of the GNU General Public License along            *
 * with this program; if not, printLine to the Free Software Foundation, Inc.,        *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                             *
 * For the text or an alternative of this public license, you may reach us            *
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, S.A. All Rights Reserved.  *
 * Contributor: Yamel Senih ysenih@erpya.com                                          *
 * Contributor: Carlos Parada cparada@erpya.com                                       *
 * See: www.erpya.com                                                                 *
 *************************************************************************************/
package org.erpya.lve.model;

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
import org.compiere.util.Util;
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
					invoice.set_ValueOfColumn("IsFiscalDocument", false);
				} else {
					MDocType documentType = (MDocType) invoice.getC_DocTypeTarget();
					invoice.set_ValueOfColumn("IsFiscalDocument",
							documentType.get_ValueAsBoolean("IsFiscalDocument"));
					//	Set Control No
					if(!documentType.get_ValueAsBoolean("IsSetControlNoOnPrint")
							&& Util.isEmpty(invoice.get_ValueAsString("ControlNo"))) {
						DocumentTypeSequence sequence = new DocumentTypeSequence(documentType);
						invoice.set_ValueOfColumn("ControlNo", sequence.getControlNo());
					}
				}
				//	Save
				invoice.saveEx();
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
				if(invoice.get_Value("InvoiceToAllocate_ID") != null){
					for(MInvoiceLine line : invoice.getLines()) {
						if(line.get_Value("InvoiceToAllocate_ID") == null) {
							line.set_ValueOfColumn("InvoiceToAllocate_ID", invoice.get_Value("InvoiceToAllocate_ID"));
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
