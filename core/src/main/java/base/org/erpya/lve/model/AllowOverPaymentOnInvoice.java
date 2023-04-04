/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
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
 * Contributor(s): Carlos Parada www.erpya.com                                *
 *****************************************************************************/
package org.erpya.lve.model;

import java.util.Optional;

import org.compiere.model.MClient;
import org.compiere.model.MPayment;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.erpya.lve.util.LVEUtil;

/**
 * Model Validator for Allow Overpayment Invoice
 * @author Carlos Parada, cparada@erpya.com, ERPCyA http://www.erpya.com
 */
public class AllowOverPaymentOnInvoice implements ModelValidator{

	/** Logger */
	private static CLogger log = CLogger
			.getCLogger(AllowOverPaymentOnInvoice.class);
	
	/** Client */
	private int clientId = -1;
	
	@Override
	public String docValidate(PO po, int timing) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getAD_Client_ID() {
		return clientId;
	}

	@Override
	public void initialize(ModelValidationEngine engine, MClient client){
		// client = null for global validator
		if (client != null) {
			clientId = client.getAD_Client_ID();
			log.info(client.toString());
		} else {
			log.info("Initializing global validator: " + this.toString());
		}
		
		engine.addModelChange(MPayment.Table_Name, this);
	}

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
		log.info("AD_User_ID=" + AD_User_ID);
		return null;
	}

	@Override
	public String modelChange(PO po, int type) throws Exception {
		Optional<PO> maybeDocument = Optional.ofNullable(po);
		maybeDocument.ifPresent(document ->{
			if (type == TYPE_BEFORE_NEW
					|| type == TYPE_BEFORE_CHANGE) {
				if (document.get_TableName().equals(MPayment.Table_Name)
						&& document.is_ValueChanged(LVEUtil.COLUMNNAME_LVE_AllowOverPayInvoice)
							&& document.get_ValueAsBoolean(LVEUtil.COLUMNNAME_LVE_AllowOverPayInvoice)
								&& document.get_ValueAsInt(MPayment.COLUMNNAME_C_Invoice_ID) > 0) {
					MPayment payment = (MPayment) document;
					payment.setIsOverUnderPayment(false);
					payment.setOverUnderAmt(Env.ZERO);
					payment.setWriteOffAmt(Env.ZERO);
				}
			}
		});
		return null;
	}

}
