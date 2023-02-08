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
package org.erpya.lve.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.CCache;

/**
 * 	Class added from standard values
 * 	@author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class MLVEListLine extends X_LVE_ListLine {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8380361947201510444L;

	public MLVEListLine(Properties ctx, int LVE_ListLine_ID, String trxName) {
		super(ctx, LVE_ListLine_ID, trxName);
	}

	public MLVEListLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**	Cache						*/
	private static CCache<Integer, MLVEListLine> cache = new CCache<Integer, MLVEListLine>(Table_Name, 40, 5);	//	5 minutes
	/**
	 * 	Get MLVEListType from Cache
	 *	@param ctx context
	 *	@param listId id
	 *	@return MLVEList or null
	 */
	public static MLVEListLine get (Properties ctx, int listId) {
		if (listId <= 0) {
			return null;
		}
		Integer key = Integer.valueOf(listId);
		MLVEListLine retValue = (MLVEListLine) cache.get (key);
		if (retValue != null) {
			return retValue;
		}
		retValue = new MLVEListLine(ctx, listId, null);
		if (retValue.get_ID () != 0) {
			cache.put (key, retValue);
		}
		return retValue;
	}	//	get

	@Override
	public String toString() {
		return "MLVEListLine [getLVE_ListLine_ID()=" + getLVE_ListLine_ID() + ", getMaxValue()=" + getMaxValue()
				+ ", getMinValue()=" + getMinValue() + ", getName()=" + getName() + "]";
	}
}
