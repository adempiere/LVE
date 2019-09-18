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
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MBank;
import org.compiere.model.MBankAccount;
import org.compiere.model.MClient;
import org.compiere.model.MCurrency;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPaySelection;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.model.MPayment;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.erpya.lve.util.ColumnsAdded;

/**
 * 	Implementation for Export Payment from Venezuela bank
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/LVE/issues/1">
 * 		@see FR [ 1 ] Initial commit</a>
 */
public class Venezuela extends LVEPaymentExportList {

	/** Logger								*/
	static private CLogger	s_log = CLogger.getCLogger (Venezuela.class);
	/**	Header Short Format	*/
	private final String SHORT_DATE_FORMAT = "dd/MM/yyyy";
	public final static char CR  = (char) 0x0D;
	public final static char LF  = (char) 0x0A;
	public final static String CRLF  = "" + CR + LF; 
	
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
			MCurrency currency = MCurrency.get(Env.getCtx(), bankAccount.getC_Currency_ID());
			MOrgInfo orgInfo = MOrgInfo.get(paySelection.getCtx(), paySelection.getAD_Org_ID(), paySelection.get_TrxName());
			MClient client = MClient.get(orgInfo.getCtx(), orgInfo.getAD_Client_ID());
			MBank bank = MBank.get(bankAccount.getCtx(), bankAccount.getC_Bank_ID());
			//	Time Format
			SimpleDateFormat dateFormat = new SimpleDateFormat(SHORT_DATE_FORMAT);
			//	Now
			Date now = new Date(System.currentTimeMillis());
			//	Fields of Control Register (fixed data)
			String sequence = rightPadding("HEADER", 8, " ");
			//	Bank Client No
			String bankClientNo = "";
			if(!Util.isEmpty(bank.get_ValueAsString("BankClientNo"))) {
				bankClientNo = processValue(bank.get_ValueAsString(ColumnsAdded.COLUMNNAME_BankClientNo));
				bankClientNo = leftPadding(bankClientNo, 8, "0", true);
			} else {
				addError(Msg.parseTranslation(Env.getCtx(), "@BankClientNo@ @NotFound@"));
			}
			//	
			String paymentRequestNo = leftPadding(processValue(paySelection.getDocumentNo()), 8, "0", true);
			//	Process Person Type
			String orgPersonType = "";
			String orgTaxId = processValue(orgInfo.getTaxID().replace("-", "")).trim();
			if(!Util.isEmpty(orgTaxId)){
				orgPersonType = orgTaxId.substring(0, 1);
				orgTaxId = orgTaxId.replaceAll("\\D+","");
				orgTaxId = leftPadding(orgTaxId, 9, "0", true);
			} else {
				addError(Msg.parseTranslation(Env.getCtx(), "@TaxID@ @NotFound@: " + client.getName()));
			}
			//	Client Name
			String orgName = processValue(client.getName());
			orgName = rightPadding(orgName, 35, " ", true);
			//	Payment Date
			String paymentDate = dateFormat.format(paySelection.getPayDate());
			//	Now
			String nowAsString = dateFormat.format(now);
			//	Account No
			String bankAccountNo = processValue(bankAccount.getAccountNo()).trim();
			bankAccountNo = rightPadding(bankAccountNo, 20, "0", true);
			// 	Control Register
			StringBuffer header = new StringBuffer();
			//	
			header.append(sequence)			//	Sequence (Constant)
				.append(paymentRequestNo)	//	Payment Request No
				.append(bankClientNo)		//	Bank Client No
				.append(orgPersonType)		//	Person Type
				.append(orgTaxId)			//	Tax ID
				.append(paymentDate)		//	Payment Date
				.append(nowAsString);		//	Current Date
			//	Write Line
			writeLine(header.toString());
			//  Write Credit Note
			s_log.fine("Iterate Payments");
			for(MPaySelectionCheck paySelectionCheck : checks) {
				//  BPartner Info
				MBPartner bpartner = MBPartner.get(paySelectionCheck.getCtx(), paySelectionCheck.getC_BPartner_ID());
				MBPBankAccount bpAccount = getBPAccountInfo(paySelectionCheck, true);
				if(bpAccount != null) {
					//	Write Credit Register
					StringBuffer line = new StringBuffer();
					
					//	Constant space
					String constant = rightPadding("DEBITO", 8, " ");
					//	Fields of Debt Register
					String documentNo = processValue(paySelectionCheck.getDocumentNo());
					documentNo = leftPadding(documentNo, 8, "0", true);
					//	Constant space
					String constant2 = leftPadding("", 2, "0");
					//	Payment Amount
					String amountAsString = String.format("%.2f", paySelectionCheck.getPayAmt().abs()).replace(".", ",");
					if(amountAsString.length() > 18) {
						addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ @Invalid@: " + bpartner.getValue() + " - " + bpartner.getName()));
					} else {
						amountAsString = leftPadding(amountAsString, 18, "0", true);
					}
					//	ISO Code
					String iSOCode = currency.getISO_Code();
					//	Constant
					String constant3 = rightPadding("40", 3, "");
					//	Constant
					String constant4 = rightPadding("CREDITO", 8, " ");
					//	Process Person Type
					String bPPersonType = "";
					String bPTaxId = bpAccount.getA_Ident_SSN();
					if(!Util.isEmpty(bPTaxId)){
						bPTaxId = bPTaxId.replace("-", "").trim();
						bPPersonType = bPTaxId.substring(0, 1);
						bPTaxId = bPTaxId.replaceAll("\\D+","");
						bPTaxId = leftPadding(bPTaxId, 9, "0", true);
					} else {
						addError(Msg.parseTranslation(Env.getCtx(), "@BPTaxID@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
					}
					//	Process Account Name
					String bPName = processValue(bpAccount.getA_Name());
					if(Optional.ofNullable(bPName).isPresent()) {
						bPName = rightPadding(bPName, 30, " ", true);
					} else {
						addError(Msg.parseTranslation(Env.getCtx(), "@A_Name@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
					}
					//	Constant
					String accountType = leftPadding("", 2, "0");
					//	Process Business Partner Account No
					String bPAccountNo = processValue(bpAccount.getAccountNo());
					if(Optional.ofNullable(bPAccountNo).isPresent()) {
						bPAccountNo = rightPadding(bPAccountNo, 20, " ", true);
					} else {
						addError(Msg.parseTranslation(Env.getCtx(), "@AccountNo@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
					}
					//	Payment Type
					MBank bpBank = MBank.get(Env.getCtx(), bpAccount.getC_Bank_ID());
					String paymentType = "10";
					if (!Util.isEmpty(bank.getSwiftCode())
							&& !Util.isEmpty(bpBank.getSwiftCode())
							&& !bank.getSwiftCode().equals(bpBank.getSwiftCode())) {
						paymentType = "00";
					}
					//	Swift Code
					String bPSwiftCode = bpBank.getSwiftCode();
					bPSwiftCode = rightPadding(bPSwiftCode, 10, " ", true);
					String constantSpace = rightPadding("", 59, " ");
					//	
					line.append(CRLF)			//	New Line
						.append(constant)		//	Sequence (Constant)
						.append(documentNo)		//	Document No
						.append(orgPersonType)	//  Person Type
						.append(orgTaxId)		// 	Tax ID
						.append(orgName)		// 	Client Name
						.append(paymentDate)	//	Payment Date
						.append(constant2)		// 	Account Type
						.append(bankAccountNo)	//	Bank Account No
						.append(amountAsString)	// 	Amount
						.append(iSOCode)		//	ISO Code Currency
						.append(constant3)		//	Sequence (Constant)
						.append(CRLF)			//	New Line
						.append(constant4)		//	Sequence (Constant)
						.append(documentNo)		//	Document No
						.append(bPPersonType)	//	BP PersonType
						.append(bPTaxId)		//	BP TaxID
						.append(bPName)			//  BP Name
						.append(accountType)	//  Account Type
						.append(bPAccountNo)	//	BP Account
						.append(amountAsString)	// 	Amount
						.append(paymentType)	//	Payment Type
						.append(bPSwiftCode)	// 	BP Swift Code
						.append(constantSpace);	//	Fixed Lenght
					s_log.fine("Write Line");
					writeLine(line.toString());
				} else {
					addError(Msg.parseTranslation(Env.getCtx(), "@C_BP_BankAccount_ID@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
				}
			}
			//	Totals Register
			//	Constant
			String constant = rightPadding("TOTAL", 8, " ");
			//	Constant
			String constant2 = leftPadding("1", 5, "0");
			//	Constant
			String constant3 = leftPadding("1", 5, "0");
			//	Payment Amount
			String totalAsString = String.format("%.2f", paySelection.getTotalAmt().abs()).replace(".", ",");
			if(totalAsString.length() > 18) {
				addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ @Invalid@"));
			} else {
				totalAsString = leftPadding(totalAsString, 18, "0", true);
			}
			//	Write Totals
			StringBuffer footer = new StringBuffer();
			footer.append(CRLF)			//	New Line
				.append(constant)			//  Sequence (Constant)
				.append(constant2)			//  Sequence (Constant)
				.append(constant3)			//	Sequence (Constant)
				.append(totalAsString);		//  Total Debts
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
	
	@Override
	public int exportToFileAsVerification(MBankAccount bankAccount, List<MPayment> payments, File file, StringBuffer error) {
		if (payments == null || payments.size() == 0)
			return 0;
		//	
		try {
			s_log.fine("Delete file if exist");
			//	Open File
			openFileWriter(file, bankAccount, payments, "CS");
			//	Write header
			MOrg org = MOrg.get(bankAccount.getCtx(), bankAccount.getAD_Org_ID());
			MOrgInfo orgInfo = MOrgInfo.get(bankAccount.getCtx(), bankAccount.getAD_Org_ID(), bankAccount.get_TrxName());
			//	Constant 
			String constant = "0";
			//	Process Organization Tax ID
			String organizationType = "";
			String orgTaxId = processValue(orgInfo.getTaxID().replace("-", "")).trim();
			//	Process Person Type
			if(!Util.isEmpty(orgTaxId)){
				organizationType = orgTaxId.substring(0, 1);
				orgTaxId = orgTaxId.replaceAll("\\D+", "");
				orgTaxId = leftPadding(orgTaxId, 9, "0", true);
			} else {
				addError(Msg.parseTranslation(Env.getCtx(), "@TaxID@ @NotFound@: " + org.getValue() + " - " + org.getName()));
			}
			//	Payments Generated
			String paymentsGenerated = leftPadding(String.valueOf(payments.size()), 5, "0", true);
			//	Bank Account No
			String bankAccountNo = processValue(bankAccount.getAccountNo()).trim();
			bankAccountNo = bankAccountNo.replace(" ", "");
			bankAccountNo = rightPadding(bankAccountNo, 20, "0", true);
			//	Iterate over payments
			BigDecimal totalAmount = getTotalAmount(payments);
			//	Payment Amount
			String totalAsString = String.format("%.2f", totalAmount.abs()).replace(".", "").replace(",", "");
			if(totalAsString.length() > 17) {
				addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ @Invalid@"));
			} else {
				totalAsString = leftPadding(totalAsString, 17, "0", true);
			}
			//	Date Format
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
			//	End Fields 
			// 	Control Register
			StringBuffer header = new StringBuffer();
			//	
			header.append(constant)
					.append(organizationType) 		// 	Type of Person
					.append(orgTaxId)				//  Customer Company Identification
					.append(paymentsGenerated)		//	Payments Generated
					.append(bankAccountNo)			//	Bank Account No
					.append(totalAsString);			//	Amount of Checks
			//	Write Line
			writeLine(header.toString());
			//  Write Credit Note
			s_log.fine("Iterate Payments");
			for(MPayment payment : payments) {
				MBPBankAccount bpAccount = getBPAccountInfo(payment, true);
				MBPartner bpartner = MBPartner.get(payment.getCtx(), payment.getC_BPartner_ID());
				//	Constant
				constant = "1";
				//	Process Account Name
				String bPName = processValue(bpAccount.getA_Name());
				if(Optional.ofNullable(bPName).isPresent()) {
					bPName = rightPadding(bPName, 60, " ", true);
				} else {
					addError(Msg.parseTranslation(Env.getCtx(), "@A_Name@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
				}
				//	Process Document No
				String checkNo = processValue(payment.getCheckNo());
				checkNo = checkNo.replaceAll("\\D+","");
				checkNo = leftPadding(checkNo, 8, "0", true);
				//	Payment Amount
				String amountAsString = String.format("%.2f", payment.getPayAmt().abs()).replace(".", "").replace(",", "");
				if(amountAsString.length() > 17) {
					addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ @Invalid@: " + bpartner.getValue() + " - " + bpartner.getName()));
				} else {
					amountAsString = leftPadding(amountAsString, 17, "0", true);
				}
				//	Payment Date
				String paymentDate = dateFormat.format(payment.getDateTrx());
				//	Change constant 2
				String constant2 = "S";
				String constant3 = leftPadding("", 80, " ");
				//	Write Credit Register
				StringBuffer line = new StringBuffer();
				line.append(CRLF)				//	New Line
					.append(constant)			//	Constant
					.append(bankAccountNo)		//	Bank Account
					.append(bPName)				//	BP Name
					.append(checkNo)			//	Check No
					.append(amountAsString)		// 	Payment Amount
					.append(paymentDate)		// 	Payment Date
					.append(constant2)			//  Constant 2
					.append(constant3);			//	Constant 3
				//	Write it
				s_log.fine("Write Line");
				writeLine(line.toString());
			}
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
		//	
		return getExportedPayments();
	}
	
	/**
	 * Get Total Amount of Payments
	 * @param payments
	 * @return
	 */
	private BigDecimal getTotalAmount(List<MPayment> payments) {
		BigDecimal totalAmount = Env.ZERO;
		for(MPayment payment : payments) {
			totalAmount = totalAmount.add(payment.getPayAmt());
		}
		//	Default return
		return totalAmount;
	}
}
