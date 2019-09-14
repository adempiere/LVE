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
 * Copyright (C) 2003-2015 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package org.erpya.lve.util.exp;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.transform.sax.TransformerHandler;

import org.adempiere.pipo.PackOut;
import org.adempiere.pipo.handler.GenericPOHandler;
import org.compiere.model.MClientInfo;
import org.compiere.model.Query;
import org.erpya.lve.model.I_LVE_List;
import org.erpya.lve.model.I_LVE_ListLine;
import org.erpya.lve.model.I_LVE_ListVersion;
import org.erpya.lve.model.I_LVE_WithholdingTax;
import org.erpya.lve.model.MLVEList;
import org.erpya.lve.model.MLVEListLine;
import org.erpya.lve.model.MLVEListVersion;
import org.erpya.lve.model.MLVEWithholdingTax;
import org.xml.sax.SAXException;

/**
 * Withholding Exporter
 * @author Yamel Senih, ySenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class LVEWithholdingExporter extends GenericPOHandler {
	@Override
	public void create(Properties ctx, TransformerHandler document) throws SAXException {
		PackOut packOut = (PackOut) ctx.get("PackOutProcess");
		if(packOut == null ) {
			packOut = new PackOut();
			packOut.setLocalContext(ctx);
		}
		//	add here exclusion tables
		List<String> parentsToExclude = new ArrayList<String>();
		//	Export Withholding Tax Setup
		List<MLVEWithholdingTax> withholdingTaxList = new Query(ctx, I_LVE_WithholdingTax.Table_Name, null, null)
				.setOnlyActiveRecords(true)
				.setClient_ID()
				.setOrderBy(I_LVE_WithholdingTax.COLUMNNAME_Name)
				.list();
		//	Export
		for(MLVEWithholdingTax withholdingTax : withholdingTaxList) {
			if(withholdingTax.getLVE_WithholdingTax_ID() < PackOut.MAX_OFFICIAL_ID) {
				continue;
			}
			packOut.createGenericPO(document, withholdingTax, true, parentsToExclude);
			//	Export LVE List
			List<MLVEList> withholdingListList = new Query(ctx, I_LVE_List.Table_Name, "LVE_ListType_ID = ? OR LVE_ListType_ID = ?", null)
					.setParameters(withholdingTax.getWithholdingRateType_ID(), MClientInfo.get(ctx).get_ValueAsInt("TributeUnitType_ID"))
					.setOnlyActiveRecords(true)
					.setClient_ID()
					.setOrderBy(I_LVE_List.COLUMNNAME_Value)
					.list();
			//	Export
			for(MLVEList withholdingList : withholdingListList) {
				if(withholdingList.getLVE_List_ID() < PackOut.MAX_OFFICIAL_ID) {
					continue;
				}
				withholdingList.setAD_Org_ID(0);
				//	Remove default bank account
				packOut.createGenericPO(document, withholdingList, true, parentsToExclude);
				//	Export List Version
				List<MLVEListVersion> withholdingListVersionList = new Query(ctx, I_LVE_ListVersion.Table_Name, I_LVE_ListVersion.COLUMNNAME_LVE_List_ID + " = ?", null)
						.setParameters(withholdingList.getLVE_List_ID())
						.setOnlyActiveRecords(true)
						.setClient_ID()
						.setOrderBy(I_LVE_ListVersion.COLUMNNAME_ValidFrom)
						.list();
				//	Export
				for(MLVEListVersion withholdingListVersion : withholdingListVersionList) {
					if(withholdingListVersion.getLVE_List_ID() < PackOut.MAX_OFFICIAL_ID) {
						continue;
					}
					withholdingListVersion.setAD_Org_ID(0);
					//	Remove default bank account
					packOut.createGenericPO(document, withholdingListVersion, true, parentsToExclude);
					//	Export List Value
					List<MLVEListLine> withholdingLineVersionList = new Query(ctx, I_LVE_ListLine.Table_Name, I_LVE_ListLine.COLUMNNAME_LVE_ListVersion_ID + " = ?", null)
							.setParameters(withholdingListVersion.getLVE_ListVersion_ID())
							.setOnlyActiveRecords(true)
							.setClient_ID()
							.list();
					//	Export
					for(MLVEListLine withholdingListLine : withholdingLineVersionList) {
						if(withholdingListLine.getLVE_ListLine_ID() < PackOut.MAX_OFFICIAL_ID) {
							continue;
						}
						withholdingListLine.setAD_Org_ID(0);
						//	Remove default bank account
						packOut.createGenericPO(document, withholdingListLine, true, parentsToExclude);
					}
				}
			}
		}
	}
}
