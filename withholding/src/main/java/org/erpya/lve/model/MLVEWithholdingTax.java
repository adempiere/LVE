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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.model.MClientInfo;
import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.Env;

/**
 * Withholding Tax for Venezuela
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class MLVEWithholdingTax extends X_LVE_WithholdingTax {
	
	public MLVEWithholdingTax(Properties ctx, int LVE_WithholdingTax_ID, String trxName) {
		super(ctx, LVE_WithholdingTax_ID, trxName);
	}
	
	public MLVEWithholdingTax(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -7050562622116465459L;
	/** Static Cache */
	private static CCache<Integer, MLVEWithholdingTax> withholdingTaxCacheIds = new CCache<Integer, MLVEWithholdingTax>(Table_Name, 30);
	/** Static Cache */
	private static CCache<String, MLVEWithholdingTax> withholdingTaxFromClientCache = new CCache<String, MLVEWithholdingTax>(Table_Name + "_Client", 30);
	/**	Client Info Tax Unit	*/
	private MLVEList tributeUnitDefinition = null;
	
	/**
	 * Get/Load Withholding Tax [CACHED]
	 * @param ctx context
	 * @param definitionId
	 * @param trxName
	 * @return activity or null
	 */
	public static MLVEWithholdingTax getById(Properties ctx, int definitionId, String trxName) {
		if (definitionId <= 0)
			return null;

		MLVEWithholdingTax withholdingTax = withholdingTaxCacheIds.get(definitionId);
		if (withholdingTax != null && withholdingTax.get_ID() > 0)
			return withholdingTax;

		withholdingTax = new Query(ctx , Table_Name , COLUMNNAME_LVE_WithholdingTax_ID + "=?" , trxName)
				.setClient_ID()
				.setParameters(definitionId)
				.first();
		if (withholdingTax != null && withholdingTax.get_ID() > 0) {
			withholdingTaxCacheIds.put(withholdingTax.get_ID(), withholdingTax);
		}
		return withholdingTax;
	}
	
	/**
	 * Get from Client definition
	 * @param ctx
	 * @param documentTypeId
	 * @return
	 */
	public static MLVEWithholdingTax getFromClient(Properties ctx, int organizationId) {
		String key = Env.getAD_Client_ID(ctx) + "|" + organizationId;
		MLVEWithholdingTax withholdingTaxDefinition = withholdingTaxFromClientCache.get(key);
		if(withholdingTaxDefinition != null) {
			return withholdingTaxDefinition;
		}
		//	
		String whereClause = (organizationId > 0? I_LVE_WithholdingTax.COLUMNNAME_AD_Org_ID + " IN(" + organizationId + ", 0)": null);
		//	
		withholdingTaxDefinition = new Query(ctx, I_LVE_WithholdingTax.Table_Name, whereClause, null)
				.setClient_ID()
				.setOnlyActiveRecords(true)
				.setOrderBy(I_LVE_WithholdingTax.COLUMNNAME_AD_Org_ID + " DESC")
				.<MLVEWithholdingTax>first();
		//	Set
		withholdingTaxFromClientCache.put(key, withholdingTaxDefinition);
		//	Return 
		return withholdingTaxDefinition;
	}
	
	/**
	 * get Version Rate from Valid From
	 * @param listSearchKey
	 * @param validFrom
	 * @return
	 */
	public BigDecimal getValidTributeUnitAmount(Timestamp validFrom) {
		MLVEListVersion version = getValidTributeUnitInstance(validFrom);
		if(version == null) {
			return Env.ZERO;
		}
		//	Default
		return version.getAmount();
	}

	/**
	 * Get Valid Version
	 * @param validFrom
	 * @return
	 */
	public MLVEListVersion getValidTributeUnitInstance(Timestamp validFrom) {
		MLVEListVersion tributeUnit = null;
		if(tributeUnitDefinition == null) {
			tributeUnitDefinition = new Query(getCtx(), I_LVE_List.Table_Name, I_LVE_List.COLUMNNAME_LVE_ListType_ID + " = ?", get_TableName())
				.setClient_ID()
				.setParameters(MClientInfo.get(getCtx()).get_ValueAsInt("TributeUnitType_ID"))
				.setOnlyActiveRecords(true)
				.first();
		}
		if(tributeUnitDefinition != null) {
			tributeUnit = tributeUnitDefinition.getValidVersionInstance(validFrom);
		}
		//	Default
		return tributeUnit;
	}
	
	@Override
	public String toString() {
		return "MLVEWithholdingTax [getLVE_WithholdingTax_ID()=" + getLVE_WithholdingTax_ID() + ", getName()="
				+ getName() + ", getUUID()=" + getUUID() + "]";
	}
}
