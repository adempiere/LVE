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
import java.math.BigDecimal;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MBank;
import org.compiere.model.MBankAccount;
import org.compiere.model.MClient;
import org.compiere.model.MCurrency;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPaySelection;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 * 	Implementation for Export Payment from Banesco bank
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/LVE/issues/1">
 * 		@see FR [ 1 ] Initial commit</a>
 */
public class Banesco extends LVEPaymentExportList {

	/** Logger								*/
	private static CLogger	s_log = CLogger.getCLogger (Banesco.class);
	/**	Header Format	*/
	private final String HEADER_DATE_FORMAT = "yyyyMMddHHmmss";
	/**	Header Short Format	*/
	private final String HEADER_SHORT_DATE_FORMAT = "yyyyMMdd";
	/**	Payment Quantity	*/
	private int paymentQty = 0;
	
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
			//	Format Date Header
			SimpleDateFormat headerFormat = new SimpleDateFormat(HEADER_DATE_FORMAT);
			//	Format Date
			SimpleDateFormat shortFormat = new SimpleDateFormat(HEADER_SHORT_DATE_FORMAT);
			//	Fields of Control Register (fixed data)
			String registerType = "HDR";
			String commercialAllocated = rightPadding("BANESCO", 15, " ");
			String standardEDIFACT = "E";
			String versionStandardEDIFACT ="D  95B";
			String documentType = "PAYMUL";
			String production = "P";
			BigDecimal totalPaymentAmount = getTotalPaymentAmount(checks);
			//	End Fields 
			// 	Control Register
			StringBuffer header = new StringBuffer();
			//	
			header.append(registerType)						//	Type Register
				.append(commercialAllocated)				//	Commercial Allocated
				.append(standardEDIFACT)					//	Standard EDIFACT
				.append(versionStandardEDIFACT)				//	Version Standard EDIFACT
				.append(documentType)						//	Document Type
				.append(production);						//	Production
			//	Write Line
			writeLine(header.toString());
			//	Set Value Type Register for Header Register
			//	Fields of Header Register 
			String transactionType = "SCV";
			//	Can be used for identify payments
			String descriptionCode = rightPadding("", 32, " ");
			String paymentRequestCondition = rightPadding("9", 3, " ");
			String paymentRequestNo = rightPadding(processValue(paySelection.getDocumentNo()), 35, " ", true);
			String paymentRequestDate = headerFormat.format(paySelection.getPayDate());
			//	Set Value Type Register for Control Register
			registerType = "01";
			header = new StringBuffer();
			//	Header
			header.append(CRLF)							//	New Line
				.append(registerType)						//  Type Register
				.append(transactionType)					//	Type Transaction
				.append(descriptionCode)					//  Description Code
				.append(paymentRequestCondition)			//  Payment Request Condition
				.append(paymentRequestNo)					//  Payment Request Number
				.append(paymentRequestDate);				//  Payment Request Date
			writeLine(header.toString());
			//  Write Debt Note
			header = new StringBuffer();
			//	Set Value Type Register for Debt Note Register
			registerType = "02";
			//	Fields of Debt Register
			String debtReferenceNo = processValue(paySelection.getDocumentNo());
			debtReferenceNo = debtReferenceNo.substring(0, debtReferenceNo.length() >= 8? 8: debtReferenceNo.length());
			debtReferenceNo = debtReferenceNo.replaceAll("\\D+","");
			debtReferenceNo = rightPadding(debtReferenceNo, 30, " ");
			//	Process Organization Tax ID
			String orgTaxId = processValue(orgInfo.getTaxID().replace("-", "")).trim();
			orgTaxId = rightPadding(orgTaxId, 17, " ").toUpperCase();
			String clientName = processValue(client.getName());
			clientName = rightPadding(clientName, 35, " ", true);
			//	Payment Amount
			String totalAmtAsString = String.format("%.2f", totalPaymentAmount).replace(".", "").replace(",", "");
			totalAmtAsString = leftPadding(totalAmtAsString, 15, "0");
			if(totalAmtAsString.length() <= 15) {
				totalAmtAsString = leftPadding(totalAmtAsString, 15, "0", true);
			} else {
				addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ > @Invalid@"));
			}
			String iSOCode = currency.getISO_Code();
			String freeField = rightPadding("", 1, " ");
			//	Account No
			String bankAccountNo = processValue(bankAccount.getAccountNo()).trim();
			bankAccountNo = bankAccountNo.replace(" ", "");
			bankAccountNo = rightPadding(bankAccountNo, 34, " ", true);
			String bankCodeOrder = rightPadding("BANESCO", 11, " ");
			String payDate = shortFormat.format(paySelection.getPayDate());
			//	Debt Note
			header.append(CRLF)											//	New Line
				.append(registerType)									//  Type Register
				.append(debtReferenceNo)								//	Reference Number
				.append(orgTaxId)										//  Organization Tax ID
				.append(clientName)										//  Client Name
				.append(totalAmtAsString)								//  Total Amount
				.append(iSOCode)										//  ISO Code Currency
				.append(freeField)										//  Free Field
				.append(bankAccountNo)									//  Bank Account Number
				.append(bankCodeOrder)									//  Bank Order Code
				.append(payDate);										//  Payment Date
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
							MUser bpContact = null;
							if(bpAccount.getAD_User_ID() != 0) {
								bpContact = MUser.get(Env.getCtx(), bpAccount.getAD_User_ID());
							}
							MBank bpBank = MBank.get(Env.getCtx(), bpAccount.getC_Bank_ID());
							//	Process Business Partner Account No
							String bPAccountNo = processValue(bpAccount.getAccountNo());
							String bPRoutingNo = rightPadding(processValue(bpBank.getRoutingNo()), 11, " ");
							if(Util.isEmpty(bPRoutingNo)) {
								addError(Msg.parseTranslation(Env.getCtx(), "@RoutingNo@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							String agencyCode = rightPadding("", 3, " ");
							String bPTaxId = bpAccount.getA_Ident_SSN();
							if(!Util.isEmpty(bPTaxId)){
								bPTaxId = bPTaxId.replace("-", "").trim();
								bPTaxId = rightPadding(bPTaxId, 17, " ", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@BPTaxID@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	Process Account Name
							String bPName = processValue(bpAccount.getA_Name());
							if(Optional.ofNullable(bPName).isPresent()) {
								bPName = rightPadding(bPName, 70, " ", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@A_Name@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	EMail
							String bPEmail = "";
							if(!Util.isEmpty(bpAccount.getA_EMail())) {
								bPEmail = bpAccount.getA_EMail();
							}
							bPEmail = rightPadding(bPEmail, 70, " ", true);
							//	Phone
							String bPPhone = "";
							String bPTaxIdContact = "";
							String bPContactName = "";
							if(Optional.ofNullable(bpContact).isPresent()) {
								if(!Util.isEmpty(bpContact.getPhone())) {
									bPPhone = processValue(bpContact.getPhone());
								}
								if(!Util.isEmpty(bpContact.getValue())) {
									bPTaxIdContact = processValue(bpContact.getValue());
								}
								if(!Util.isEmpty(bpContact.getName())) {
									bPContactName = processValue(bpContact.getName());
								}
							}
							bPPhone = rightPadding(bPPhone, 25, " ", true);
							//	Contact Tax Id
							bPTaxIdContact = rightPadding(processValue(bPTaxIdContact), 17, " ", true);
							//	Contact Name
							bPContactName = rightPadding(processValue(bPContactName), 35, " ", true);
							//	SettlorQualifier
							String settlorQualifier = rightPadding("", 1, " ", true);
							//	Employee
							String cardEmployee = rightPadding("", 30, " ", true);
							//	Payroll Type
							String payrollType = rightPadding("", 2, " ", true);
							//	Contact Location
							String contactLocation = rightPadding("", 21, " ", true);
							if(Optional.ofNullable(bPAccountNo).isPresent()) {
								bPAccountNo = leftPadding(bPAccountNo, 20, "0", true);
								bPAccountNo = rightPadding(bPAccountNo, 30, " ", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@AccountNo@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	Credit Register
							String lineRegisterType = "03";
							//	Process Document No
							String documentNo = processValue(paySelectionCheck.getDocumentNo());
							documentNo = documentNo.substring(0, documentNo.length() >= 8? 8: documentNo.length());
							documentNo = documentNo.replaceAll("\\D+","");
							documentNo = leftPadding(documentNo, 8, "0");
							documentNo = rightPadding(documentNo, 30, " ");
							//	Payment Amount
							String amountAsString = String.format("%.2f", paySelectionCheck.getPayAmt().abs()).replace(".", "").replace(",", "");
							if(amountAsString.length() <= 15) {
								amountAsString = leftPadding(amountAsString, 15, "0", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ > @Invalid@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							String paymentTerm = "425";
							if (!Util.isEmpty(bank.getSwiftCode())
									&& !Util.isEmpty(bpBank.getSwiftCode())
									&& bank.getSwiftCode().equals(bpBank.getSwiftCode())) {
								paymentTerm = rightPadding("42", 3, " ");
							}
							//	Write Credit Register
							StringBuffer line = new StringBuffer();
							line.append(CRLF)						//	New Line
								.append(lineRegisterType)			//	Type Register	
								.append(documentNo)					//	Document Number
								.append(amountAsString)				// 	Payment Amount
								.append(iSOCode)					//	ISO Code Currency
								.append(bPAccountNo)				//  BP Bank Account
								.append(bPRoutingNo)				// 	BP Bank Routing No
								.append(agencyCode)					// 	Agency Code
								.append(bPTaxId)					// 	BP TaxID
								.append(bPName)						//	BP Name
								.append(bPEmail)					//	BP Email
								.append(bPPhone)					//  BP Phone
								.append(bPTaxIdContact)				//  BP TaxID Contact
								.append(bPContactName)				// 	BP Name Contact
								.append(settlorQualifier)			//	Settlor Qualifier
								.append(cardEmployee)				// 	Card Employee
								.append(payrollType)				//	Type Payroll
								.append(contactLocation)			//	Location
								.append(paymentTerm); 				//	Payment Term
							
							s_log.fine("Write Line");
							writeLine(line.toString());
						} else {
							addError(Msg.parseTranslation(Env.getCtx(), "@C_BP_BankAccount_ID@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
						}
						//	
						addPaymentToCounter();
			});
			//	Totals Register
			//	Set Value Type Register for Totals Register
			registerType = "06";
			String countDebit = leftPadding("1", 15, "0");
			String countCredit = leftPadding("" + getPaymentQty(), 15, "0");
			//	Write Totals
			StringBuffer footer = new StringBuffer();
			footer.append(CRLF)						//	New Line
				.append(registerType)				//  Type Register
				.append(countDebit)					//	Count Debt
				.append(countCredit)				//  Count Credit
				.append(totalAmtAsString);			//  Total Amount
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
	
	@Override
	public String processValue(String value) {
		if(Util.isEmpty(value)) {
			return value;
		}
		//	
		return value.replaceAll("[+^:&áàäéèëíìïóòöúùñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ$,;*/-]", "");
	}
	
	@Override
	public int exportToFileAsEnrollment(MBankAccount bankAccount, List<MBPBankAccount> bPartnerAccountList,
			boolean isEnroll, File file, StringBuffer error) {
		MOrgInfo orgInfo = MOrgInfo.get(bankAccount.getCtx(), bankAccount.getAD_Org_ID(), bankAccount.get_TrxName());
		MClient client = MClient.get(orgInfo.getCtx(), orgInfo.getAD_Client_ID());
		MBank bank = MBank.get(bankAccount.getCtx(), bankAccount.getC_Bank_ID());
		//	Process Organization Tax ID
		String clientName = processValue(client.getName());
		clientName = rightPadding(clientName, 35, " ", true);
		String orgTaxId = processValue(orgInfo.getTaxID().replace("-", "")).trim();
		orgTaxId = rightPadding(orgTaxId, 17, " ").toUpperCase();
		SimpleDateFormat shortFormat = new SimpleDateFormat(HEADER_DATE_FORMAT);
		String documentNo = "AFIL" + shortFormat.format(new Time(System.currentTimeMillis()));
		documentNo = rightPadding(documentNo, 30, " ", true);
		// 	Control Register
		StringBuffer header = new StringBuffer();
		//	
		header.append(clientName)		//	Client Name
			.append(orgTaxId)			//	Tax ID
			.append(documentNo);		//	Document No
		//	Write Line
		writeLine(header.toString());
		//	Load Line
		s_log.fine("Iterate Payments");
		bPartnerAccountList.stream()
				.filter(bPartnerAccount -> bPartnerAccount != null)
				.forEach(bPartnerAccount -> {
					//  BPartner Info
					MBPartner bpartner = MBPartner.get(bPartnerAccount.getCtx(), bPartnerAccount.getC_BPartner_ID());
					MBank bpBank = MBank.get(Env.getCtx(), bPartnerAccount.getC_Bank_ID());
					boolean isSameBank = false;
					if (!Util.isEmpty(bank.getSwiftCode())
							&& !Util.isEmpty(bpBank.getSwiftCode())
							&& bank.getSwiftCode().equals(bpBank.getSwiftCode())) {
						isSameBank = true;
					}
					String transactionType = "";
					//	Validate Transaction
					if(isEnroll) {
						if(isSameBank) {
							transactionType = "051";
						} else {
							transactionType = "052";
						}
					} else {
						if(isSameBank) {
							transactionType = "071";
						} else {
							transactionType = "072";
						}
					}
					//	Reference No
					String referenceNo = rightPadding("", 30, " ", true);
					//	Person Type
					String bPTaxId = bPartnerAccount.getA_Ident_SSN();
					//	Process Person Type
					String personType = "";
					String firstChar = "";
					if(!Util.isEmpty(bPTaxId)){
						bPTaxId = bPTaxId.replace("-", "").trim();
						firstChar = bPTaxId.substring(0, 1);
						bPTaxId = bPTaxId.replaceAll("\\D+","");
						bPTaxId = leftPadding(bPTaxId, 11, "0", true);
						bPTaxId = firstChar + bPTaxId;
						bPTaxId = rightPadding(bPTaxId, 12, "0", true);
						if(isNumeric(firstChar)) {
							addError(Msg.parseTranslation(Env.getCtx(), "@PersonType@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()) + " - " + bPartnerAccount.getA_Ident_SSN());
						} else { 
							if(firstChar.equals("V")
									|| firstChar.equals("E")) {
								personType = "N";
							} else {
								personType = "J";
							}
						}
					} else {
						addError(Msg.parseTranslation(Env.getCtx(), "@BPTaxID@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
					}
					//	Account No
					String bPAccountNo = processValue(bPartnerAccount.getAccountNo());
					bPAccountNo = rightPadding(bPAccountNo, 20, "0", true);
					//	Process Account Name
					String bPName = processValue(bPartnerAccount.getA_Name());
					if(Optional.ofNullable(bPName).isPresent()) {
						bPName = rightPadding(bPName, 35, " ", true);
					} else {
						addError(Msg.parseTranslation(Env.getCtx(), "@A_Name@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
					}
					//	Status
					String transactionStatus = "000";					
					//	Write Credit Register
					StringBuffer line = new StringBuffer();
					line.append(CRLF)						//	New Line
						.append(transactionType)			//	Transaction Type
						.append(referenceNo)				//	Reference No
						.append(personType)					// 	Person Type
						.append(bPTaxId)					//	Fist char of Tax ID
						.append(bPAccountNo)				//  BP Bank Account
						.append(bPName)						//	BP Name
						.append(transactionStatus); 		//	Transaction Status
					s_log.fine("Write Line");
					writeLine(line.toString());
		});
		//	Exported payments
		return getExportedPayments();
	}
	
	/**
	 * Get total Payment Amount
	 * @param checks
	 * @return
	 */
	private BigDecimal getTotalPaymentAmount(List<MPaySelectionCheck> checks) {
		BigDecimal totalPaymentamount = Env.ZERO;
		for(MPaySelectionCheck check : checks) {
			totalPaymentamount = totalPaymentamount.add(check.getPayAmt().abs());
		}
		//	Return
		return totalPaymentamount;
	}
}
