/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program.	If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.erpya.lve.model.validator;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClient;
import org.compiere.model.MConversionRate;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MTax;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.erpya.lve.util.LVEUtil;

/**
 * Write here your change comment
 * @author Yamel Senih ysenih@erpya.com
 *
 */
public class CreditMemo implements ModelValidator {

	/** Logger */
	private static CLogger log = CLogger.getCLogger(CreditMemo.class);
	/** Client */
	private int clientId = -1;
	
	@Override
	public void initialize(ModelValidationEngine engine, MClient client) {
		// client = null for global validator
		if (client != null) {
			clientId = client.getAD_Client_ID();
			log.info(client.toString());
		} else {
			log.info("Initializing global validator: " + this.toString());
		}
		//	Add Persistence for IsDefault values
		engine.addDocValidate(I_C_Invoice.Table_Name, this);
	}
	
	@Override
	public int getAD_Client_ID() {
		return clientId;
	}

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
		log.info("AD_User_ID=" + AD_User_ID);
		return null;
	}
	
	@Override
	public String modelChange(PO entity, int type) throws Exception {
		return null;
	}

	@Override
	public String docValidate(PO entity, int timing) {
		if(timing == TIMING_BEFORE_COMPLETE) {
			if(entity.get_TableName().equals(I_C_Invoice.Table_Name)) {
				MInvoice creditMemo = (MInvoice) entity;
				MDocType documentType = MDocType.get(entity.getCtx(), creditMemo.getC_DocTypeTarget_ID());
				if(creditMemo.isSOTrx()
						&& creditMemo.isCreditMemo()
						&& !creditMemo.isReversal()
						&& creditMemo.get_ValueAsBoolean(LVEUtil.COLUMNNAME_IsFiscalDocument)
						&& !documentType.get_ValueAsBoolean(LVEUtil.LVE_AllowOverdraftReference)) {
					Map<Integer, BigDecimal> documentsToAllocate = new HashMap<>();
					Arrays.asList(creditMemo.getLines())
					.stream()
					.filter(creditMemoLine -> creditMemoLine.get_ValueAsInt(LVEUtil.COLUMNNAME_InvoiceToAllocate_ID) != 0)
					.forEach(creditMemoLine -> {
						MInvoice sourceInvoice = new MInvoice(creditMemoLine.getCtx(), creditMemoLine.get_ValueAsInt(LVEUtil.COLUMNNAME_InvoiceToAllocate_ID), creditMemoLine.get_TrxName());
						BigDecimal amountToAllocate = creditMemoLine.getLineNetAmt();
						//	Add Tax if exists
						if(creditMemoLine.getC_Tax_ID() > 0) {
							MTax tax = MTax.get(creditMemoLine.getCtx(), creditMemoLine.getC_Tax_ID());
							amountToAllocate = amountToAllocate.add(tax.calculateTax(amountToAllocate, creditMemoLine.isTaxIncluded(), creditMemoLine.getPrecision()));
						}
						//	Convert It
						amountToAllocate = MConversionRate.convert(creditMemo.getCtx(), amountToAllocate, creditMemo.getC_Currency_ID(), sourceInvoice.getC_Currency_ID(), creditMemo.getDateAcct(), creditMemo.getC_ConversionType_ID(), creditMemo.getAD_Client_ID(), creditMemo.getAD_Org_ID());
						if(documentsToAllocate.containsKey(sourceInvoice.getC_Invoice_ID())) {
							amountToAllocate = amountToAllocate.add(documentsToAllocate.get(sourceInvoice.getC_Invoice_ID()));
						}
						documentsToAllocate.put(sourceInvoice.getC_Invoice_ID(), amountToAllocate);
					});
					//	Validate
					StringBuffer message = new StringBuffer();
					documentsToAllocate.entrySet().forEach(entry -> {
						MInvoice sourceInvoice = new MInvoice(creditMemo.getCtx(), entry.getKey(), creditMemo.get_TrxName());
						BigDecimal openAmount = Optional.ofNullable(sourceInvoice.getOpenAmt()).orElse(Env.ZERO);
						if(openAmount.compareTo(entry.getValue()) < 0) {
							message.append(Msg.getMsg(creditMemo.getCtx(), "LVE.InvoiceOverdraft",
									new Object[] {
											sourceInvoice.getDocumentNo(),
											sourceInvoice.getGrandTotal(),
											openAmount,
											entry.getValue(),
											MCurrency.get(sourceInvoice.getCtx(), sourceInvoice.getC_Currency_ID()).getISO_Code(),
											openAmount.subtract(entry.getValue())
									}));
						}
					});
					if(message.length() > 0) {
						throw new AdempiereException(message.toString());
					}
				}
			}
		}
		return null;
	}
}
