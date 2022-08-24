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
package org.erpya.lve.setup;

import java.util.Properties;

import org.compiere.model.I_C_Order;
import org.compiere.model.I_C_OrderLine;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.spin.model.I_WH_Setting;
import org.spin.model.MWHSetting;
import org.spin.model.MWHType;
import org.spin.util.ISetupDefinition;

/**
 * Add here your setup class
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class WithholdingPOSIVASetup implements ISetupDefinition {

	private static final String DESCRIPTION = "(*Created from Setup Automatically*)";
	private static final String UUID = "(*AutomaticSetup*)";
	
	private Properties context = null;
	private String transactionName = null;
	
	private Properties getCtx() {
		return context;
	}
	
	private String getTrx_Name() {
		return transactionName;
	}
	
	@Override
	public String doIt(Properties context, String transactionName) {
		//	Add Model Validator
		this.context = context;
		this.transactionName = transactionName;
		setupWithholding();
		return "@AD_SetupDefinition_ID@ @Ok@";
	}
	
	/**
	 * Create Setting for Withholding Additional Tax
	 * @return
	 */
	private void setupWithholding() {
		String whereClause = "EXISTS (SELECT 1 FROM WH_Setting s WHERE WH_Type.WH_Type_ID = s.WH_Type_ID AND ".concat(MWHSetting.COLUMNNAME_WithholdingClassName).concat(" = ?)");
		MWHType withHolgingType = new Query(getCtx(), MWHType.Table_Name, whereClause, getTrx_Name())
				.setParameters(org.erpya.lve.util.APInvoiceIVA.class.getName())
				.setClient_ID()
				.<MWHType>first();
		//	Validate
		if(withHolgingType == null
				|| withHolgingType.getWH_Type_ID() <= 0) {
			return;
		}
		int maxSequence = DB.getSQLValue(getTrx_Name(), "SELECT MAX(SeqNo) FROM WH_Setting WHERE WH_Type_ID = ?", withHolgingType.getWH_Type_ID());
		if(maxSequence < 0) {
			maxSequence = 10;
		} else {
			maxSequence += 10;
		}
		//	Order Line Create
		if(createSettingWithEvent(withHolgingType.getWH_Type_ID(), org.erpya.lve.util.POSOrderIVANew.class.getName(), MWHSetting.EVENTMODELVALIDATOR_TableAfterNew, I_C_OrderLine.Table_ID, "IVA-Linea-Orden-Crear", "Retención I.V.A Después de Crear Linea de Orden", maxSequence)) {
			maxSequence += 10;
		}
		//	Order Line Update
		if(createSettingWithEvent(withHolgingType.getWH_Type_ID(), org.erpya.lve.util.POSOrderIVAChange.class.getName(), MWHSetting.EVENTMODELVALIDATOR_TableAfterChange, I_C_OrderLine.Table_ID, "IVA-Linea-Orden-Modificar", "Retención I.V.A Después de Modificar Linea de Orden", maxSequence)) {
			maxSequence += 10;
		}
		//	Order Line Delete
		if(createSettingWithEvent(withHolgingType.getWH_Type_ID(), org.erpya.lve.util.POSOrderIVAChange.class.getName(), MWHSetting.EVENTMODELVALIDATOR_TableAfterDelete, I_C_OrderLine.Table_ID, "IVA-Linea-Orden-Eliminar", "Retención I.V.A Después de Eliminar Linea de Orden", maxSequence)) {
			maxSequence += 10;
		}
		//	Order New
		if(createSettingWithEvent(withHolgingType.getWH_Type_ID(), org.erpya.lve.util.POSOrderIVANew.class.getName(), MWHSetting.EVENTMODELVALIDATOR_TableBeforeNew, I_C_Order.Table_ID, "IVA-Orden-Crear", "Retención I.V.A Antes de Crear Orden de Venta", maxSequence)) {
			maxSequence += 10;
		}
		//	Order Change
		if(createSettingWithEvent(withHolgingType.getWH_Type_ID(), org.erpya.lve.util.POSOrderIVAChange.class.getName(), MWHSetting.EVENTMODELVALIDATOR_TableBeforeChange, I_C_Order.Table_ID, "IVA-Orden-Modificar", "Retención I.V.A Antes de odificar Orden de Venta", maxSequence)) {
			maxSequence += 10;
		}
	}
	
	/**
	 * Find a create a setting if not exists
	 * @param withholdingTypeId
	 * @param className
	 * @param eventType
	 * @param tableId
	 * @param value
	 * @param name
	 * @param sequence
	 * @return true if was created and false if already exist
	 */
	private boolean createSettingWithEvent(int withholdingTypeId, String className, String eventType, int tableId, String value, String name, int sequence) {
		MWHSetting setting = new Query(getCtx(), I_WH_Setting.Table_Name, "EventType = 'E' "
				+ "AND WH_Type_ID = ? "
				+ "AND EventModelValidator = ? "
				+ "AND WithholdingClassName = ?", getTrx_Name())
				.setParameters(withholdingTypeId, eventType, className)
				.first();
		if(setting != null) {
			return false;
		}
		setting = new MWHSetting(getCtx(), 0, getTrx_Name());
		setting.setWH_Type_ID(withholdingTypeId);
		setting.setValue(value);
		setting.setName(name);
		setting.setSeqNo(sequence);
		setting.setEventType(MWHSetting.EVENTTYPE_Event);
		setting.setAD_Table_ID(tableId);
		setting.setEventModelValidator(eventType);
		setting.setDescription(DESCRIPTION);
		setting.setWithholdingClassName(className);
		setting.setUUID(UUID);
		setting.setIsDirectLoad(true);
		setting.saveEx();
		return true;
	}
}
