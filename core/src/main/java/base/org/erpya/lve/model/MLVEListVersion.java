/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 or later of the                                  *
 * GNU General Public License as published                                    *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2016 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package org.erpya.lve.model;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.util.CCache;


/**
 * List version Model Class
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class MLVEListVersion extends X_LVE_ListVersion {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5508386218141968441L;
	/**	List version with Valid From	*/
	private CCache<String, List<MLVEListLine>> listLineCache = new CCache<String, List<MLVEListLine>>(I_LVE_ListLine.Table_Name + "_LineID", 40, 5);	//	5 minutes

	public MLVEListVersion(Properties ctx, int WH_Log_ID, String trxName) {
		super(ctx, WH_Log_ID, trxName);
	}
	
	public MLVEListVersion(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	public String toString() {
		return "MLVEListVersion [getAmount()=" + getAmount() + ", getLVE_List_ID()=" + getLVE_List_ID()
				+ ", getLVE_ListVersion_ID()=" + getLVE_ListVersion_ID() + ", getName()=" + getName()
				+ ", getValidFrom()=" + getValidFrom() + "]";
	}
	
	/**
	 * Helper Method : Get a value for a column from value range
	 * @param listSearchKey Value List
	 * @from from date to valid list
	 * @param amount Amount to search
	 * @param columnParam Number of column to return (1.......8)
	 * @return The amount corresponding to the designated column 'column'
	 */
	public List<MLVEListLine> getListLine() {
		
		String key = getLVE_ListVersion_ID() + "" ;
		List<MLVEListLine> listLine = listLineCache.get(key);
		if(listLine == null) {
			ArrayList<Object> params = new ArrayList<Object>();
			StringBuffer whereClause = new StringBuffer(COLUMNNAME_LVE_ListVersion_ID + " = ?");
			params.add(getLVE_ListVersion_ID());
			//check client
			listLine = new Query(getCtx(), MLVEListLine.Table_Name, whereClause.toString(), null)
					.setParameters(params)
					.setOrderBy(MLVEListLine.COLUMNNAME_SeqNo + " DESC")
					.setClient_ID()
					.setOnlyActiveRecords(true)
					.list();
			if(listLine != null) {
				listLineCache.put(key, listLine);
			}
		}
		return listLine;
		
	} // getList
}
