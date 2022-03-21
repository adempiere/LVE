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
import java.util.List;
import java.util.Optional;

import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MBank;
import org.compiere.model.MBankAccount;
import org.compiere.model.MOrg;
import org.compiere.model.MPaySelection;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.erpya.lve.util.LVEUtil;

/**
 * 	Implementation for Export Payment from Mercantil bank for Payroll
 * 	@author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class VenezuelaNomina extends LVEPaymentExportList {

	/** Logger								*/
	static private CLogger	s_log = CLogger.getCLogger (MercantilNominaFacil.class);
	/**	Header Short Format	*/
	private final String HEADER_SHORT_DATE_FORMAT = "dd/MM/yy";
			
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
			String headerBankClientNo = bankAccount.get_ValueAsString(LVEUtil.COLUMNNAME_BankClientNo);
			if(Util.isEmpty(headerBankClientNo)) {
				headerBankClientNo = bank.get_ValueAsString(LVEUtil.COLUMNNAME_BankClientNo);
			}
			if(!Util.isEmpty(headerBankClientNo)) {
				headerBankClientNo = processValue(headerBankClientNo);
				headerBankClientNo = leftPadding(headerBankClientNo, 5, "0", true);
			} else {
				addError(Msg.parseTranslation(Env.getCtx(), "@BankClientNo@ @NotFound@"));
			}
			String lineBankClientNo = leftPadding(headerBankClientNo, 6, "0", true);
			//	Fields of Control Register (fixed data)
			String orgName = org.getName();
			//	Add padding
			orgName = rightPadding(orgName, 41, " ", true);
			String constant = "01";
			//	Payment Amount
			String totalAmtAsString = String.format("%.2f", getTotalAmount(checks).abs()).replace(".", "").replace(",", "");
			if(totalAmtAsString.length() > 15) {
				addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ > @InValid@"));
			}
			totalAmtAsString = leftPadding(totalAmtAsString, 13, "0", true);
			//	Format Date
			SimpleDateFormat shortFormat = new SimpleDateFormat(HEADER_SHORT_DATE_FORMAT);
			String payDate = shortFormat.format(paySelection.getPayDate());

			//	Account No
			String bankAccountNo = processValue(bankAccount.getAccountNo()).trim();
			bankAccountNo = bankAccountNo.replace(" ", "");
			//	
			StringBuffer header = new StringBuffer();
			//	Debt Note
			header.append("")					//  Constant
				.append(orgName)				//	Organization Name
				.append(bankAccountNo)			//	Bank Client No
				.append(constant)				//	Constant
				.append(payDate)				//	Payment Date
				.append(totalAmtAsString)		//	Total Amount
				.append(headerBankClientNo);	//  Bank Client No
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
						//addPayrollProcess(payselectionCheck);
						if(bpAccount != null) {
							String bPTaxId = bpAccount.getA_Ident_SSN();
							String constantLine = "1";
							if(!Util.isEmpty(bPTaxId)){
								bPTaxId = bPTaxId.replace("-", "").trim();
								bPTaxId = bPTaxId.replaceAll("\\D+","");
								bPTaxId = leftPadding(bPTaxId, 10, "0", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@BPTaxID@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	Process Business Partner Account No
							String bPAccountNo = processValue(bpAccount.getAccountNo());
							//	Process Account Name
							String bPName = processValue(bpAccount.getA_Name());
							if(Optional.ofNullable(bPName).isPresent()) {
								bPName = rightPadding(bPName, 40, " ", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@A_Name@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	Payment Amount
							String amountAsString = String.format("%.2f", payselectionCheck.getPayAmt().abs()).replace(".", "").replace(",", "");
							if(amountAsString.length() > 11) {
								addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ > @Valid@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							amountAsString = leftPadding(amountAsString, 11, "0", true);
							String accountType = "0";
							if(!Util.isEmpty(bpAccount.getBankAccountType())
									&& bpAccount.getBankAccountType().equals(MBankAccount.BANKACCOUNTTYPE_Savings)) {
								accountType = "1";
							}
							String lineConstant = "770";
							//	Write Credit Register
							StringBuffer line = new StringBuffer();
							line.append(Env.NL)						//	New Line
								.append(constantLine)				//	Constant
								.append(bPAccountNo)				//	Bank Account
								.append(amountAsString)				//	Amount
								.append(accountType)				//	Account Type
								.append(lineConstant)				//	Constant
								.append(bPName)						//	BP Value
								.append(bPTaxId)					//	BP Tax
								.append(lineBankClientNo);			//	Customer Account No
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
		return value.replaceAll("[+^:&áàäéèëíìïóòöúùñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ$,;*/áéíóúÁÉÍÓÚñÑ¿¡]", "");
	}
	
	/**
	 * Get Total Amount of Payments
	 * @param checks
	 * @return
	 */
	private BigDecimal getTotalAmount(List<MPaySelectionCheck> checks) {
		BigDecimal totalAmount = Env.ZERO;
		for(MPaySelectionCheck payment : checks) {
			totalAmount = totalAmount.add(payment.getPayAmt());
		}
		//	Default return
		return totalAmount;
	}
}
