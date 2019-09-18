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
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MBank;
import org.compiere.model.MBankAccount;
import org.compiere.model.MClient;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPaySelection;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.erpya.lve.util.ColumnsAdded;

/**
 * 	Implementation for Export Payment from Banesco bank
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/LVE/issues/1">
 * 		@see FR [ 1 ] Initial commit</a>
 */
public class BFC extends LVEPaymentExportList {

	/** Logger								*/
	static private CLogger	s_log = CLogger.getCLogger (BFC.class);
	/**	Header Short Format	*/
	private final String SHORT_DATE_FORMAT = "yyyyMMdd";
	/**	Header Format	*/
	private final String TIME_FORMAT = "HHmmss";
	/**	Payment Quantity	*/
	private int paymentQty = 1;
	
	
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
			MClient client = MClient.get(orgInfo.getCtx(), orgInfo.getAD_Client_ID());
			MBank bank = MBank.get(bankAccount.getCtx(), bankAccount.getC_Bank_ID());
			//	Time Format
			SimpleDateFormat dateFormat = new SimpleDateFormat(SHORT_DATE_FORMAT);
			//	Date Format
			SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT);
			//	Fields of Control Register (fixed data)
			String sequence = leftPadding("", 6, "0");
			//	Fields of Control Register (fixed data)
			String sequence2 = leftPadding("", 14, "0");
			//	Now
			Date now = new Date(System.currentTimeMillis());
			//	Bank Client No
			String bankClientNo = "";
			if(!Util.isEmpty(bank.get_ValueAsString("BankClientNo"))) {
				bankClientNo = processValue(bank.get_ValueAsString(ColumnsAdded.COLUMNNAME_BankClientNo));
				bankClientNo = leftPadding(bankClientNo, 6, "0", true);
			} else {
				addError(Msg.parseTranslation(Env.getCtx(), "@BankClientNo@ @NotFound@"));
			}
			//	Constant Service Code
			String serviceCode = leftPadding("77", 6, "0");
			//	Constant space
			String sequence3 = leftPadding("", 1, " ");
			//	Account Type
			String accountType = "CC";
			if(bankAccount.getBankAccountType().equals(MBankAccount.BANKACCOUNTTYPE_Savings)) {
				accountType = "CA";
			}
			//	Account No
			String bankAccountNo = processValue(bankAccount.getAccountNo()).trim();
			bankAccountNo = bankAccountNo.replace(" ", "");
			bankAccountNo = leftPadding(bankAccountNo, 22, "0", true);
			//	Constant space
			String sequence4 = leftPadding("", 3, " ");
			//	Constant space
			String sequence5 = leftPadding("", 34, "0");
			//	Process Organization Tax ID
			String orgTaxId = processValue(orgInfo.getTaxID().replace("-", "")).trim();
			orgTaxId = leftPadding(orgTaxId, 10, " ", true);
			//	Constant space
			String sequence6 = leftPadding("", 48, "0");
			//	End Fields 
			// 	Control Register
			StringBuffer header = new StringBuffer();
			//	
			header.append(sequence)										//	Sequence (Constant)
				.append(dateFormat.format(now))							//	Current Date
				.append(timeFormat.format(now))							//	Current Time
				.append(dateFormat.format(paySelection.getPayDate()))	//	Payment Date
				.append(timeFormat.format(paySelection.getPayDate()))	//	Payment Time
				.append(sequence2)										//	Sequence (Constant)
				.append(bankClientNo)									//	Bank Client No
				.append(serviceCode)									//	Service Code
				.append(sequence3)										//	Sequence (Constant)
				.append(accountType)									//	Account Type
				.append(bankAccountNo)									//	Bank Account No
				.append(sequence4)										//	Sequence (Constant)
				.append(sequence5)										//	Sequence (Constant)
				.append(orgTaxId)										//	Organization Tax ID
				.append(sequence6);										//	Sequence (Constant)
			//	Write Line
			writeLine(header.toString());
			//  Write Credit Note
			s_log.fine("Iterate Payments");
			checks.stream()
					.filter(paySelectionCheck -> paySelectionCheck != null)
					.forEach(paySelectionCheck -> {
						//  BPartner Info
						MBPartner bpartner = MBPartner.get(paySelectionCheck.getCtx(), paySelectionCheck.getC_BPartner_ID());
						MBPBankAccount bpAccount = getBPAccountInfo(paySelectionCheck, true);
						if(bpAccount != null) {
							//	Credit Register
							String lineNo = leftPadding(String.valueOf(getPaymentQty()), 6, "0");
							//	Constant space
							String constant = leftPadding("", 1, " ");
							//	Account Type
							String vendorAccountType = "CC";
							if(!Util.isEmpty(bpAccount.getBankAccountType())
									&& bpAccount.getBankAccountType().equals(MBankAccount.BANKACCOUNTTYPE_Savings)) {
								vendorAccountType = "CA";
							}
							//	Process Business Partner Account No
							String bPAccountNo = processValue(bpAccount.getAccountNo());
							if(Optional.ofNullable(bPAccountNo).isPresent()) {
								bPAccountNo = leftPadding(bPAccountNo, 20, "0", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@AccountNo@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	Process Person Type
							String personType = "";
							String bPTaxId = bpAccount.getA_Ident_SSN();
							if(!Util.isEmpty(bPTaxId)){
								bPTaxId = bPTaxId.replace("-", "").trim();
								personType = bPTaxId.substring(0, 1);
								bPTaxId = bPTaxId.replaceAll("\\D+","");
								bPTaxId = leftPadding(bPTaxId, 10, "0", true);
								if(isNumeric(personType)) {
									personType = "V";
								}
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@BPTaxID@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	Constant space
							String constant2 = leftPadding("", 20, "0");
							//	Payment Amount
							String amountAsString = String.format("%.2f", paySelectionCheck.getPayAmt().abs()).replace(".", "").replace(",", "");
							if(amountAsString.length() > 15) {
								addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ > @Invalid@: " + bpartner.getValue() + " - " + bpartner.getName()));
							} else {
								amountAsString = leftPadding(amountAsString, 15, "0", true);
							}
							//	Constant
							String constant3 = "C";
							//	Constant
							String constant4 = leftPadding("", 1, "0");
							//	Process Account Name
							String bPName = processValue(bpAccount.getA_Name());
							if(Optional.ofNullable(bPName).isPresent()) {
								bPName = rightPadding(bPName, 40, " ", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@A_Name@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	Constant
							String constant5 = leftPadding("", 4, "0");
							//	Constant
							String constant6 = leftPadding("", 40, " ");
							//	Constant
							String constant7 = leftPadding("", 9, "0");
							//	Write Credit Register
							StringBuffer line = new StringBuffer();
							line.append(Env.NL)					//	New Line
								.append(lineNo)					//	Line No
								.append(constant)				//	Sequence (Constant)
								.append(vendorAccountType)		// 	Vendor Account Type
								.append(bPAccountNo)			//	Bank Account No
								.append(personType)				//  Person Type
								.append(bPTaxId)				// 	Tax ID
								.append(constant2)				// 	Sequence (Constant)
								.append(amountAsString)			// 	Amount
								.append(constant3)				//	Sequence (Constant)
								.append(constant4)				//	Sequence (Constant)
								.append(bPName)					//  BP Name
								.append(constant5)				//  Sequence (Constant)
								.append(constant6)				// 	Sequence (Constant)
								.append(constant7);				//	Sequence (Constant)
							s_log.fine("Write Line");
							writeLine(line.toString());
						} else {
							addError(Msg.parseTranslation(Env.getCtx(), "@C_BP_BankAccount_ID@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
						}
						//	
						addPaymentToCounter();
			});
			//	Totals Register
			//	Constant
			String constant = leftPadding("", 6, "9", true);
			//	Client Name
			String clientName = processValue(client.getName());
			clientName = clientName.substring(0, clientName.length() >= 40? 40: clientName.length());
			clientName = rightPadding(clientName, 40, " ");
			//	Records No
			String recordsNo = leftPadding("" + checks.size(), 6, "0");
			//	Debts and Credits
			String totalAmtAsString = String.format("%.2f", paySelection.getTotalAmt().abs()).replace(".", "").replace(",", "");
			if(totalAmtAsString.length() > 15) {
				addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ > @Valid@"));
			} else {
				totalAmtAsString = leftPadding(totalAmtAsString, 15, "0", true);
			}
			//	Debts Quantity
			String countDebts = leftPadding("" + checks.size(), 6, "0");
			//	Credits Quantity
			String countCredits = leftPadding("" + checks.size(), 6, "0");
			//	Constant
			String constant2 = leftPadding("", 76, "0");
			//	Write Totals
			StringBuffer footer = new StringBuffer();
			footer.append(Env.NL)				//	New Line
				.append(constant)				//  Sequence (Constant)
				.append(clientName)				//  Client Name
				.append(recordsNo)				//	Records No
				.append(totalAmtAsString)		//  Total Debts
				.append(totalAmtAsString)		//  Total Credits
				.append(countDebts)				//  Count Debts
				.append(countCredits)			//  Count Credit
				.append(constant2);				//  Sequence (Constant)
			writeLine(footer.toString());
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
	
	/**
	 * Get Payment Quantity
	 * @return
	 */
	private int getPaymentQty() {
		return paymentQty;
	}
	
	/**
	 * Add Payment to counter
	 * @param paymentQty
	 */
	private void addPaymentToCounter() {
		paymentQty++;
	}
}
