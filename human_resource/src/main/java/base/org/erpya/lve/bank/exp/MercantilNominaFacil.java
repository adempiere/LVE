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
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MBank;
import org.compiere.model.MBankAccount;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPaySelection;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentBatch;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;
import org.compiere.util.Util;

/**
 * 	Implementation for Export Payment from Mercantil bank
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/LVE/issues/1">
 * 		@see FR [ 1 ] Initial commit</a>
 */
public class MercantilNominaFacil extends LVEPaymentExportList {

	/** Logger								*/
	static private CLogger	s_log = CLogger.getCLogger (MercantilNomina.class);
	/**	Header Short Format	*/
	private final String HEADER_SHORT_DATE_FORMAT = "yyyyMMdd";
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
			MBank bank = MBank.get(bankAccount.getCtx(), bankAccount.getC_Bank_ID());
			MOrg org = MOrg.get(paySelection.getCtx(), paySelection.getAD_Org_ID());
			MOrgInfo orgInfo = MOrgInfo.get(paySelection.getCtx(), paySelection.getAD_Org_ID(), paySelection.get_TrxName());
			// Bank Identification
			String bankSwift = "";
			//	Validate Swift
			if(!Util.isEmpty(bank.getSwiftCode())) {
				bankSwift = bank.getSwiftCode();
				bankSwift = rightPadding(bankSwift, 12, " ", true);
			} else {
				addError(Msg.parseTranslation(Env.getCtx(), "@C_Bank_ID@: " + bank.getName() + " @SwiftCode@ @NotFound@"));
			}
			//	Fields of Control Register (fixed data)
			String paymentRequestNo = processValue(paySelection.getDocumentNo());
			paymentRequestNo = leftPadding(paymentRequestNo, 15, "0", true);
			//	Identify
			String identifyRequestNo = processValue(paySelection.getDocumentNo());
			identifyRequestNo = leftPadding(identifyRequestNo, 8, "0", true);
			// Product Type
			String productType = "NOMIN";
			//	Payment Type
			String paymentTypeConstant = leftPadding("222", 10, "0");
			//	Process Organization Tax ID
			String orgTaxId = processValue(orgInfo.getTaxID().replace("-", ""));
			//	Process Person Type
			String organizationType = "";
			if(!Util.isEmpty(orgTaxId)){
				orgTaxId = orgTaxId.replace("-", "").trim();
				organizationType = orgTaxId.substring(0, 1).toUpperCase();
				orgTaxId = orgTaxId.replaceAll("\\D+", "");
				orgTaxId = leftPadding(orgTaxId, 15, "0", true);
			} else {
				addError(Msg.parseTranslation(Env.getCtx(), "@TaxID@ @NotFound@: " + org.getValue() + " - " + org.getName()));
			}
			//	Payments Generated
			String payGenerated = String.valueOf(checks.size());
			payGenerated = leftPadding(payGenerated, 8, "0", true);
			if(payGenerated.length() > 8) {
				addError(Msg.parseTranslation(Env.getCtx(), "@Qty@ > @InValid@"));
			}
			//	Payment Amount
			String totalAmtAsString = String.format("%.2f", paySelection.getTotalAmt().abs()).replace(".", "").replace(",", "");
			if(totalAmtAsString.length() > 17) {
				addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ > @InValid@"));
			}
			totalAmtAsString = leftPadding(totalAmtAsString, 17, "0", true);
			//	Format Date
			SimpleDateFormat shortFormat = new SimpleDateFormat(HEADER_SHORT_DATE_FORMAT);
			String payDate = shortFormat.format(paySelection.getPayDate());
			//	Account No
			String bankAccountNo = processValue(bankAccount.getAccountNo());
			bankAccountNo = bankAccountNo.replace(" ", "");
			bankAccountNo = rightPadding(bankAccountNo, 20, "0", true);
			//	
			StringBuffer header = new StringBuffer();
			//	Debt Note
			header.append("1")						//  Constant
				.append(bankSwift)					//	Swift
				.append(paymentRequestNo)			//	documentNo
				.append(productType)				//	Product Type
				.append(paymentTypeConstant)		//  Payment Type
				.append(organizationType)			//	Organization Type
				.append(orgTaxId)					//  Tax ID
				.append(payGenerated)				//	Payments Generated
				.append(totalAmtAsString)			//  Total Amount
				.append(payDate)					//	Payment Date
				.append(bankAccountNo)				//  Account No
				.append(leftPadding("", 7, "0"))	//  Reserved
				.append(identifyRequestNo)			//  Reserved Note Serial Number Company
				.append(leftPadding("", 4, "0")) 	//	Reserved Response Code (Data Output)
				.append(leftPadding("", 8, "0")) 	//	Reserved Date process (Data Output)
				.append(leftPadding("", 261, "0"))	// 	Reserved
				.append(CRLF);						//	New Line
			//	Open File
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
							String bPTaxId = bpAccount.getA_Ident_SSN();
							//	Process Person Type
							String personType = "";
							if(!Util.isEmpty(bPTaxId)){
								bPTaxId = bPTaxId.replace("-", "").trim();
								personType = bPTaxId.substring(0, 1).toUpperCase();
								bPTaxId = bPTaxId.replaceAll("\\D+","");
								bPTaxId = leftPadding(bPTaxId, 15, "0", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@BPTaxID@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	Payment Type
							MBank bpBank = MBank.get(Env.getCtx(), bpAccount.getC_Bank_ID());
							String paymentType = "1";
							if (!Util.isEmpty(bank.getSwiftCode())
									&& !Util.isEmpty(bpBank.getSwiftCode())
									&& !bank.getSwiftCode().equals(bpBank.getSwiftCode())) {
								paymentType = "3";
							}
							//	Account No
							//	Process Business Partner Account No
							String bPAccountNo = processValue(bpAccount.getAccountNo());
							if(Optional.ofNullable(bPAccountNo).isPresent()) {
								bPAccountNo = rightPadding(bPAccountNo, 20, "0", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@AccountNo@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	BP Value
							String bPValue = bpartner.getValue();
							bPValue = bPValue.replace("-", "").trim();
							bPValue = rightPadding(bPValue, 16, " ", true);
							//	Process Account Name
							String bPName = processValue(bpAccount.getA_Name());
							if(Optional.ofNullable(bPName).isPresent()) {
								bPName = rightPadding(bPName, 60, " ", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@A_Name@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							
							//	Process Document No
							String paymentDescription = processValue(payselectionCheck.getDocumentNo());
							if(!Util.isEmpty(paySelection.getName())) {
								paymentDescription = paymentDescription + " " + processValue(paySelection.getName());
							}
							if(!Util.isEmpty(paySelection.getDescription())) {
								paymentDescription = paymentDescription + " " + processValue(paySelection.getDescription());
							}
							//	
							paymentDescription = rightPadding(paymentDescription, 80, " ", true);
							//	Payment Amount
							String amountAsString = String.format("%.2f", payselectionCheck.getPayAmt().abs()).replace(".", "").replace(",", "");
							if(amountAsString.length() > 17) {
								addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ > @Valid@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							amountAsString = leftPadding(amountAsString, 17, "0", true);
							//	EMail
							String bPEmail = "";
							if(!Util.isEmpty(bpAccount.getA_EMail())) {
								bPEmail = bpAccount.getA_EMail();
							}
							bPEmail = rightPadding(bPEmail, 50, " ", true);
							//	Write Credit Register
							StringBuffer line = new StringBuffer();
							line.append("2")						//	Constant
								.append(personType)					//	Type Register	
								.append(bPTaxId)					// 	BP TaxID
								.append(paymentType)				//	Payment Type (Same Bank / Other Bank)
								.append(leftPadding("", 12, "0"))	//	Reserved
								.append(leftPadding("", 30, " "))	//	Reserved
								.append(bPAccountNo)				//  BP Bank Account
								.append(amountAsString)				//	Payment Amount
								.append(bPValue)					//	BP Value
								.append(paymentTypeConstant)		//	Payment Type
								.append(leftPadding("", 3, "0"))	//  Reserved
								.append(bPName)						//	BP Name
								.append(leftPadding("", 15, "0"))	//  Reserved
								.append(bPEmail) 					//	Email
								.append(leftPadding("", 4, "0")) 	//	Response Code
								.append(leftPadding("", 30, " "))	//  Response Message
								.append(paymentDescription)			//	Payment Concept
								.append(leftPadding("", 35, "0")) 	//	Reserved
								.append(CRLF);						//	New Line
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
	public int exportToFileAsVerification(MBankAccount bankAccount, List<MPayment> payments, File file, StringBuffer error) {
		if (payments == null || payments.size() == 0)
			return 0;
		//	
		try {
			s_log.fine("Delete file if exist");
			//	Open File
			openFileWriter(file, bankAccount, payments, "CS");
			//	Write header
			MPayment firstPayment = (MPayment) payments.get(0);
			MOrg org = MOrg.get(bankAccount.getCtx(), bankAccount.getAD_Org_ID());
			MOrgInfo orgInfo = MOrgInfo.get(bankAccount.getCtx(), bankAccount.getAD_Org_ID(), bankAccount.get_TrxName());
			MBank bank = MBank.get(bankAccount.getCtx(), bankAccount.getC_Bank_ID());
			//	Constant 
			String constant = "1";
			// Batch Document No
			MPaymentBatch paymentBatch = (MPaymentBatch) firstPayment.getC_PaymentBatch();
			//	Validate
			if(paymentBatch == null) {
				addError(Msg.parseTranslation(Env.getCtx(), "@C_PaymentBatch_ID@ @NotFound@"));
			}
			String batchDocumentNo = paymentBatch.getDocumentNo();
			batchDocumentNo = batchDocumentNo.replaceAll("\\D+", "");
			batchDocumentNo = leftPadding(batchDocumentNo, 15, "0", true);
			//	Date Format
			SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
			//	Time Format
			SimpleDateFormat timeFormat = new SimpleDateFormat("hhmmss");
			//	Current Time
			Timestamp currentDate = new Timestamp(System.currentTimeMillis());
			//	Process Date
			String processDate = dateFormat.format(currentDate);
			//	ProcessTime
			String processTime = timeFormat.format(currentDate);
			//	Process Organization Tax ID
			String organizationType = "";
			String orgTaxId = processValue(orgInfo.getTaxID().replace("-", ""));
			//	Process Person Type
			if(!Util.isEmpty(orgTaxId)){
				organizationType = orgTaxId.substring(0, 1);
				orgTaxId = orgTaxId.replaceAll("\\D+", "");
				orgTaxId = leftPadding(orgTaxId, 15, "0", true);
			} else {
				addError(Msg.parseTranslation(Env.getCtx(), "@TaxID@ @NotFound@: " + org.getValue() + " - " + org.getName()));
			}
			//	Payments Generated
			String paymentsGenerated = leftPadding(String.valueOf(payments.size()), 7, "0", true);
			//	Iterate over payments
			BigDecimal totalAmount = getTotalAmount(payments);
			//	Payment Amount
			String totalAsString = String.format("%.2f", totalAmount.abs()).replace(".", "").replace(",", "");
			if(totalAsString.length() > 15) {
				addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ @Invalid@"));
			} else {
				totalAsString = leftPadding(totalAsString, 15, "0", true);
			}
			//	Constant
			String constant2 = leftPadding("", 142, " ");
			//	Bank Account No
			String bankAccountNo = processValue(bankAccount.getAccountNo());
			bankAccountNo = bankAccountNo.replace(" ", "");
			bankAccountNo = rightPadding(bankAccountNo, 20, "0", true);
			//	Days Due
			int paymentDaysDue = bank.get_ValueAsInt("PaymentDaysDue");
			//	End Fields 
			// 	Control Register
			StringBuffer header = new StringBuffer();
			//	
			header.append(constant)
					.append(batchDocumentNo)		//  Customer File Number or Lot Number				
					.append(processDate)			//	Process date
					.append(processTime) 			// 	Process Time
					.append(organizationType) 		// 	Type of Person
					.append(orgTaxId)				//  Customer Company Identification
					.append(paymentsGenerated)		//	Payments Generated
					.append(totalAsString)			//	Amount of Checks
					.append(constant2);				// 	Reserved Area
			//	Write Line
			writeLine(header.toString());
			//  Write Credit Note
			s_log.fine("Iterate Payments");
			for(MPayment payment : payments) {
				MBPBankAccount bpAccount = getBPAccountInfo(payment, true);
				MBPartner bpartner = MBPartner.get(payment.getCtx(), payment.getC_BPartner_ID());
				//	Constant
				constant = "2";
				//	Process Document No
				String checkNo = processValue(payment.getCheckNo());
				checkNo = checkNo.replaceAll("\\D+","");
				checkNo = leftPadding(checkNo, 11, "0", true);
				//	Process Account Name
				String bPName = processValue(bpAccount.getA_Name());
				if(Optional.ofNullable(bPName).isPresent()) {
					bPName = rightPadding(bPName, 120, " ", true);
				} else {
					addError(Msg.parseTranslation(Env.getCtx(), "@A_Name@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
				}
				//	Is a Valid Status
				int validStatus = 0;
				BigDecimal paymentAmount = payment.getPayAmt();
				if(!payment.getDocStatus().equals(MPayment.DOCSTATUS_Completed)
						&& !payment.getDocStatus().equals(MPayment.DOCSTATUS_Closed)) {
					validStatus = 1;
					paymentAmount = Env.ZERO;
				}
				//	Payment Amount
				String amountAsString = String.format("%.2f", paymentAmount.abs()).replace(".", "").replace(",", "");
				if(amountAsString.length() > 15) {
					addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ @Invalid@: " + bpartner.getValue() + " - " + bpartner.getName()));
				} else {
					amountAsString = leftPadding(amountAsString, 15, "0", true);
				}
				//	Payment Date
				String paymentDate = dateFormat.format(payment.getDateTrx());
				//	Payment Due Date
				String paymentDueDate = dateFormat.format(TimeUtil.addDays(payment.getDateTrx(), paymentDaysDue));
				
				String bPTaxId = bpAccount.getA_Ident_SSN();
				if(!Util.isEmpty(bPTaxId)) {
					bPTaxId = bPTaxId.replace("-", "").trim();
					String bpTaxIdChar = bPTaxId.substring(0, 1);
					bPTaxId = bPTaxId.substring(1, bPTaxId.length());
					bPTaxId = leftPadding(bPTaxId, 9, "0", true);
					bPTaxId = bpTaxIdChar + bPTaxId;
				} else {
					addError(Msg.parseTranslation(Env.getCtx(), "@BPTaxID@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
				}
				//	Change constant 2
				constant2 = leftPadding("", 26, " ");
				//	Write Credit Register
				StringBuffer line = new StringBuffer();
				line.append(CRLF)				//	New Line
					.append(constant)			//	Constant
					.append(bankAccountNo)		//	Bank Account
					.append(checkNo)			//	Check No
					.append(bPName)				//	BP Name
					.append(amountAsString)		// 	Payment Amount
					.append(paymentDate)		// 	Payment Date
					.append(paymentDueDate)		// 	Payment Due Date
					.append(validStatus)		//  Valid status
					.append(constant2);			//  Constant 2
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
	
	/**
	 * Open File from Payment Selection
	 * @param file
	 * @param checks
	 */
	public void openFileWriter(File file, List<MPaySelectionCheck> checks) {
		MPaySelectionCheck check = checks.get(0);
		MPaySelection paymentSelection = check.getParent();
		String fileName = getFileName(file, "", paymentSelection.getDocumentNo());
		openFileWriter(fileName.replaceAll("_", ""));
	}
}
