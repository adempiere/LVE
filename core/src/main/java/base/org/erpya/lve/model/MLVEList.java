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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * 	Class added from standard values
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/LVE/issues/2>
 * 		@see FR [ 2 ] Add standard LVE values</a>
 */
public class MLVEList extends X_LVE_List {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8380361947201510444L;

	public MLVEList(Properties ctx, int LVE_List_ID, String trxName) {
		super(ctx, LVE_List_ID, trxName);
	}

	public MLVEList(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**	Cache						*/
	private static CCache<Integer, MLVEList> cache = new CCache<Integer, MLVEList>(Table_Name, 40, 5);	//	5 minutes
	/**	List version with Valid From	*/
	private CCache<String, MLVEListVersion> listVersionCache = new CCache<String, MLVEListVersion>(I_LVE_ListVersion.Table_Name + "_ValidFrom", 40, 5);	//	5 minutes
	/**
	 * 	Get MLVEList from Cache
	 *	@param ctx context
	 *	@param listId id
	 *	@return MLVEList or null
	 */
	public static MLVEList get (Properties ctx, int listId) {
		if (listId <= 0) {
			return null;
		}
		Integer key = new Integer (listId);
		MLVEList retValue = (MLVEList) cache.get (key);
		if (retValue != null) {
			return retValue;
		}
		retValue = new MLVEList(ctx, listId, null);
		if (retValue.get_ID () != 0) {
			cache.put (key, retValue);
		}
		return retValue;
	}	//	get
	
	/**
	 * Get amount for list from current object
	 * @param from
	 * @param amount
	 * @param columnParam
	 * @return
	 */
	public BigDecimal getList(Timestamp from, BigDecimal amount, String columnParam) {
		return getList(getAD_Client_ID(), getValue(), from, amount, columnParam);
	}
	
	/**
	 * Helper Method : Get a value for a column from value range
	 * @param listSearchKey Value List
	 * @from from date to valid list
	 * @param amount Amount to search
	 * @param columnParam Number of column to return (1.......8)
	 * @return The amount corresponding to the designated column 'column'
	 */
	public static BigDecimal getList(int clientId, String listSearchKey, Timestamp from, BigDecimal amount, String columnParam) {
		BigDecimal value = Env.ZERO;
		ArrayList<Object> params = new ArrayList<Object>();
		String sqlList = "SELECT " + columnParam +
			" FROM LVE_List l " +
			"INNER JOIN LVE_ListVersion lv ON (lv.LVE_List_ID=l.LVE_List_ID) " +
			"INNER JOIN LVE_ListLine ll ON (ll.LVE_ListVersion_ID=lv.LVE_ListVersion_ID) " +
			"WHERE l.IsActive='Y' AND lv.IsActive='Y' AND ll.IsActive='Y' AND l.Value = ? AND " +
			"l.AD_Client_ID = ? AND " +
			"(? BETWEEN lv.ValidFrom AND lv.ValidTo ) AND " +
			"(? >= ll.MinValue AND ? <= ll.MaxValue)";
		params.add(listSearchKey);
		params.add(clientId);
		params.add(from);
		params.add(amount);
		params.add(amount);
		//	
		value = DB.getSQLValueBDEx(null,sqlList,params);
		return value;
	} // getList
	
	/**
	 * get Version Rate from Valid From
	 * @param listSearchKey
	 * @param validFrom
	 * @return
	 */
	public BigDecimal getListVersionAmount(Timestamp validFrom) {
		MLVEListVersion version = getValidVersionInstance(validFrom);
		if(version == null) {
			return Env.ZERO;
		}
		//	Default
		return version.getAmount();
	}
	
	/**
	 * Get Valid Rate Instance of List Version
	 * @param validFrom
	 * @return
	 */
	public MLVEListVersion getValidVersionInstance(Timestamp validFrom) {
		SimpleDateFormat format = (SimpleDateFormat)DateFormat.getInstance();
		format.applyPattern("yyyyMMdd");
		String key = getLVE_List_ID() + "|" + format.format(validFrom);
		MLVEListVersion listVersion = listVersionCache.get(key);
		if(listVersion == null) {
			ArrayList<Object> params = new ArrayList<Object>();
			StringBuffer whereClause = new StringBuffer(MLVEListVersion.COLUMNNAME_LVE_List_ID + " = ?");
			params.add(getLVE_List_ID());
			// check ValidFrom
			whereClause.append(" AND ").append(MLVEListVersion.COLUMNNAME_ValidFrom + "<=?");
			params.add(validFrom);
			//check client
			listVersion = new Query(getCtx(), MLVEListVersion.Table_Name, whereClause.toString(), null)
					.setParameters(params)
					.setOrderBy(MLVEListVersion.COLUMNNAME_ValidFrom + " DESC")
					.setClient_ID()
					.setOnlyActiveRecords(true)
					.first();
			if(listVersion != null) {
				listVersionCache.put(key, listVersion);
			}
		}
		//	
		return listVersion;
	}
	
	@Override
	public String toString() {
		return "MLVEList [getLVE_List_ID()=" + getLVE_List_ID() + ", getValue()="
				+ getValue() + ", getName()=" + getName() + "]";
	}
}
