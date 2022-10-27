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
package org.erpya.lve.bank.exp;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MBankAccount;
import org.compiere.model.MPaySelection;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 * 	Implementation for Export Payment from BNC bank for Account Payable
 * 	@author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class BNC_v3 extends LVEPaymentExportList {

	public final static char CR  = (char) 0x0D;
	public final static char LF  = (char) 0x0A; 

	public final static String CRLF  = "" + CR + LF; 
	
	/** Logger								*/
	private static CLogger	s_log = CLogger.getCLogger (BNC_v3.class);
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
			//	Format Date
			SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			// 	Control Register
			StringBuffer header = new StringBuffer();
			//	Set Value Type Register for Control Register
			String registerType = "C";
			//	Fields of Debt Register
			String paymentRequestNo = processValue(paySelection.getDocumentNo());
			paymentRequestNo = getNumericOnly(paymentRequestNo);
			paymentRequestNo = leftPadding(paymentRequestNo, 10, "0", true);
			//	Debt Account
			AtomicReference<String> debitAccount = new AtomicReference<String>(bankAccount.getAccountNo());
			if(!Util.isEmpty(debitAccount.get())) {
				debitAccount.set(leftPadding(debitAccount.get(), 20, "0", true));
			} else {
				addError(Msg.parseTranslation(Env.getCtx(), "@AccountNo@ @IsMandatory@"));
			}
			//	Payment Quantity
			String paymentQty = leftPadding(String.valueOf(checks.size()), 5, "0");
			//	Payment Amount
			String totalAmtAsString = String.format("%.2f", paySelection.getTotalAmt().abs()).replace(".", "").replace(",", "");
			if(totalAmtAsString.length() > 15) {
				addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ @Invalid@"));
			}
			totalAmtAsString = leftPadding(totalAmtAsString, 15, "0", true);
			header = new StringBuffer();
			//	Header
			header.append(registerType)			//  Type Register
				.append(paymentQty)				//  Payment Quantity
				.append(totalAmtAsString)		//	Total Amount
				.append(paymentRequestNo)		//	Batch Document No
				.append("S")					//	Error
				.append("S")					//	Send Email 
				.append("N")					//	Verify Records 
				.append("00")					//	Valid
				.append(Env.NL);
			writeLine(header.toString());
			String payDate = dateFormat.format(paySelection.getPayDate());
			//  Write Credit Note
			s_log.fine("Iterate Payments");
			checks.stream()
					.filter(paySelectionCheck -> paySelectionCheck != null)
					.forEach(paySelectionCheck -> {
						//  BPartner Info
						MBPartner bpartner = MBPartner.get(paySelectionCheck.getCtx(), paySelectionCheck.getC_BPartner_ID());
						MBPBankAccount bpAccount = getBPAccountInfo(paySelectionCheck, true);
						if(bpAccount != null) {
							//	Line Register Type
							String lineRegisterType = "D";
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
							//	Process Account Name
							String bPName = processValue(bpAccount.getA_Name());
							if(Optional.ofNullable(bPName).isPresent()) {
								bPName = rightPadding(bPName, 80, " ", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@A_Name@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	Description (Can be filled with document reference)
							String lineDescription = processValue(getDetail(paySelectionCheck));
							lineDescription = rightPadding(lineDescription, 30, " ", true);
							//	BP Account No
							String bPAccountNo = processValue(bpAccount.getAccountNo());
							if(Optional.ofNullable(bPAccountNo).isPresent()) {
								bPAccountNo = leftPadding(bPAccountNo, 20, "0", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@AccountNo@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	Payment Amount
							String amountAsString = String.format("%.2f", paySelectionCheck.getPayAmt().abs()).replace(".", "").replace(",", "");
							if(amountAsString.length() > 15) {
								addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ @Invalid@"));
							}
							amountAsString = leftPadding(amountAsString, 15, "0", true);
							//	Comments
							String comment = getDetail(paySelectionCheck);
							if(Util.isEmpty(comment)) {
								comment = "";
							}
							comment = rightPadding(comment, 60, " ", true);
							//	EMail
							String bPEmail = "";
							if(!Util.isEmpty(bpAccount.getA_EMail())) {
								bPEmail = bpAccount.getA_EMail();
							}
							bPEmail = rightPadding(bPEmail, 100, " ", true);
							//	
							String customerReference = leftPadding(bPTaxId, 10, "0");
							//	Write Credit Register
							StringBuffer line = new StringBuffer();
							line.append(Env.NL)					//	New Line
								.append(lineRegisterType)		//	Type Register
								.append(payDate)				//	Payment Date
								.append(debitAccount.get())		//	Debt Account
								.append(bPAccountNo)			//  BP Bank Account								
								.append(amountAsString)			// 	Payment Amount
								.append(comment)				//	Comment
								.append(bPPersonType)			//	Person Type
								.append(bPTaxId)				//  BP TaxID
								.append(bPName)					//	BP Name
								.append(bPEmail)				//	BP EMail
								.append(customerReference);			//	Customer Reference
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
		return value.replaceAll("[+^:&áàäéèëíìïóòöúùñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ$,;*/áéíóúÁÉÍÓÚñÑ¿¡.-]", "");
	}
}
