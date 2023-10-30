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
public class MLVEListType extends X_LVE_ListType {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8380361947201510444L;

	public MLVEListType(Properties ctx, int LVE_ListType_ID, String trxName) {
		super(ctx, LVE_ListType_ID, trxName);
	}

	public MLVEListType(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**	Cache						*/
	private static CCache<Integer, MLVEListType> cache = new CCache<Integer, MLVEListType>(Table_Name, 40, 5);	//	5 minutes
	/**
	 * 	Get MLVEListType from Cache
	 *	@param ctx context
	 *	@param listId id
	 *	@return MLVEList or null
	 */
	public static MLVEListType get (Properties ctx, int listId) {
		if (listId <= 0) {
			return null;
		}
		Integer key = Integer.valueOf(listId);
		MLVEListType retValue = (MLVEListType) cache.get (key);
		if (retValue != null) {
			return retValue;
		}
		retValue = new MLVEListType(ctx, listId, null);
		if (retValue.get_ID () != 0) {
			cache.put (key, retValue);
		}
		return retValue;
	}	//	get

	@Override
	public String toString() {
		return "MLVEListType [getLVE_ListType_ID()=" + getLVE_ListType_ID() + ", getName()=" + getName()
				+ ", getValue()=" + getValue() + "]";
	}
}
