/************************************************************************************
 * Copyright (C) 2012-2026 E.R.P. Consultores y Asociados, C.A.                     *
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
package org.erpya.lve.model.validator;

import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.erpya.lve.util.LVEUtil;

/**
 * Validator to prevent voiding and reversing completed fiscal sales invoices.
 * 
 * @author Jesús Albujas, jesusramirez35000@gmail.com
 *
 */
public class FiscalSalesInvoice implements ModelValidator {

	/** Logger */
	private static CLogger log = CLogger.getCLogger(FiscalSalesInvoice.class);
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
		// Add Document Validate
		engine.addDocValidate(I_C_Invoice.Table_Name, this);
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
		return null;
	}

	@Override
	public String docValidate(PO entity, int timing) {
		if (timing == TIMING_BEFORE_VOID || timing == TIMING_BEFORE_REVERSEACCRUAL
				|| timing == TIMING_BEFORE_REVERSECORRECT) {
			if (entity.get_TableName().equals(I_C_Invoice.Table_Name)) {
				MInvoice invoice = (MInvoice) entity;
				int c_DocType_ID = invoice.getC_DocTypeTarget_ID() == 0 ? invoice.getC_DocType_ID()
						: invoice.getC_DocTypeTarget_ID();
				MDocType documentType = MDocType.get(entity.getCtx(), c_DocType_ID);

				if (invoice.isSOTrx()
						&& MInvoice.DOCSTATUS_Completed.equals(invoice.getDocStatus())
						&& documentType.get_ValueAsBoolean(LVEUtil.COLUMNNAME_IsFiscalDocument)) {

					throw new AdempiereException("@ActionNotAllowed@ - @IsFiscalDocument@ @Completed@: " + invoice.getDocumentNo());
				}
			}
		}
		return null;
	}
}
