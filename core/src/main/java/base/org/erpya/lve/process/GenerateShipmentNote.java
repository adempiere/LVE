/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.                                     *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net                                                  *
 * or https://github.com/adempiere/adempiere/blob/develop/license.html        *
 *****************************************************************************/

package org.erpya.lve.process;

import org.eevolution.distribution.model.MDDOrder;
import org.erpya.lve.util.LVEUtil;

/** Generated Process for (Generate Shipment Note)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.4
 */
public class GenerateShipmentNote extends GenerateShipmentNoteAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		String resultMsg = "";
		if (getRecord_ID() > 0
				&& getTable_ID() == MDDOrder.Table_ID) {
			MDDOrder ddOrder = new MDDOrder(getCtx(), getRecord_ID(), get_TrxName());
			resultMsg = "@Created@ -> " + LVEUtil.createSalesOrderFromDistributionOrder(ddOrder);
		}
		return resultMsg;
	}
}