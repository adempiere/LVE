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

package org.erpya.lve.process;

import org.compiere.model.MInvoice;
import org.compiere.process.ProcessInfo;
import org.eevolution.services.dsl.ProcessBuilder;
import org.spin.process.WithholdingGenerate;

/** Generated Process for (Withholding Process for AR Invoice / AR Credit Memo)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.2
 */
public class ARWithholdingProcess extends ARWithholdingProcessAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		String result = "";
		
		ProcessInfo info = ProcessBuilder.create(getCtx())
			.process(org.spin.process.WithholdingProcess.class)
	        .withParentProcess(null)
	        .withParameter(WH_TYPE_ID, getTypeId())
	        .withParameter(WH_SETTING_ID, getSettingId())
	        .withParameter(WH_DEFINITION_ID, getDefinitionId())
	        .withParameter(A_BASE_AMOUNT, getBaseAmount())
	        .withParameter(WITHHOLDINGAMT, getWithholdingAmt())
	        .withParameter(WITHHOLDINGRATE, getWithholdingRate())
	        .withParameter(DOCUMENTNO, getDocumentNo())
	        .withRecordId(MInvoice.Table_ID ,getRecord_ID())
	        .withoutTransactionClose()
	        .execute();
		
		if (!info.isError()) {
			result += info.getSummary() + "\n";
			
			info = ProcessBuilder.create(getCtx())
				.process(org.spin.process.WithholdingGenerate.class)
		        .withParentProcess(null)
		        .withParameter(WithholdingGenerate.C_INVOICE_ID, getRecord_ID())
		        .withParameter(WH_TYPE_ID, getTypeId())
		        .withParameter(DATEDOC, getDateDoc())
		        .withoutTransactionClose()
		        .execute();
		}
		
		if (!info.isError()) 
			result += info.getSummary();
		else 
			result = "@Error@ " + info.getThrowable().getMessage();
		
		return result;
	}
}