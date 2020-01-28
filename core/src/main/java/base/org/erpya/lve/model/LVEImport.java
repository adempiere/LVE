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
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.Query;
import org.compiere.model.X_I_BPartner;
import org.compiere.model.X_I_Invoice;
import org.compiere.process.ImportBPartner;
import org.compiere.process.ImportInvoice;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Msg;
import org.erpya.lve.util.ColumnsAdded;

/**
 * 	Add Default Model Validator for import process on Location Venezuela
 * 	@author Carlos Parada, cparada@erpcya.com, ERPCyA http://www.erpcya.com
 */
public class LVEImport implements ImportValidator{
	/**	Logger							*/
	protected CLogger			log = CLogger.getCLogger (getClass());
	

	@Override
	public void validate(ImportProcess process, Object importModel, Object targetModel, int timing) {
		
		if (process !=null) {
			if (importModel !=null) {
				if (process instanceof ImportInvoice) {
				
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
				}else if (process instanceof ImportBPartner) {
					X_I_BPartner impBPartner = (X_I_BPartner)importModel;
					
					if (timing == ImportValidator.TIMING_AFTER_IMPORT) {
						if (targetModel!=null
								&& targetModel instanceof MBPartner) {
							MBPartner bPartner = (MBPartner) targetModel;
							bPartner.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_PersonType, impBPartner.get_Value(ColumnsAdded.COLUMNNAME_PersonType));
							bPartner.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_IsWithholdingTaxExempt, impBPartner.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsWithholdingTaxExempt));
							bPartner.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_WithholdingTaxRate_ID, impBPartner.get_Value(ColumnsAdded.COLUMNNAME_WithholdingTaxRate_ID));
							bPartner.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_IsWithholdingRentalExempt, impBPartner.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsWithholdingRentalExempt));
							bPartner.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_IsWithholdingMunicipalExempt, impBPartner.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsWithholdingMunicipalExempt));
							bPartner.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_BusinessActivity_ID, impBPartner.get_Value(ColumnsAdded.COLUMNNAME_BusinessActivity_ID));
							bPartner.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_WithholdingMunicipalRate_ID, impBPartner.get_Value(ColumnsAdded.COLUMNNAME_WithholdingMunicipalRate_ID));
						}else if (targetModel!=null
								&& targetModel instanceof MBPartnerLocation) {
							MBPartnerLocation bPartnerLocation = (MBPartnerLocation) targetModel;
							bPartnerLocation.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_SICACode, impBPartner.get_Value(ColumnsAdded.COLUMNNAME_SICACode));
						}
					}
				}
			}else {
				if (process instanceof ImportBPartner) {
					StringBuilder sql;
					int no = 0;
					String clientCheck = ((ImportBPartner)process).getWhereClause();
					
					if (timing == ImportValidator.TIMING_BEFORE_VALIDATE) {
						//Set Withholding Tax
						sql = new StringBuilder ("UPDATE I_BPartner i "
												+ "SET WithholdingTaxRate_ID=(SELECT LVE_List_ID FROM LVE_List l"
																			+ " WHERE i.WithholdingTaxValue=l.Value AND l.AD_Client_ID IN (0, i.AD_Client_ID)"
																			+ " AND EXISTS(SELECT 1 FROM LVE_WithholdingTax wt WHERE wt.Type = 'IV' AND wt.WithholdingRateType_ID = l.LVE_ListType_ID)) "
												+ "WHERE WithholdingTaxRate_ID IS NULL AND COALESCE(i.IsWithholdingTaxExempt,'N') = 'N'"
												+ " AND I_IsImported<>'Y'").append(clientCheck);
						no = DB.executeUpdateEx(sql.toString(), process.get_TrxName());
						log.fine("Set Withholding Tax=" + no);
						
						//Set Withholding Municipal Activity
						sql = new StringBuilder ("UPDATE I_BPartner i "
												+ "SET BusinessActivity_ID=(SELECT LVE_List_ID FROM LVE_List l"
																			+ " WHERE i.BusinessActivityValue=l.Value AND l.AD_Client_ID IN (0, i.AD_Client_ID)"
																			+ " AND EXISTS(SELECT 1 FROM LVE_WithholdingTax wt WHERE wt.Type = 'IM' AND wt.WithholdingRateType_ID = l.LVE_ListType_ID)) "
												+ "WHERE BusinessActivity_ID IS NULL AND COALESCE(i.IsWithholdingMunicipalExempt,'N') = 'N'"
												+ " AND I_IsImported<>'Y'").append(clientCheck);
						no = DB.executeUpdateEx(sql.toString(), process.get_TrxName());
						log.fine("Set Withholding Municipal Activity=" + no);
						
						//Set Withholding Municipal Rate
						sql = new StringBuilder ("UPDATE I_BPartner i "
												+ "SET WithholdingMunicipalRate_ID=(SELECT LVE_ListVersion_ID FROM LVE_ListVersion lv"
																			+ " WHERE i.WithholdingMunicipalValue=lv.Name AND lv.AD_Client_ID IN (0, i.AD_Client_ID)"
																			+ " AND lv.LVE_List_ID = i.BusinessActivity_ID) "
												+ "WHERE WithholdingMunicipalRate_ID IS NULL AND COALESCE(i.IsWithholdingMunicipalExempt,'N') = 'N'"
												+ " AND I_IsImported<>'Y'").append(clientCheck);
						no = DB.executeUpdateEx(sql.toString(), process.get_TrxName());
						log.fine("Set Withholding Municipal Rate=" + no);
					}else if (timing == ImportValidator.TIMING_AFTER_VALIDATE) {
						//Set Withholding Tax Error
						sql = new StringBuilder ("UPDATE I_BPartner i "
												+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=" + Msg.parseTranslation(process.getCtx(), "@Invalid@ @WithholdingTaxRate_ID@") + ", ' "
												+ "WHERE WithholdingTaxRate_ID IS NULL AND WithholdingTaxValue IS NOT NULL"
												+ " AND I_IsImported<>'Y'").append(clientCheck);
						no = DB.executeUpdateEx(sql.toString(), process.get_TrxName());
						log.config("Invalid Withholding Tax Rate=" + no);
						
						//Set Withholding Municipal Activity
						sql = new StringBuilder ("UPDATE I_BPartner i "
												+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=" + Msg.parseTranslation(process.getCtx(), "@Invalid@ @BusinessActivity_ID@") + ", ' "
												+ "WHERE BusinessActivity_ID IS NULL AND BusinessActivityValue IS NOT NULL"
												+ " AND I_IsImported<>'Y'").append(clientCheck);
						no = DB.executeUpdateEx(sql.toString(), process.get_TrxName());
						log.config("Invalid Withholding Municipal Activity=" + no);
						
						//Set Withholding Municipal Rate
						sql = new StringBuilder ("UPDATE I_BPartner i "
												+ "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=" + Msg.parseTranslation(process.getCtx(), "@Invalid@ @WithholdingMunicipalRate_ID@") + ", ' "
												+ "WHERE WithholdingMunicipalRate_ID IS NULL AND WithholdingMunicipalValue IS NOT NULL"
												+ " AND I_IsImported<>'Y'").append(clientCheck);
						no = DB.executeUpdateEx(sql.toString(), process.get_TrxName());
						log.config("Invalid Withholding Municipal Rate=" + no);
					}
				}
			}
		}
		
	}
	

}
