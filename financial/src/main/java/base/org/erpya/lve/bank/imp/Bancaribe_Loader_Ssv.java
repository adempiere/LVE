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

import org.spin.util.impexp.BankStatementHandler;
import org.spin.util.impexp.BankTransactionAbstract;

/**
 * Bancaribe separado por punto y coma
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 * <li> 
 * @see https://github.com/adempiere/adempiere/issues/1701
 */
public final class Bancaribe_Loader_Ssv extends BankStatementHandler {
	@Override
	protected BankTransactionAbstract getBankTransactionInstance() {
		return new Bancaribe_Ssv();
	}
}
