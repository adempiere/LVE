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
package org.erpya.lve.bank.matcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Class used for Test import matcher
 *
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
import org.compiere.impexp.BankStatementMatchInfo;
import org.compiere.impexp.BankStatementMatcherInterface;
import org.compiere.model.MBankStatementLine;
import org.compiere.model.X_I_BankStatement;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 * Add matcher by currency, amount and transaction date
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class CurrencyAmountAndTrxDate implements BankStatementMatcherInterface {

	public CurrencyAmountAndTrxDate() {
		
	}

	@Override
	public BankStatementMatchInfo findMatch(MBankStatementLine bsl) {
		return null;
	}

	@Override
	public BankStatementMatchInfo findMatch(X_I_BankStatement ibs) {
		BankStatementMatchInfo info = new BankStatementMatchInfo();
		//	Validate
		if(ibs.getC_Payment_ID() != 0) {
			return info;
		}
		//	
		String ORDERVALUE = " DESC NULLS LAST";
		StringBuffer sql = new StringBuffer("SELECT p.C_Payment_ID "
				+ "FROM C_Payment p "
				+ "WHERE p.AD_Client_ID = ? ");
		//	Were
		StringBuffer where = new StringBuffer();
		StringBuffer orderByClause = new StringBuffer(" ORDER BY ");
		//	Search criteria
		List<Object> params = new ArrayList<Object>();
		//	Client
		params.add(ibs.getAD_Client_ID());
		//	Add Currency
		if(!Util.isEmpty(ibs.getISO_Code())) {
			where.append(" AND EXISTS(SELECT 1 FROM C_Currency c WHERE c.C_Currency_ID = p.C_Currency_ID AND c.ISO_Code = ?) ");
			params.add(ibs.getISO_Code());
		} else if(ibs.getC_Currency_ID() != 0){
			where.append(" AND p.C_Currency_ID = ? ");
			params.add(ibs.getC_Currency_ID());
		}
		//	For Amount
		if(where.length() > 0) {
			where.append(" AND ");
		}
		//	Validate amount for it
		boolean isReceipt = ibs.getTrxAmt().compareTo(Env.ZERO) > 0;
		where.append("(p.PayAmt = ? ");
		params.add(isReceipt
				? ibs.getTrxAmt()
						: ibs.getTrxAmt().negate());
		//	Add Receipt
		where.append("AND p.IsReceipt = ? )");
		params.add(isReceipt);
		//	Add Transaction Date
		if(ibs.getEftStatementDate() != null
				|| ibs.getEftStatementLineDate() != null
				|| ibs.getEftValutaDate() != null
				|| ibs.getDateAcct() != null) {
			where.append(" AND (");
			StringBuffer internalTransaction = new StringBuffer();
			//	EFT Statement Date
			if(ibs.getEftStatementDate() != null) {
				internalTransaction.append("p.DateTrx = ?");
				params.add(ibs.getEftStatementDate());
			}
			//	EFT Statement Line Date
			if(ibs.getEftStatementLineDate() != null) {
				if(internalTransaction.length() > 0) {
					internalTransaction.append(" OR ");
				}
				internalTransaction.append("p.DateTrx = ?");
				params.add(ibs.getEftStatementLineDate());
			}
			//	EFT Valuata Date
			if(ibs.getEftValutaDate() != null) {
				if(internalTransaction.length() > 0) {
					internalTransaction.append(" OR ");
				}
				internalTransaction.append("p.DateTrx = ?");
				params.add(ibs.getEftValutaDate());
			}
			//	EFT Accounting Date
			if(ibs.getDateAcct() != null) {
				if(internalTransaction.length() > 0) {
					internalTransaction.append(" OR ");
				}
				internalTransaction.append("p.DateTrx = ?");
				params.add(ibs.getDateAcct());
			}
			//	
			where.append(internalTransaction);
			where.append(")");
		}
		//	For Account
		if(where.length() > 0) {
			where.append(" AND ");
		}
		where.append("(p.C_BankAccount_ID = ?)");
		params.add(ibs.getC_BankAccount_ID());
		//	Additional validation
		where.append(" AND p.DocStatus IN('CO', 'CL')");
		where.append(" AND p.IsReconciled = 'N'");
		where.append(" AND NOT EXISTS(SELECT 1 FROM I_BankStatement i WHERE i.C_Payment_ID = p.C_Payment_ID) ");
		//	Add Order By
		orderByClause.append("p.DateTrx ASC");
		orderByClause.append(", p.DocumentNo").append(ORDERVALUE);
		orderByClause.append(", p.CheckNo").append(ORDERVALUE);
		orderByClause.append(", p.Description").append(ORDERVALUE);
		//	Add where
		sql.append(where);
		//	Add Order By
		sql.append(orderByClause);
		//	Find payment
		int paymentId = DB.getSQLValue(ibs.get_TrxName(), sql.toString(), params);
		//	set if exits
		if(paymentId > 0) {
			info.setC_Payment_ID(paymentId);
		}
		//	Set Business Partner
		if(!Util.isEmpty(ibs.getEftPayeeAccount())) {
			String additionaWhereClause = "";
			String businessPartnerTaxId = ibs.getEftPayeeAccount().trim();
			businessPartnerTaxId = businessPartnerTaxId.replace("-", "").trim();
			String businessPartneType = businessPartnerTaxId.substring(0, 1);
			businessPartnerTaxId = businessPartnerTaxId.replaceAll("\\D+", "");
			businessPartnerTaxId = getValidReference(businessPartnerTaxId);
			businessPartnerTaxId = "%" + businessPartnerTaxId + "%";
			List<Object> businessPartnerParameters = new ArrayList<>();
			businessPartnerParameters.add(ibs.getAD_Client_ID());
			businessPartnerParameters.add(businessPartnerTaxId);
			businessPartnerParameters.add(businessPartnerTaxId);
			if(!Util.isEmpty(businessPartneType)) {
				businessPartneType = businessPartneType+ "%";
				businessPartnerParameters.add(businessPartneType);
				businessPartnerParameters.add(businessPartneType);
				additionaWhereClause = " AND (UPPER(Value) LIKE ? OR UPPER(TaxID) LIKE ?)";
			}
			//	Find
			int businessPartnerId = DB.getSQLValue(ibs.get_TrxName(), 
					"SELECT C_BPartner_ID FROM C_BPartner WHERE AD_Client_ID = ? AND (Value LIKE ? OR TaxID LIKE ?)" + additionaWhereClause, businessPartnerParameters);
			if(businessPartnerId > 0) {
				info.setC_BPartner_ID(businessPartnerId);
			}
		}
		return info;
	}
	
	/**
	 * Get valid reference for value
	 * @param reference
	 * @return
	 */
	private String getValidReference(String reference) {
		if(!Util.isEmpty(reference)) {
			reference = reference.trim();
			reference = reference.replace(".", "");
			if(reference.matches("[+-]?\\d*(\\.\\d+)?")) {
				Long longReference = null;
				try {
					longReference = new Long(reference);
				} catch (Exception e) {
					//	Nothing
				}
				if(longReference != null) {
					reference = String.valueOf(longReference);
				}
			}
		}
		return reference;
	}
}
