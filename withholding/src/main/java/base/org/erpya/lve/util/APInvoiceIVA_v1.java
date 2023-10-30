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
package org.erpya.lve.util;

import org.adempiere.core.domains.models.I_C_Order;
import org.spin.model.MWHSetting;

/**
 * 	Implementación de retención de I.V.A para la localización de Venezuela
 * 	Esto puede aplicar para Documentos por Pagar y Notas de Crédito de Documentos por Pagar
 * 	Para los documentos que vengan del POS no aplica esta funcionalidad
 * 	Note que la validación de las 20 UT solo aplica para documentos por pagar
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *  @contributor Carlos Parada, cparada@erpya.com, ERPCyA http://www.erpya.com
 */
public class APInvoiceIVA_v1 extends APInvoiceIVA {

	public APInvoiceIVA_v1(MWHSetting setting) {
		super(setting);
	}

	@Override
	public boolean isValid() {
		//	Validate POS
		if(getDocument().get_ValueAsInt(I_C_Order.COLUMNNAME_C_POS_ID) > 0) {
			return false;
		}
		return super.isValid();
	}

	@Override
	public String run() {
		return super.run();
	}
}