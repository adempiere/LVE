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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MBank;
import org.compiere.model.MBankAccount;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPaySelection;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.eevolution.model.MHRMovement;
import org.eevolution.model.MHRProcess;
import org.erpya.lve.util.ColumnsAdded;

/**
 * 	Implementation for Export Payment from Mercantil bank for Payroll
 * 	@author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class BNCNomina extends LVEPaymentExportList {

	/** Logger								*/
	static private CLogger	s_log = CLogger.getCLogger (MercantilNominaFacil.class);
	/**	Payroll process list	*/
	private Map<Integer, MHRProcess> payrollProcessMap = new HashMap<Integer, MHRProcess>();
	/**	Payroll process Amount	*/
	private Map<Integer, BigDecimal> payrollAmountMap = new HashMap<Integer, BigDecimal>();
	
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
			//	Bank Client No
			String bankClientNo = "";
			if(!Util.isEmpty(bank.get_ValueAsString("BankClientNo"))) {
				bankClientNo = processValue(bank.get_ValueAsString(ColumnsAdded.COLUMNNAME_BankClientNo));
				bankClientNo = leftPadding(bankClientNo, 6, "0", true);
			} else {
				addError(Msg.parseTranslation(Env.getCtx(), "@BankClientNo@ @NotFound@"));
			}
			//	Fields of Control Register (fixed data)
			String paymentBatchNo = processValue(paySelection.getDocumentNo());
			paymentBatchNo = leftPadding(paymentBatchNo, 15, "0", true);
			String orgTaxId = processValue(orgInfo.getTaxID().replace("-", "")).trim();
			//	Process Person Type
			String organizationType = "";
			if(!Util.isEmpty(orgTaxId)){
				orgTaxId = orgTaxId.replace("-", "").trim();
				organizationType = orgTaxId.substring(0, 1);
				orgTaxId = orgTaxId.replaceAll("\\D+", "");
			} else {
				addError(Msg.parseTranslation(Env.getCtx(), "@TaxID@ @NotFound@: " + org.getValue() + " - " + org.getName()));
			}
			//	Payment Amount
			String totalAmtAsString = String.format("%.2f", getTotalAmount(checks).abs()).replace(".", "").replace(",", "");
			if(totalAmtAsString.length() > 15) {
				addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ > @InValid@"));
			}
			totalAmtAsString = leftPadding(totalAmtAsString, 13, "0", true);
			//	Account No
			String bankAccountNo = processValue(bankAccount.getAccountNo()).trim();
			bankAccountNo = bankAccountNo.replace(" ", "");
			if(bankAccountNo.length() > 20) {
				bankAccountNo = bankAccountNo.substring(20, bankAccountNo.length());
			}
			//	
			StringBuffer header = new StringBuffer();
			//	Debt Note
			header.append("ND ")						//  Constant
				.append(bankAccountNo)				//	Bank Client No
				.append(totalAmtAsString)			//  Total Amount
				.append(organizationType)			//	Organization Type
				.append(orgTaxId);					//  Tax ID
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
						addPayrollProcess(payselectionCheck);
						if(bpAccount != null) {
							String bPTaxId = bpAccount.getA_Ident_SSN();
							//	Process Person Type
							String personType = "";
							if(!Util.isEmpty(bPTaxId)){
								bPTaxId = bPTaxId.replace("-", "").trim();
								personType = bPTaxId.substring(0, 1);
								bPTaxId = bPTaxId.replaceAll("\\D+","");
								bPTaxId = leftPadding(bPTaxId, 9, "0", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@BPTaxID@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	Process Business Partner Account No
							String bPAccountNo = processValue(bpAccount.getAccountNo());
							if(!Optional.ofNullable(bPAccountNo).isPresent()) {
								addError(Msg.parseTranslation(Env.getCtx(), "@AccountNo@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							
							//	Payment Amount
							String amountAsString = String.format("%.2f", payselectionCheck.getPayAmt().abs()).replace(".", "").replace(",", "");
							if(amountAsString.length() > 15) {
								addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ > @Valid@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							amountAsString = leftPadding(amountAsString, 13, "0", true);

							//	Write Credit Register
							StringBuffer line = new StringBuffer();
							line.append(Env.NL)						//	New Line
								.append("NC ")						//	Constant
								.append(bPAccountNo)				//  BP Bank Account
								.append(amountAsString)				//	Payment Amount
								.append(personType)					//	Type Register	
								.append(bPTaxId);					//	BP Value

							s_log.fine("Write Line");
							writeLine(line.toString());
						} else {
							addError(Msg.parseTranslation(Env.getCtx(), "@C_BP_BankAccount_ID@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
						}
			});
			//writeDetail();
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
	 * Add Payroll to list
	 * @param payselectionCheck
	 */
	private void addPayrollProcess(MPaySelectionCheck payselectionCheck) {
		payselectionCheck.getPaySelectionLinesAsList(false).stream()
		.forEach(paySelectionLine -> {
			MHRMovement movement = (MHRMovement) paySelectionLine.getHR_Movement();
			payrollProcessMap.put(movement.getHR_Process_ID(), (MHRProcess) movement.getHR_Process());
			//	
			BigDecimal amount = payrollAmountMap.get(movement.getHR_Process_ID());
			if(amount != null) {
				amount = amount.add(movement.getAmount());
			} else {
				amount = Env.ZERO;
			}
			//	Set Amount
			payrollAmountMap.put(movement.getHR_Process_ID(), amount);
		});
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
