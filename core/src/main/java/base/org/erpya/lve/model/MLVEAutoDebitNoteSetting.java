/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.										*
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/
package org.erpya.lve.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;
import org.compiere.model.Query;
import org.compiere.util.CCache;

public class MLVEAutoDebitNoteSetting extends X_LVE_AutoDebitNoteSetting{

	private static final long serialVersionUID = 1L;

	public MLVEAutoDebitNoteSetting(Properties ctx, int LVE_AutoDebitNoteSetting_ID, String trxName) {
		super(ctx, LVE_AutoDebitNoteSetting_ID, trxName);
		// TODO Auto-generated constructor stub
	}
	public MLVEAutoDebitNoteSetting(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected boolean beforeSave(boolean newRecord) {
		// TODO Auto-generated method stub
		if (newRecord)
			if (new Query(getCtx(), MLVEAutoDebitNoteSetting.Table_Name, COLUMNNAME_C_DocType_ID.concat("=?"), get_TrxName())
				.setParameters(getC_DocType_ID())
				.setClient_ID()
				.setOnlyActiveRecords(true)
				.count() > 0)
				throw new AdempiereException("@AlreadyExists@ -> @C_DocType_ID@ ".concat(MDocType.get(getCtx(), getC_DocType_ID()).getName()));
		
		return super.beforeSave(newRecord);
		
	}
	
	static public MLVEAutoDebitNoteSetting get (Properties ctx, int C_DocType_ID)
	{
		MLVEAutoDebitNoteSetting retValue = (MLVEAutoDebitNoteSetting)s_cache.get(C_DocType_ID);
		if (retValue == null)
		{
			retValue = new Query(ctx, MLVEAutoDebitNoteSetting.Table_Name, COLUMNNAME_C_DocType_ID.concat("=?"), null)
					.setParameters(C_DocType_ID)
					.setClient_ID()
					.setOnlyActiveRecords(true)
					.first() ;
			if (retValue !=null )
				s_cache.put(C_DocType_ID, retValue);
		}
		return retValue; 
	} 	//	get
	
	/**	Cache					*/
	static private CCache<Integer,MLVEAutoDebitNoteSetting>	s_cache = new CCache<Integer,MLVEAutoDebitNoteSetting>(Table_Name, 20);

}
