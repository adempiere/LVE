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

import org.compiere.model.I_C_Invoice;
import org.compiere.model.I_C_Order;
import org.compiere.model.I_C_OrderLine;
import org.compiere.model.I_C_POS;
import org.compiere.model.MPOS;
import org.compiere.model.MPayment;
import org.compiere.model.MTable;
import org.compiere.model.PO;
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
		if(createSettingWithEvent(withHolgingType.getWH_Type_ID(), org.erpya.lve.util.POSOrderIVANew.class.getName(), MWHSetting.EVENTMODELVALIDATOR_TableAfterNew, I_C_Order.Table_ID, "IVA-Orden-Crear", "Retención I.V.A Antes de Crear Orden de Venta", maxSequence)) {
			maxSequence += 10;
		}
		//	Order Change
		if(createSettingWithEvent(withHolgingType.getWH_Type_ID(), org.erpya.lve.util.POSOrderIVAChange.class.getName(), MWHSetting.EVENTMODELVALIDATOR_TableAfterChange, I_C_Order.Table_ID, "IVA-Orden-Modificar", "Retención I.V.A Antes de odificar Orden de Venta", maxSequence)) {
			maxSequence += 10;
		}
		//	Order Process
		if(createSettingWithEvent(withHolgingType.getWH_Type_ID(),org.erpya.lve.util.POSOrderIVAProcess.class.getName(), MWHSetting.EVENTMODELVALIDATOR_DocumentAfterComplete, I_C_Order.Table_ID, "IVA-Orden-Procesar", "Retención I.V.A Después de Procesar Orden de Venta", maxSequence)) {
			maxSequence += 10;
		}
		//	After process withholding document
		if(createSettingWithEvent(withHolgingType.getWH_Type_ID(),org.erpya.lve.util.POSWihholdingIVAProcess.class.getName(), MWHSetting.EVENTMODELVALIDATOR_DocumentBeforeComplete, I_C_Invoice.Table_ID, "IVA-Comprobante-Procesar", "Retención I.V.A Antes de Procesar Comprobante de Retención", maxSequence)) {
			maxSequence += 10;
		}
		//	Payment Method
		createPaymentMethod(withHolgingType.getWH_Type_ID());
	}
	
	/**
	 * Create Payment Method for POS
	 */
	private void createPaymentMethod(int withholdingTypeId) {
		PO paymentMethod = new Query(getCtx(), "C_PaymentMethod", "TenderType = 'M' "
				+ "AND Value = ?", getTrx_Name())
				.setParameters("Retencion-IVA")
				.first();
		if(paymentMethod == null
				|| paymentMethod.get_ID() <= 0) {
			paymentMethod = MTable.get(context, "C_PaymentMethod").getPO(0, getTrx_Name());
			paymentMethod.setAD_Org_ID(0);
			paymentMethod.set_ValueOfColumn("TenderType", MPayment.TENDERTYPE_CreditMemo);
			paymentMethod.set_ValueOfColumn("Value", "Retencion-IVA");
			paymentMethod.set_ValueOfColumn("Name", "Retencion de I.V.A.");
			paymentMethod.set_ValueOfColumn("Description", "I.V.A. sobre");
			paymentMethod.set_ValueOfColumn("WH_Type_ID", withholdingTypeId);
			paymentMethod.saveEx();
		}
		PO payPaymentMethod = new Query(getCtx(), "C_PaymentMethod", "TenderType = 'M' "
				+ "AND Value = ?", getTrx_Name())
				.setParameters("Pago-IVA")
				.first();
		if(payPaymentMethod == null
				|| payPaymentMethod.get_ID() <= 0) {
			payPaymentMethod = MTable.get(context, "C_PaymentMethod").getPO(0, getTrx_Name());
			payPaymentMethod.setAD_Org_ID(0);
			payPaymentMethod.set_ValueOfColumn("TenderType", MPayment.TENDERTYPE_CreditMemo);
			payPaymentMethod.set_ValueOfColumn("Value", "Pago-IVA");
			payPaymentMethod.set_ValueOfColumn("Name", "Pago de I.V.A.");
			payPaymentMethod.set_ValueOfColumn("Description", "Pago de I.V.A.");
			payPaymentMethod.set_ValueOfColumn("WH_Type_ID", withholdingTypeId);
			payPaymentMethod.set_ValueOfColumn("IsWithholdingExempt", true);
			payPaymentMethod.saveEx();
		}
		//	
		if(MTable.getTable_ID("C_POSPaymentTypeAllocation") <= 0) {
			return;
		}
		int paymentMethodId = paymentMethod.get_ID();
		int payPaymentMethodId = payPaymentMethod.get_ID();
		//	Add for All POS
		new Query(getCtx(), I_C_POS.Table_Name, null, getTrx_Name())
			.setOnlyActiveRecords(true)
			.setClient_ID()
			.getIDsAsList()
			.forEach(posId -> {
				MPOS pos = MPOS.get(getCtx(), posId);
				PO allocatedPaymentMethod = new Query(getCtx(), "C_POSPaymentTypeAllocation", "C_POS_ID = ? AND C_PaymentMethod_ID = ?" ,getTrx_Name())
						.setParameters(posId, paymentMethodId)
						.first();
				if(allocatedPaymentMethod == null
						|| allocatedPaymentMethod.get_ID() <= 0) {
					allocatedPaymentMethod = MTable.get(getCtx(), "C_POSPaymentTypeAllocation").getPO(0, getTrx_Name());
					allocatedPaymentMethod.setAD_Org_ID(pos.getAD_Org_ID());
					allocatedPaymentMethod.set_ValueOfColumn("C_POS_ID", posId);
					allocatedPaymentMethod.set_ValueOfColumn("C_PaymentMethod_ID", paymentMethodId);
					allocatedPaymentMethod.set_ValueOfColumn("IsPaymentReference", true);
					allocatedPaymentMethod.set_ValueOfColumn("IsDisplayedFromCollection", false);
					allocatedPaymentMethod.set_ValueOfColumn("SeqNo", 999);
					allocatedPaymentMethod.saveEx();
				}
				//	For payment
				PO allocatedPayPaymentMethod = new Query(getCtx(), "C_POSPaymentTypeAllocation", "C_POS_ID = ? AND C_PaymentMethod_ID = ?" ,getTrx_Name())
						.setParameters(posId, payPaymentMethodId)
						.first();
				if(allocatedPayPaymentMethod == null
						|| allocatedPayPaymentMethod.get_ID() <= 0) {
					allocatedPayPaymentMethod = MTable.get(getCtx(), "C_POSPaymentTypeAllocation").getPO(0, getTrx_Name());
					allocatedPayPaymentMethod.setAD_Org_ID(pos.getAD_Org_ID());
					allocatedPayPaymentMethod.set_ValueOfColumn("C_POS_ID", posId);
					allocatedPayPaymentMethod.set_ValueOfColumn("Name", "Comprobante de I.V.A.");
					allocatedPayPaymentMethod.set_ValueOfColumn("C_PaymentMethod_ID", payPaymentMethodId);
					allocatedPayPaymentMethod.set_ValueOfColumn("IsPaymentReference", false);
					allocatedPayPaymentMethod.set_ValueOfColumn("IsDisplayedFromCollection", true);
					allocatedPayPaymentMethod.set_ValueOfColumn("SeqNo", 999);
					allocatedPayPaymentMethod.saveEx();
				}
			});
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
				+ "AND WithholdingClassName = ? "
				+ "AND AD_Table_ID = ?", getTrx_Name())
				.setParameters(withholdingTypeId, eventType, className, tableId)
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
