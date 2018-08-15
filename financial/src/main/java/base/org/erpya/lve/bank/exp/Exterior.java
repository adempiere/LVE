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
import org.compiere.model.MBank;
import org.compiere.model.MBankAccount;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPaySelection;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.model.MPaySelectionLine;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.eevolution.model.MHRMovement;
import org.eevolution.model.MHRProcess;

/**
 * 	Implementation for Export Payment from Exterior bank
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/LVE/issues/1">
 * 		@see FR [ 1 ] Initial commit</a>
 */
public class Exterior extends LVEPaymentExportList {

	/** Logger								*/
	private static CLogger	s_log = CLogger.getCLogger (Exterior.class);
	/**	Header Short Format	*/
	private final String DATE_FORMAT = "ddMMyyyy";
	
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
			SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			// 	Control Register
			StringBuffer header = new StringBuffer();
			//	Process Person Type
			String personType = "";
			String orgTaxId = processValue(orgInfo.getTaxID().replace("-", "")).trim();
			if(!Util.isEmpty(orgTaxId)){
				orgTaxId = orgTaxId.replace("-", "").trim();
				personType = orgTaxId.substring(0, 1);
				orgTaxId = getNumericOnly(orgTaxId);
				orgTaxId = rightPadding(orgTaxId, 9, "0", true);
			} else {
				addError(Msg.parseTranslation(Env.getCtx(), "@TaxID@ @NotFound@"));
			}
			//	Account No
			String bankAccountNo = processValue(bankAccount.getAccountNo()).trim();
			bankAccountNo = bankAccountNo.replace(" ", "");
			bankAccountNo = rightPadding(bankAccountNo, 20, "0", true);
			//	Payment Quantity
			String paymentQty = leftPadding(String.valueOf(checks.size()), 4, "0", true);
			//	Payment Amount
			String totalAmtAsString = String.format("%.2f", paySelection.getTotalAmt().abs()).replace(".", "").replace(",", "");
			if(totalAmtAsString.length() > 13) {
				addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ @Invalid@"));
			}
			totalAmtAsString = leftPadding(totalAmtAsString, 13, "0", true);
			//	Document Date
			String paymentRequestDate = dateFormat.format(paySelection.getPayDate());
			//	Set Value Type Register for Control Register
			String serviceCode = "01";
			//	Constant
			String constant = leftPadding("", 19, " ");
			header = new StringBuffer();
			//	Header
			header.append(personType)			//	Person Type
				.append(orgTaxId)				//  Organization Tax ID
				.append(bankAccountNo)			//	Account No
				.append(paymentQty)				//	Payment Quantity
				.append(totalAmtAsString)		//	Account No
				.append(paymentRequestDate)		//	Payment Date
				.append(serviceCode)			//	Service Code
				.append(constant);				//  Constant
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
							MBank bpBank = MBank.get(Env.getCtx(), bpAccount.getC_Bank_ID());
							//	Process Account Name
							String bPName = processValue(bpAccount.getA_Name());
							if(Optional.ofNullable(bPName).isPresent()) {
								bPName = rightPadding(bPName, 60, " ", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@A_Name@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	Payment Amount
							String amountAsString = String.format("%.2f", payselectionCheck.getPayAmt().abs()).replace(".", "").replace(",", "");
							if(amountAsString.length() > 12) {
								addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ @Invalid@"));
							}
							amountAsString = leftPadding(amountAsString, 12, "0", true);
							//	Line Description
							String description = getDetail(payselectionCheck);
							description = rightPadding(description, 120, " ", true);
							//	Routing No
							String bPRoutingNo = "";
							if(Util.isEmpty(bpBank.getRoutingNo())) {
								addError(Msg.parseTranslation(Env.getCtx(), "@RoutingNo@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							bPRoutingNo = processValue(bpBank.getRoutingNo());
							bPRoutingNo = leftPadding(bPRoutingNo, 3, "0", true);
							//	BP Account No
							String bPAccountNo = processValue(bpAccount.getAccountNo());
							if(Optional.ofNullable(bPAccountNo).isPresent()) {
								bPAccountNo = leftPadding(bPAccountNo, 20, "0", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@AccountNo@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	EMail
							String bPEmail = "";
							if(!Util.isEmpty(bpAccount.getA_EMail())) {
								bPEmail = bpAccount.getA_EMail();
							}
							bPEmail = rightPadding(bPEmail, 60, " ", true);
							//	Reference No
							String debtReferenceNo = processValue(paySelection.getDocumentNo());
							debtReferenceNo = getNumericOnly(debtReferenceNo);
							debtReferenceNo = leftPadding(debtReferenceNo, 8, "0", true);
							//	Process Person Type
							String bPPersonType = "";
							String bPTaxId = bpAccount.getA_Ident_SSN();
							if(!Util.isEmpty(bPTaxId)){
								bPTaxId = bPTaxId.replace("-", "").trim();
								bPPersonType = bPTaxId.substring(0, 1);
								bPTaxId = getNumericOnly(bPTaxId);
								bPTaxId = leftPadding(bPTaxId, 9, "0", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@BPTaxID@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							
							//	Write Credit Register
							StringBuffer line = new StringBuffer();
							line.append(Env.NL)					//	New Line
								.append(bPName)					//	BP Name
								.append(amountAsString)			// 	Payment Amount
								.append(description)			// 	Payment Description
								.append(bPRoutingNo)			//	Client Routing No
								.append(bPAccountNo)			//  BP Bank Account
								.append(bPEmail)				//	BP Email
								.append(debtReferenceNo)		//	Debts Reference No
								.append(bPPersonType)			//	Person Type
								.append(bPTaxId);				//  BP TaxID
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
		return value.replaceAll("[+^:&áàäéèëíìïóòöúùñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ$,;*/@?_\"/:]", "");
	}
}
