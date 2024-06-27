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
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package org.erpya.lve.util;

import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
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
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MLocation;
import org.compiere.model.MOrgInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.util.Util;
import org.eevolution.hr.model.MHREmployee;
import org.spin.hr.util.AbstractPayrollReportExport;
import org.spin.pr.model.MHRProcessReport;

/**
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 * Export class for Cesta Ticket in payroll
 */
public class CestaTicket extends AbstractPayrollReportExport {
	public CestaTicket(Properties ctx) {
		super(ctx);
	}

	/** Logger										*/
	static private CLogger	s_log = CLogger.getCLogger (CestaTicket.class);
	
	/**	Constant Payroll						*/
	private final String PAYROLL_CONSTANT= "CT";
	/**	Separator								*/
	private final String SEPARATOR = ";";
	/**	Date Format								*/
	private SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
	/**	Decimal Format								*/
	private DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#############.00");
	/** Name File */
	private String fileName	= "Temp";
	
	
	@Override
	public boolean exportToFile(File file) {
		//	Current Business Partner
		if(getDetail() == null || getDetail().isEmpty()) {
			return false;
		}
		Optional<X_RV_HR_ProcessDetail> processDetail = getDetail().stream().findFirst();
		try {
			//	Set new File Name
			StringBuffer pathName = new StringBuffer();
			MHRProcessReport processReport = MHRProcessReport.get(getCtx(), processDetail.get().getHR_ProcessReport_ID());
			MOrgInfo organizationInfo = MOrgInfo.get(getCtx(), processDetail.get().getAD_Org_ID(), null);
			String cestaTicketCode = organizationInfo.get_ValueAsString(LVEUtil.LVE_CestaTicketCode);
			String productCategoryCode = Optional.ofNullable(processReport.get_ValueAsString(LVEUtil.LVE_CT_ProductCategoryCode)).orElse("");
			String productCode = Optional.ofNullable(processReport.get_ValueAsString(LVEUtil.LVE_CT_ProductCode)).orElse("");
			String deliveryPoint = Optional.ofNullable(processReport.get_ValueAsString(LVEUtil.LVE_CT_DeliveryPoint)).orElse("");
			Timestamp dateAcct = processDetail.get().getDateAcct();
			pathName
			    .append(PAYROLL_CONSTANT)
				//	Separator
				.append("_")
				//	Payroll Account
				.append(cestaTicketCode)
				//	Separator
				.append("_")
				//	Product Code
				.append(productCode)
				//	Separator
				.append("_")
				//	Accounting Date in format dd MM yyyy
				.append(new SimpleDateFormat("ddMMyyyy").format(dateAcct));
			fileName = pathName.toString();
			//	Get movements
			Map<Integer, List<X_RV_HR_ProcessDetail>> summaryMovements = getDetail().stream().collect(Collectors.groupingBy(X_RV_HR_ProcessDetail::getHR_Employee_ID));
			//	Delete if exists file
			Optional.ofNullable(file).ifPresent(fileToDelete -> fileToDelete.deleteOnExit());
			FileWriter fileWriter = new FileWriter(file);
			//	Write it
			AtomicBoolean first = new AtomicBoolean(true);
			for(Entry<Integer, List<X_RV_HR_ProcessDetail>> movement : summaryMovements.entrySet()) {
				int empoloyeeId = movement.getKey();
				BigDecimal amount = movement.getValue().stream().map(x -> x.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
				Employee employee = Employee.newInstance(empoloyeeId);
				StringBuffer line = new StringBuffer();
				if(first.get()) {
					first.set(false);
				} else {
					line.append(Env.NL);
				}
				line
					.append(employee.getCode())
					.append(SEPARATOR)
					.append(employee.getName())
					.append(SEPARATOR)
					.append(employee.getLastName())
					.append(SEPARATOR)
					.append(DATE_FORMAT.format(employee.getBirthday()))
					.append(SEPARATOR)
					.append(deliveryPoint)
					.append(SEPARATOR)
					.append(cestaTicketCode)
					.append(SEPARATOR)
					.append(productCode)
					.append(SEPARATOR)
					.append(SEPARATOR)
					.append(DECIMAL_FORMAT.format(amount).replace(".", ","))	//	Amount of the Load
					.append(SEPARATOR)
					.append(DATE_FORMAT.format(dateAcct))
					.append(SEPARATOR)
					.append(employee.getGender())
					.append(SEPARATOR)
					.append(employee.getMaritalStatus())
					.append(SEPARATOR)
					.append(employee.getEmail())
					.append(SEPARATOR)
					.append(employee.getPhone())
					.append(SEPARATOR)
					//	Country code "1" for Venezuela
					.append(1)
					.append(SEPARATOR)
					.append(employee.getPlaceOfBirth())
					.append(SEPARATOR)
					.append(SEPARATOR)
					.append(productCategoryCode)
					.append(SEPARATOR)
					.append(SEPARATOR)
				;
				fileWriter.write(line.toString());
			}
			fileWriter.flush();
			fileWriter.close();
		} catch (Exception e) {
			s_log.log(Level.WARNING, "Loading file name ", e);
		}
		return true;
	}
	
	@Override
	public String getFileName() {
		return fileName;
	}
	
	private static class Employee {
		private String code;
		private String name;
		private String lastName;
		private Timestamp birthday;
		private String gender;
		private String maritalStatus;
		private String email;
		private String phone;
		private String placeOfBirth;
		private int businessPartnerId;
		
		private Employee(int employeeId) {
			MHREmployee employee = new MHREmployee(Env.getCtx(), employeeId, null);
			MBPartner businessPartner = new MBPartner(Env.getCtx(), employee.getC_BPartner_ID(), null);
			businessPartnerId = businessPartner.getC_BPartner_ID(); 
			code = processNameAndValue(businessPartner.getValue());
			name = getFirstOnly(processNameAndValue(Optional.ofNullable(businessPartner.getName()).orElse("").trim()));
			lastName = getFirstOnly(processNameAndValue(Optional.ofNullable(businessPartner.getName2()).orElse("").trim()));
			birthday = businessPartner.getBirthday();
			if(birthday == null) {
				birthday = TimeUtil.getDay(System.currentTimeMillis());
			}
			gender = Optional.ofNullable(businessPartner.getGender()).orElse("").trim();
			if(Util.isEmpty(gender)) {
				gender = MBPartner.GENDER_Male;
			}
			maritalStatus = Optional.ofNullable(businessPartner.getMaritalStatus()).orElse("").trim();
			if(Util.isEmpty(maritalStatus)) {
				maritalStatus = MBPartner.MARITALSTATUS_Single;
			}
			if(maritalStatus.equals(MBPartner.MARITALSTATUS_Single)) {
				maritalStatus = "S";
			} else if(maritalStatus.equals(MBPartner.MARITALSTATUS_Live_In)) {
				maritalStatus = "C";
			} else if(maritalStatus.equals(MBPartner.MARITALSTATUS_Married)) {
				maritalStatus = "C";
			} else if(maritalStatus.equals(MBPartner.MARITALSTATUS_Divorced)) {
				maritalStatus = "D";
			} else if(maritalStatus.equals(MBPartner.MARITALSTATUS_Widow) || maritalStatus.equals(MBPartner.MARITALSTATUS_Windower)) {
				maritalStatus = "V";
			}
			if(businessPartner.getPlaceOfBirth_ID() > 0) {
				MLocation location = MLocation.get(businessPartner.getCtx(), businessPartner.getPlaceOfBirth_ID(), null);
				if(location != null) {
					placeOfBirth = location.getCity();
				}
			}
			if(Util.isEmpty(placeOfBirth)) {
				placeOfBirth = "";
			}
			fillContactValues();
		}
		
		private String getFirstOnly(String value) {
			if(Util.isEmpty(value)) {
				return "";
			}
			String[] split = value.split(" ");
			if(split.length > 0) {
				return split[0];
			}
			return "";
		}
		
		private void fillContactValues() {
			String sql = "SELECT COALESCE(bpl.Phone, bpl.Phone2, usr.Phone, usr.Phone2, '') As Phone,"
					+ " COALESCE(bpl.EMail, usr.EMail, '') AS EMail"
					+ " FROM C_BPartner bp"
					+ " LEFT JOIN C_BPartner_Location bpl ON (bp.C_BPartner_ID = bpl.C_BPartner_ID)"
					+ " LEFT JOIN AD_User usr ON (bp.C_BPartner_ID = usr.C_BPartner_ID)"
					+ " WHERE bp.C_BPartner_ID = ? "
					+ "	AND bp.IsEmployee = 'Y'";
			List<Object> parameters = List.of(businessPartnerId);
			DB.runResultSet(null, sql, parameters, resultSet -> {
				if(resultSet.next()) {
					phone = processContact(resultSet.getString("Phone"));
					email = processContact(resultSet.getString("EMail"));
				}
			}).onFailure(throwable -> {
				throw new AdempiereException(throwable);
			});
		}
		
		public static Employee newInstance(int businessPartnerId) {
			return new Employee(businessPartnerId);
		}
		
		public String processNameAndValue(String value) {
			if(Util.isEmpty(value)) {
				return value;
			}
			return value.replaceAll("[^a-zA-Z0-9á-źÁ-Ź ]", "").trim();
		}
		
		public String processContact(String value) {
			if(Util.isEmpty(value)) {
				return value;
			}
			return value.replaceAll("[^a-zA-Z0-9-.]", "").trim();
		}

		public String getCode() {
			return code;
		}

		public String getName() {
			return name;
		}

		public String getLastName() {
			return lastName;
		}

		public Timestamp getBirthday() {
			return birthday;
		}

		public String getGender() {
			return gender;
		}

		public String getMaritalStatus() {
			return maritalStatus;
		}

		public String getEmail() {
			return email;
		}

		public String getPhone() {
			return phone;
		}

		public String getPlaceOfBirth() {
			return placeOfBirth;
		}
	}
}

