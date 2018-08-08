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
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MBank;
import org.compiere.model.MBankAccount;
import org.compiere.model.MCurrency;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPaySelection;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.eevolution.model.MHRMovement;
import org.eevolution.model.MHRProcess;

/**
 * 	Implementation for Export Payment from BOD bank
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/LVE/issues/1">
 * 		@see FR [ 1 ] Initial commit</a>
 */
public class BOD extends LVEPaymentExportList {

	/** Logger								*/
	private static CLogger	s_log = CLogger.getCLogger (BOD.class);
	/**	Header Short Format	*/
	private final String DATE_FORMAT = "yyyyMMdd";
	/**	Reference Format	*/
	private final String REFERENCE_DATE_FORMAT = "yyyy/MM/dd";
	
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
			MBank bank = MBank.get(bankAccount.getCtx(), bankAccount.getC_Bank_ID());
			//	Format Date
			SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			// 	Control Register
			StringBuffer header = new StringBuffer();
			//	Set Value Type Register for Control Register
			String registerType = "01";
			//	Fields of Header Register 
			String transactionType = rightPadding("PROVEEDORES", 20, " ");
			//	Process Person Type
			String personType = "";
			String orgTaxId = processValue(orgInfo.getTaxID().replace("-", "")).trim();
			if(!Util.isEmpty(orgTaxId)){
				orgTaxId = orgTaxId.replace("-", "").trim();
				personType = orgTaxId.substring(0, 1);
				orgTaxId = orgTaxId.replaceAll("\\D+","");
				orgTaxId = leftPadding(orgTaxId, 9, "0", true);
			} else {
				addError(Msg.parseTranslation(Env.getCtx(), "@TaxID@ @NotFound@"));
			}
			//	Bank Client No
			String bankClientNo = "";
			if(!Util.isEmpty(bank.get_ValueAsString("BankClientNo"))) {
				bankClientNo = processValue(bank.get_ValueAsString("BankClientNo"));
				bankClientNo = leftPadding(bankClientNo, 17, "0", true);
			} else {
				addError(Msg.parseTranslation(Env.getCtx(), "@BankClientNo@ @NotFound@"));
			}
			//	Fields of Debt Register
			String paymentRequestNo = processValue(paySelection.getDocumentNo());
			paymentRequestNo = rightPadding(paymentRequestNo, 9, " ", true);
			//	Document Date
			String paymentRequestDate = dateFormat.format(paySelection.getPayDate());
			//	Payment Quantity
			String paymentQty = leftPadding(String.valueOf(checks.size()), 6, "0");
			//	Payment Amount
			String totalAmtAsString = String.format("%.2f", paySelection.getTotalAmt().abs()).replace(".", "").replace(",", "");
			if(totalAmtAsString.length() > 19) {
				addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ @Invalid@"));
			}
			totalAmtAsString = leftPadding(totalAmtAsString, 19, "0", true);
			//	ISO Code for Currency
			String iSOCode = currency.getISO_Code();
			//	Constant
			String constant = leftPadding("", 158, " ");
			header = new StringBuffer();
			//	Header
			header.append(registerType)			//  Type Register
				.append(transactionType)		//	Type Transaction
				.append(personType)				//	Person Type
				.append(orgTaxId)				//  Organization Tax ID
				.append(bankClientNo)			//	Bank Client No
				.append(paymentRequestNo)		//  Payment Request No
				.append(paymentRequestDate)		//  Payment Request Date
				.append(paymentQty)				//  Payment Quantity
				.append(totalAmtAsString)		//	Total Amount
				.append(iSOCode)				//	Currency ISO Code
				.append(constant);				//  Constant
			writeLine(header.toString());
			String payDate = dateFormat.format(paySelection.getPayDate());
			//  Write Credit Note
			s_log.fine("Iterate Payments");
			checks.stream()
					.filter(paySelectionCheck -> paySelectionCheck != null)
					.forEach(payselectionCheck -> {
						//  BPartner Info
						MBPartner bpartner = MBPartner.get(payselectionCheck.getCtx(), payselectionCheck.getC_BPartner_ID());
						MBPBankAccount bpAccount = getBPAccountInfo(payselectionCheck, true);
						if(bpAccount != null) {
							MUser bpContact = null;
							if(bpAccount.getAD_User_ID() != 0) {
								bpContact = MUser.get(Env.getCtx(), bpAccount.getAD_User_ID());
							}
							MBank bpBank = MBank.get(Env.getCtx(), bpAccount.getC_Bank_ID());
							//	Line Register Type
							String lineRegisterType = "02";
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
								bPName = rightPadding(bPName, 60, " ", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@A_Name@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	Process Document No
							String documentNo = processValue(payselectionCheck.getDocumentNo());
							documentNo = rightPadding(documentNo, 9, " ", true);
							//	Description (Can be filled with document reference)
							String lineDescription = rightPadding("", 30, " ", true);
							//	Payment Type
							String paymentType = "CTA";
							if (!Util.isEmpty(bank.getSwiftCode())
									&& !Util.isEmpty(bpBank.getSwiftCode())
									&& !bank.getSwiftCode().equals(bpBank.getSwiftCode())) {
								paymentType = "BAN";
							}
							//	BP Account No
							String bPAccountNo = processValue(bpAccount.getAccountNo());
							if(Optional.ofNullable(bPAccountNo).isPresent()) {
								bPAccountNo = leftPadding(bPAccountNo, 20, "0", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@AccountNo@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	Routing No
							String bPRoutingNo = "";
							if(Util.isEmpty(bpBank.getRoutingNo())) {
								addError(Msg.parseTranslation(Env.getCtx(), "@RoutingNo@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							bPRoutingNo = processValue(bpBank.getRoutingNo());
							bPRoutingNo = leftPadding(bPRoutingNo, 4, "0", true);
							//	Payment Amount
							String amountAsString = String.format("%.2f", payselectionCheck.getPayAmt().abs()).replace(".", "").replace(",", "");
							if(amountAsString.length() > 17) {
								addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ @Invalid@"));
							}
							amountAsString = amountAsString.substring(0, amountAsString.length() >= 17? 17: amountAsString.length());
							amountAsString = leftPadding(amountAsString, 17, "0");
							//	Withholding Tax
							String withholdingTaxAsString = String.format("%.2f", Env.ZERO).replace(".", "").replace(",", "");
							withholdingTaxAsString = leftPadding(withholdingTaxAsString, 17, "0", true);
							//	EMail
							String bPEmail = "";
							if(!Util.isEmpty(bpAccount.getA_EMail())) {
								bPEmail = bpAccount.getA_EMail();
							}
							bPEmail = rightPadding(bPEmail, 40, " ", true);
							//	Phone
							String bPPhone = "";
							if(Optional.ofNullable(bpContact).isPresent()) {
								if(!Util.isEmpty(bpContact.getPhone())) {
									bPPhone = processValue(bpContact.getPhone());
								}
							}
							bPPhone = rightPadding(bPPhone, 11, " ", true);
							//	Constant
							String constantLine = rightPadding("", 20, " ", true);
							//	Write Credit Register
							StringBuffer line = new StringBuffer();
							line.append(Env.NL)					//	New Line
								.append(lineRegisterType)		//	Type Register
								.append(bPPersonType)			//	Person Type
								.append(bPTaxId)				//  BP TaxID
								.append(bPName)					//	BP Name
								.append(documentNo)				//	Document Number
								.append(lineDescription)		//	Line Description
								.append(paymentType)			//	Payment Type
								.append(bPAccountNo)			//  BP Bank Account
								.append(bPRoutingNo)			// 	BP Bank Routing No
								.append(payDate)				//	Payment Date
								.append(amountAsString)			// 	Payment Amount
								.append(iSOCode)				//	ISO Code Currency
								.append(withholdingTaxAsString)	//	Withholding Tax
								.append(bPEmail)				//	BP Email
								.append(bPPhone)				//  BP Phone
								.append(constantLine); 			//	Constant
							s_log.fine("Write Line");
							writeLine(line.toString());
							//	Write detail of payment
							writeDetail(payselectionCheck);
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
	
	/**
	 * Write Detail of payment Selection
	 */
	private void writeDetail(MPaySelectionCheck check) {
		check.getPaySelectionLinesAsList(false).stream()
			.forEach(paySelectionLine -> {
				String registerType = "03";
				//	Process Document 
				String documentNo = null;
				BigDecimal documentAmount = Env.ZERO;
				String iSOCode = null;
				Timestamp documentDate = null;
				BigDecimal withholdingTax = Env.ZERO;
				//	
				MPaySelection paymentSelection = (MPaySelection) paySelectionLine.getC_PaySelection();
				MCurrency defaultCurrency = null;
				if(paymentSelection.getC_Currency_ID() != 0) {
					defaultCurrency = MCurrency.get(Env.getCtx(), paymentSelection.getC_Currency_ID());
				}
				//	Validate for fill
				if(paySelectionLine.getC_Invoice_ID() != 0) {
					MInvoice invoice = (MInvoice) paySelectionLine.getC_Invoice();
					documentNo = invoice.getDocumentNo();
					documentAmount = invoice.getGrandTotal();
					if(invoice.getC_Currency_ID() != 0) {
						MCurrency currency = MCurrency.get(Env.getCtx(), invoice.getC_Currency_ID());
						iSOCode = currency.getISO_Code();
					}
					documentDate = invoice.getDateInvoiced();
				} else if(paySelectionLine.getC_Order_ID() != 0) {
					MOrder order = (MOrder) paySelectionLine.getC_Order();
					documentNo = order.getDocumentNo();
					documentAmount = order.getGrandTotal();
					if(order.getC_Currency_ID() != 0) {
						MCurrency currency = MCurrency.get(Env.getCtx(), order.getC_Currency_ID());
						iSOCode = currency.getISO_Code();
					}
					documentDate = order.getDateOrdered();
				} else if(paySelectionLine.getHR_Movement_ID() != 0) {
					MHRMovement movement = (MHRMovement) paySelectionLine.getHR_Movement();
					MHRProcess payrollProcess = (MHRProcess) movement.getHR_Process();
					documentNo = payrollProcess.getDocumentNo();
					documentAmount = movement.getAmount();
					documentDate = payrollProcess.getDateAcct();
				} else {
					documentNo = "SP-" +  paymentSelection.getDocumentInfo();
					documentAmount = check.getPayAmt();
					documentDate = paymentSelection.getDateDoc();
				}
				//	Get Default ISO Code
				if(Util.isEmpty(iSOCode)) {
					if(defaultCurrency== null) {
						addError("@C_Currency_ID@ @NotFound@: " + documentNo);
					} else {
						iSOCode = defaultCurrency.getISO_Code();
					}
				}
				//	Document No
				documentNo = rightPadding(documentNo, 20, " ");
				//	Reference Amount
				String documentAmountAsString = String.format("%.2f", documentAmount.abs()).replace(".", "").replace(",", "");
				documentAmountAsString = leftPadding(documentAmountAsString, 17, "0", true);
				//	Document Date
				String documentDateAsString = new SimpleDateFormat(REFERENCE_DATE_FORMAT).format(documentDate);
				//	Withholding Tax
				String withholdingTaxAsString = String.format("%.2f", withholdingTax).replace(".", "").replace(",", "");
				withholdingTaxAsString = leftPadding(withholdingTaxAsString, 17, "0", true);
				//	Constant
				String constant = leftPadding("", 187, " ");
				//	Line
				StringBuffer line = new StringBuffer();
				line.append(Env.NL)						//	New Line
					.append(registerType)				//	Register Type
					.append(documentNo)					//	Document No
					.append(documentAmountAsString)		//	Document Amount
					.append(iSOCode)					//	ISO Code
					.append(documentDateAsString)		//	Document Date
					.append(withholdingTaxAsString)		//	Withholding Tax
					.append(constant);					//	Constant
				//	
				s_log.fine("Write Line");
				writeLine(line.toString());
			});
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
