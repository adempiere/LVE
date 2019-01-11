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
package org.erpya.lve.bank.imp;

import java.math.BigDecimal;

import org.compiere.impexp.OFXBankStatementHandler;
import org.compiere.util.Util;

/**
 * Custom format for Provincial Bank Transaction
 * It is a specific solution for a bank
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 * <li> FR [ 1701 ] Add support to OFX format
 * @see https://github.com/adempiere/adempiere/issues/1701
 */
public class Bancaribe_BankTransaction extends OFXBankStatementHandler {
	/**	Debt Constant	*/
	public static final String DEBT = "D";
	/**	Credit Constant	*/
	public static final String CREDIT = "C";

	@Override
	public String getTrxType() {
		return super.getPayeeName();
	}
	
	@Override
	public BigDecimal getTrxAmt() {
		BigDecimal trxAmt = super.getTrxAmt();
		if(!Util.isEmpty(getTrxType())
				&& getTrxType().equals(DEBT)
				&& trxAmt != null) {
			trxAmt = trxAmt.negate();
		}
		//	Default
		return trxAmt;
	}
	
	@Override
	public BigDecimal getStmtAmt() {
		BigDecimal smtAmt = super.getStmtAmt();
		if(!Util.isEmpty(getTrxType())
				&& getTrxType().equals(DEBT)
				&& smtAmt != null) {
			smtAmt = smtAmt.negate();
		}
		//	Default
		return smtAmt;
	}
	
}
