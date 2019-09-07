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

import org.adempiere.model.ImportValidator;
import org.adempiere.process.ImportProcess;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.Query;
import org.compiere.model.X_I_Invoice;
import org.compiere.process.ImportInvoice;
import org.erpya.lve.util.ColumnsAdded;

/**
 * 	Add Default Model Validator for import process on Location Venezuela
 * 	@author Carlos Parada, cparada@erpcya.com, ERPCyA http://www.erpcya.com
 */
public class LVEImport implements ImportValidator{

	@Override
	public void validate(ImportProcess process, Object importModel, Object targetModel, int timing) {
		
		if (process !=null
				&& importModel !=null
					&& process instanceof ImportInvoice) {
			
			if (targetModel!=null
					&& targetModel instanceof MInvoice
						&& timing == ImportValidator.TIMING_BEFORE_IMPORT) {
				X_I_Invoice impInvoice = (X_I_Invoice)importModel;
				MInvoice invoice = (MInvoice) targetModel;
				invoice.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_ControlNo, impInvoice.get_ValueAsString(ColumnsAdded.COLUMNNAME_ControlNo));
			}
			
			if (targetModel!=null
					&& targetModel instanceof MInvoiceLine
						&& timing == ImportValidator.TIMING_BEFORE_IMPORT) {
				X_I_Invoice impInvoice = (X_I_Invoice)importModel;
				MInvoiceLine invoiceLine = (MInvoiceLine) targetModel;
				MInvoice sourceInvoice = new Query(impInvoice.getCtx(), MInvoice.Table_Name, "C_BPartner_ID = ? AND DocumentNo = ?", impInvoice.get_TrxName())
											.setParameters(impInvoice.getC_BPartner_ID(),impInvoice.get_ValueAsString(ColumnsAdded.COLUMNNAME_AffectedDocumentNo))
											.first();
				if (sourceInvoice!=null
						&& sourceInvoice.get_ID()>0)
					invoiceLine.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_InvoiceToAllocate_ID, sourceInvoice.get_ID());
			}
		}
		
	}
	

}
