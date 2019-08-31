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

import java.util.Properties;

import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.I_C_BPartner;
import org.compiere.model.I_C_Order;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MLocation;
import org.erpya.lve.util.ColumnsAdded;

/**
 * 	Class added from standard values
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/LVE/issues/2>
 * 		@see FR [ 2 ] Add standard LVE values</a>
 */
public class CalloutOrder extends CalloutEngine {
	/**
	 * Set Tax ID from Value
	 * @param ctx
	 * @param WindowNo
	 * @param mTab
	 * @param field
	 * @param value
	 * @return
	 */
	public String codeSICA (Properties ctx, int WindowNo, GridTab mTab, GridField field, Object value) {
		Integer taxID = null;
		String val = null;
		if(field.getColumnName().equals(I_C_Order.COLUMNNAME_Bill_Location_ID)){
			taxID = (Integer)value;
			if(taxID != null && taxID != 0){
				MBPartnerLocation bPartnerLocation = new MBPartnerLocation(ctx, taxID, null);  
				val = bPartnerLocation.get_ValueAsString(ColumnsAdded.COLUMNNAME_Code);
				// Set Code SICA
				mTab.setValue(ColumnsAdded.COLUMNNAME_Code, val);
			}
		} 
		return "";
	}	//	codeSICA	
}	//	CalloutOrder
