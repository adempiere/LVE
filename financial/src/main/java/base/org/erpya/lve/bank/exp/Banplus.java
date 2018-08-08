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
package org.erpya.lve.bank.exp;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MBankAccount;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPaySelection;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 * 	Implementation for Export Payment from Banplus bank
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/LVE/issues/1">
 * 		@see FR [ 1 ] Initial commit</a>
 */
public class Banplus extends LVEPaymentExportList {

	/** Logger								*/
	static private CLogger	s_log = CLogger.getCLogger (Banplus.class);
	/**	separator	*/
	private final String SEPARATOR = ";";
	/**	Header Short Format	*/
	private final String HEADER_SHORT_DATE_FORMAT = "yyyyMMdd";
	
	
	@Override
	public int exportToFile(List<MPaySelectionCheck> checks, File file, StringBuffer error) {
		if (checks == null || checks.size() == 0)
			return 0;
		//	
		try {
			s_log.fine("Delete file if exist");
			openFileWriter(file, checks);
			//	Write header
			MPaySelection paySelection = (MPaySelection) checks.get(0).getC_PaySelection();
			MBankAccount bankAccount = (MBankAccount) paySelection.getC_BankAccount();
			MOrgInfo orgInfo = MOrgInfo.get(paySelection.getCtx(), paySelection.getAD_Org_ID(), paySelection.get_TrxName());
			//	Format Date
			SimpleDateFormat shortFormat = new SimpleDateFormat(HEADER_SHORT_DATE_FORMAT);
			//	Fields of Control Register (fixed data)
			String paymentRequestNo = processValue(paySelection.getDocumentNo());
			paymentRequestNo = paymentRequestNo.substring(0, paymentRequestNo.length() >= 10? 10: paymentRequestNo.length());
			//	Fields of Debt Register
			String debtReferenceNo = processValue(paySelection.getDocumentNo());
			debtReferenceNo = debtReferenceNo.substring(0, debtReferenceNo.length() >= 8? 8: debtReferenceNo.length());
			debtReferenceNo = debtReferenceNo.replaceAll("\\D+","");
			debtReferenceNo = rightPadding(debtReferenceNo, 30, " ");
			//	Process Organization Tax ID
			String orgTaxId = processValue(orgInfo.getTaxID().replace("-", "")).trim();
			orgTaxId = orgTaxId.substring(0, orgTaxId.length() >= 12? 12: orgTaxId.length());
			//	Payment Amount
			String totalAmtAsString = String.format("%.2f", paySelection.getTotalAmt().abs()).replace(".", "").replace(",", "");
			if(totalAmtAsString.length() <= 18) {
				totalAmtAsString = totalAmtAsString.substring(0, totalAmtAsString.length() >= 15? 15: totalAmtAsString.length());
			} else {
				addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ > @InValid@"));
			}
			//	Account No
			String bankAccountNo = processValue(bankAccount.getAccountNo()).trim();
			bankAccountNo = bankAccountNo.substring(0, (bankAccountNo.length() >= 20 ? 20: bankAccountNo.length()));
			bankAccountNo = bankAccountNo.replace(" ", "");
			//	Payments Generated
			String payGenerated = String.valueOf(checks.size());
			if(payGenerated.length() > 3) {
				addError(Msg.parseTranslation(Env.getCtx(), "@Qty@ > @InValid@"));
			}
			
			String payDate = shortFormat.format(paySelection.getPayDate());
			StringBuffer header = new StringBuffer();
			//	Debt Note
			header.append(orgTaxId)					//  Organization Tax ID
				.append(SEPARATOR)					//	Separator
				.append(bankAccountNo)				//	Bank Account No
				.append(SEPARATOR)					//	Separator
				.append(payGenerated)				//  Payments Generated
				.append(SEPARATOR)					//	Separator
				.append(totalAmtAsString)			//  Total Amount
				.append(SEPARATOR)					//	Separator
				.append(payDate)					//  Payment Date
				.append(SEPARATOR)					//	Separator
				.append(paymentRequestNo);			//  DocumentNo
			writeLine(header.toString());
			//  Write Credit Note
			s_log.fine("Iterate Payments");
			checks.stream()
					.filter(paySelectionCheck -> paySelectionCheck != null)
					.forEach(payselectionCheck -> {
						//  BPartner Info
						MBPartner bpartner = MBPartner.get(payselectionCheck.getCtx(), payselectionCheck.getC_BPartner_ID());
						MBPBankAccount bpAccount = getBPAccountInfo(payselectionCheck, true);
						if(bpAccount != null) {
							//	Process Business Partner Account No
							String bPAccountNo = processValue(bpAccount.getAccountNo());
							String bPTaxId = bpAccount.getA_Ident_SSN();
							//	Process Person Type
							String personType = "";
							if(!Util.isEmpty(bPTaxId)){
								bPTaxId = bPTaxId.replace("-", "").trim();
								personType = bPTaxId.substring(0, 1);
								//	Evaluate
								if(personType.equals("V"))
									personType = "01";
								else if(personType.equals("E"))
									personType = "08";
								else
									personType = "04";
								bPTaxId = bPTaxId.replaceAll("\\D+","");
								bPTaxId = bPTaxId.substring(0, bPTaxId.length() >= 8? 8: bPTaxId.length());
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@BPTaxID@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	Process Account Name
							String bPName = processValue(bpAccount.getA_Name());
							if(Optional.ofNullable(bPName).isPresent()) {
								bPName = bPName.substring(0, bPName.length() >= 40? 40: bPName.length());
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@A_Name@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							if(Optional.ofNullable(bPAccountNo).isPresent()) {
								bPAccountNo = bPAccountNo.substring(0, bPAccountNo.length() >= 20? 20: bPAccountNo.length());
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@AccountNo@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	Process Document No
							String documentNo = processValue(payselectionCheck.getDocumentNo());
							documentNo = documentNo.substring(0, documentNo.length() >= 8? 8: documentNo.length());
							//	Payment Amount
							String amountAsString = String.format("%.2f", payselectionCheck.getPayAmt().abs()).replace(".", "").replace(",", "");
							if(amountAsString.length() > 15) {
								addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ > @Valid@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	EMail
							String bPEmail = "";
							if(!Util.isEmpty(bpAccount.getA_EMail())) {
								bPEmail = bpAccount.getA_EMail();
							}
							bPEmail = bPEmail.substring(0, bPEmail.length() >= 70? 70: bPEmail.length());
							//	Make Payment
							String makePayment = "SI";
							//	Write Credit Register
							StringBuffer line = new StringBuffer();
							line.append(Env.NL)						//	New Line
								.append(personType)					//	Type Register	
								.append(SEPARATOR)					//	Separator
								.append(bPTaxId)					// 	BP TaxID
								.append(SEPARATOR)					//	Separator
								.append(bPName)						//	BP Name
								.append(SEPARATOR)					//	Separator
								.append(bPAccountNo)				//  BP Bank Account
								.append(SEPARATOR)					//	Separator								
								.append(amountAsString)				//	Payment Amount
								.append(SEPARATOR)					//	Separator
								.append(documentNo) 				//	Document No
								.append(SEPARATOR)					//	Separator
								.append(bPEmail) 					//	Email
								.append(SEPARATOR)					//	Separator
								.append(makePayment); 				//	Make Payment
							s_log.fine("Write Line");
							writeLine(line.toString());
						} else {
							addError(Msg.parseTranslation(Env.getCtx(), "@C_BP_BankAccount_ID@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
						}
			});
			//	
			closeFileWriter();
		} catch (Exception e) {
			addError(e.toString());
		} finally {
			closeFileWriter();
			error.append(getError());
			if(error.length() > 0) {
				setExportedPayments(-1);
			}
		}
		return getExportedPayments();
	}
	
	@Override
	public String processValue(String value) {
		if(Util.isEmpty(value)) {
			return value;
		}
		//	
		return value.replaceAll("[+^:&áàäéèëíìïóòöúùñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ$,;*/]", "");
	}
}
