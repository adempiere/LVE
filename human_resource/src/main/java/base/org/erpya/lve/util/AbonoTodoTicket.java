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
 * Copyright (C) 2003-2014 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Carlos Parada www.erpya.com                                *
 *****************************************************************************/
package org.erpya.lve.util;

import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.X_RV_HR_ProcessDetail;
import org.compiere.model.MBPartner;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.eevolution.hr.model.MHREmployee;
import org.spin.hr.util.AbstractPayrollReportExport;

/**
 * @author Carlos Parada, cparada@erpya.com, ERPCyA http://www.erpya.com
 * Export class for Abono Todo Ticket in payroll
 * Fixed-width format: Cedula(12) + Amount(21) + Date(8) = 41 chars per line
 */
public class AbonoTodoTicket extends AbstractPayrollReportExport {
	public AbonoTodoTicket(Properties ctx) {
		super(ctx);
	}

	/** Logger										*/
	static private CLogger	s_log = CLogger.getCLogger (AbonoTodoTicket.class);

	/**	Constant Payroll						*/
	private final String PAYROLL_CONSTANT= "AT";
	/**	Cedula Length							*/
	private final int CEDULA_LENGTH = 12;
	/**	Amount Length							*/
	private final int AMOUNT_LENGTH = 21;
	/**	Date Format								*/
	private SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("ddMMyyyy");
	/** Name File */
	private String fileName	= "Temp";


	@Override
	public boolean exportToFile(File file) {
		if(getDetail() == null || getDetail().isEmpty()) {
			return false;
		}
		Optional<X_RV_HR_ProcessDetail> processDetail = getDetail().stream().findFirst();
		try {
			//	Set new File Name
			Timestamp dateAcct = processDetail.get().getDateAcct();
			StringBuffer pathName = new StringBuffer();
			pathName
			    .append(PAYROLL_CONSTANT)
				.append("_")
				.append(DATE_FORMAT.format(dateAcct));
			fileName = pathName.toString();
			//	Get movements
			Map<Integer, List<X_RV_HR_ProcessDetail>> summaryMovements = getDetail().stream().collect(Collectors.groupingBy(X_RV_HR_ProcessDetail::getHR_Employee_ID));
			//	Delete if exists file
			Optional.ofNullable(file).ifPresent(fileToDelete -> fileToDelete.deleteOnExit());
			FileWriter fileWriter = new FileWriter(file);
			//	Write it
			AtomicBoolean first = new AtomicBoolean(true);
			for(Entry<Integer, List<X_RV_HR_ProcessDetail>> movement : summaryMovements.entrySet()) {
				int employeeId = movement.getKey();
				BigDecimal amount = movement.getValue().stream().map(x -> x.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
				//	Get employee cedula
				MHREmployee employee = new MHREmployee(Env.getCtx(), employeeId, null);
				MBPartner businessPartner = new MBPartner(Env.getCtx(), employee.getC_BPartner_ID(), null);
				String cedula = processNameAndValue(businessPartner.getValue());
				//	Build line: cedula(12) + amount(21) + date(8)
				StringBuffer line = new StringBuffer();
				if(first.get()) {
					first.set(false);
				} else {
					line.append("\r\n");
				}
				line
					.append(String.format("%-" + CEDULA_LENGTH + "s", cedula))
					.append(String.format("%0" + AMOUNT_LENGTH + "d", amount.longValue()))
					.append(DATE_FORMAT.format(dateAcct))
				;
				fileWriter.write(line.toString());
			}
			fileWriter.flush();
			fileWriter.close();
		} catch (Exception e) {
			s_log.log(Level.WARNING, "Exporting Abono Todo Ticket file ", e);
		}
		return true;
	}

	@Override
	public String getFileName() {
		return fileName;
	}

	private String processNameAndValue(String value) {
		if(Util.isEmpty(value)) {
			return value;
		}
		return value.replaceAll("[^a-zA-Z0-9á-źÁ-Ź ]", "").trim();
	}
}
