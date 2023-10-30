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
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.compiere.model.MBPartner;
import org.compiere.model.MOrgInfo;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.eevolution.hr.model.MHREmployee;
import org.adempiere.core.domains.models.X_RV_HR_ProcessDetail;
import org.spin.hr.util.AbstractPayrollReportExport;

/**
 * @author <a href="mailto:yamelsenih@gmail.com">Yamel Senih</a>
 * Export class for BANAVIH in payroll
 */
public class HR_BANAVIH extends AbstractPayrollReportExport {
	public HR_BANAVIH(Properties ctx) {
		super(ctx);
	}

	/** Logger										*/
	static private CLogger	s_log = CLogger.getCLogger (HR_BANAVIH.class);
	/** BPartner Info Index for Nationality	    	*/
	private static final int     BP_NATIONALITY 	= 0;
	/** BPartner Info Index for Tax ID		    	*/
	private static final int     BP_TAX_ID 			= 1;
	/** BPartner Info Index for First Name 1    	*/
	private static final int     BP_FIRST_NAME_1 	= 2;
	/** BPartner Info Index for First Name 1    	*/
	private static final int     BP_FIRST_NAME_2 	= 3;
	/** BPartner Info Index for First Name 1    	*/
	private static final int     BP_LAST_NAME_1 	= 4;
	/** BPartner Info Index for First Name 1    	*/
	private static final int     BP_LAST_NAME_2 	= 5;
	/** BPartner Info Index for Employee Start Date	*/
	private static final int     EM_START_DATE 		= 6;
	/** BPartner Info Index for Employee End Date	*/
	private static final int     EM_END_DATE 		= 7;
	
	/**	Constant Payroll						*/
	private final String		PAYROLL_CONSTANT= "N";
	/**	Separator								*/
	private final String 		SEPARATOR 		= ",";
	/**	Date Format								*/
	private SimpleDateFormat 	dateFormat 		= null;
	/**	Current Amount							*/
	private BigDecimal 			currentAmount	= null;
	/**	Current Process Report Line				*/
	private X_RV_HR_ProcessDetail 	currentProcessDetail = null;
	/**	File Writer								*/
	private FileWriter 				fileWriter	= null;
	/**	Number Lines							*/
	private int 					lines 	= 0;
	/** Name File								*/
	private String 					fileName	= "Temp"; 
	//	Number Format
	private final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("000000000.00");
	
	
	@Override
	public boolean exportToFile(File file) {
		//	Date Format
		dateFormat = new SimpleDateFormat("ddMMyyyy");
		//	Current Business Partner
		int currentBusinessPartnerId = 0;
		if (getDetail() == null || getDetail().isEmpty())
			return false;
		Optional<X_RV_HR_ProcessDetail> processDetail = getDetail().stream().findFirst();
		try {
			//	Set new File Name
			StringBuffer pathName = new StringBuffer();
			pathName
			    .append(PAYROLL_CONSTANT)
				//	Payroll Account
				.append(MOrgInfo.get(getCtx(), processDetail.get().getAD_Org_ID(),processDetail.get().get_TrxName()).get_ValueAsString(LVEUtil.COLUMNNAME_BANAVIHCode))
				//	Accounting Date in format MM YYYY
				.append(new SimpleDateFormat("MMyyyy").format(processDetail.get().getDateAcct()));
			fileName = pathName.toString();
		} catch (Exception e) {
			s_log.log(Level.WARNING, "Could not delete ", e);
		}
		try {
			//	
			Optional.ofNullable(file).ifPresent(fileToDelete -> fileToDelete.deleteOnExit());
			fileWriter = new FileWriter(file);
			//  write header
			lines ++;
			//  write lines
			Map<Integer, List<X_RV_HR_ProcessDetail>> det = getDetail().stream().collect(Collectors.groupingBy(X_RV_HR_ProcessDetail::getC_BPartner_ID));
			
			for (Map.Entry<Integer, List<X_RV_HR_ProcessDetail>> detail : det.entrySet()) {
				if (detail == null)
					continue;
				//	Verify Current Business Partner and Month
				if(currentBusinessPartnerId != detail.getKey()) {
					writeLine();
					currentProcessDetail = detail.getValue().get(0);
					currentBusinessPartnerId = detail.getKey();
					currentAmount = detail.getValue().stream().map(x -> x.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
				} 
			}   
			//  write last line
			writeLine();
			//	Close
			fileWriter.flush();
			fileWriter.close();
			
		} catch (Exception e) {
			//err.append(e.toString());
			s_log.log(Level.SEVERE, "", e);
			return false;
		}
		//
		return true;
	}
	
	/**
	 * Write Line
	 * @author <a href="mailto:yamelsenih@gmail.com">Yamel Senih</a> 8/12/2014, 15:08:36
	 * @return void
	 * @throws IOException 
	 */
	private void writeLine() throws IOException {
		//	Valid Null Value
		if(currentProcessDetail == null)
			return;
		//	Process Business Partner
		Map<Integer,String> bpInfo = processBPartner(currentProcessDetail.getC_BPartner_ID(), currentProcessDetail.getDateAcct(), currentProcessDetail.get_TrxName());
		//	Line
		if(bpInfo == null)
			return;
		StringBuffer line = new StringBuffer();
		//	Amount
		if(currentAmount == null)
			currentAmount = Env.ZERO;
		
		String currentAmountAsString = DECIMAL_FORMAT.format(currentAmount);
		
		//	New Line
		if(lines > 1)
			line.append(Env.NL);
		//	Nationality
		line.append(bpInfo.get(BP_NATIONALITY))
			.append(SEPARATOR)
			//	Tax ID
			.append(bpInfo.get(BP_TAX_ID))
			.append(SEPARATOR)
			//	First Name 1
			.append(bpInfo.get(BP_FIRST_NAME_1))
			.append(SEPARATOR)
			//	First Name 2
			.append(bpInfo.get(BP_FIRST_NAME_2))
			.append(SEPARATOR)
			//	Last Name 1
			.append(bpInfo.get(BP_LAST_NAME_1))
			.append(SEPARATOR)
			//	Last Name 2
			.append(bpInfo.get(BP_LAST_NAME_2))
			.append(SEPARATOR)
			//	Amount
			.append(currentAmountAsString.replace(",", ".").replace(".", ""))
			.append(SEPARATOR)
			//	Employee Start Date
			.append(bpInfo.get(EM_START_DATE))
			.append(SEPARATOR)
			//	Employee End Date
			.append(bpInfo.get(EM_END_DATE));
		//	Write Line
		fileWriter.write(line.toString());
		lines ++;
	}
	
	/**
	 * Process Business Partner
	 * @param businessPartnerId
	 * @param dateAcct
	 * @param transactionName
	 * @return String []
	 */
	private Map<Integer,String>  processBPartner(int businessPartnerId, Timestamp dateAcct, String transactionName) {
		Map<Integer,String> bpInfo = new HashMap<Integer, String>();

		//	Get Business Partner
		MBPartner bpartner = MBPartner.get(Env.getCtx(), businessPartnerId);
		//	Get Name
		String name = bpartner.getName();
		String name2 = bpartner.getName2();
		//	Valid Null
		if(Util.isEmpty(name)) {
			name = "";
		}
		if(Util.isEmpty(name2)) {
			name2 = "";
		}
		
		List <String> nameList = Arrays.asList(name.split(" "));
		//	Extract First Name 1
		String firstName1 = "";
		String firstName2  = "";
		boolean isName = false;

		for (String firstName : nameList) {
			if(isName) {
				firstName2 += firstName + " ";
			}
			if(firstName.length() < 3 && !isName)
				firstName1 += firstName +  " ";
			else if(firstName.length() > 2 && !isName) {
				firstName1 += firstName;
				isName = true;
			}
				
		} 
		
		//	End Index for Last Name
		List <String> lastNameList = Arrays.asList(name2.split(" "));
		//	Extract First Name 1
		String lastName1 = "";
		String lastName2  = "";
		isName = false;
		//	Extract First Name 2
		for (String lastName : lastNameList) {
			if(isName) {
				lastName2 += lastName + " ";
			}
			if(lastName.length() < 3 && !isName)
			    lastName1 += lastName + " ";
			else if(lastName.length() > 2 && !isName) {
				lastName1 += lastName;
				isName = true;
			}
		} 
		
		//	Valid length
		if(firstName1.length() > 25)
			firstName1 = firstName1.substring(0, 24);
		else if(firstName1.length() == 0)
			firstName1 = "";
		if(firstName2.length() > 25)
			firstName2 = firstName2.substring(0, 24);
		else if(firstName2.length() == 0)
			firstName2 = "";
		if(lastName1.length() > 25)
			lastName1 = lastName1.substring(0, 24);
		else if(lastName1.length() == 0)
			lastName1 = "";
		if(lastName2.length() > 25)
			lastName2 = lastName2.substring(0, 24);
		else if(lastName2.length() == 0)
			lastName2 = "";
		
		//	Get Active Employee
		MHREmployee employee = getByPartnerIdAndStartDate(bpartner.getC_BPartner_ID(), dateAcct, transactionName);
		//	Valid Employee
		if(employee == null)
			return null;
		//	Get Start Date
		String startDate = dateFormat.format(employee.getStartDate());
		String endDate = "";
		//	Get End Date
		if(employee.getEndDate() != null)
			endDate = dateFormat.format(employee.getEndDate());

		String bPTaxId = bpartner.getValue();
		String personType = bPTaxId.substring(0, 1);
		bPTaxId = bPTaxId.replaceAll("\\D+","");
		//	Set Array
		bpInfo.put(BP_NATIONALITY, personType);
		bpInfo.put(BP_TAX_ID, bPTaxId);
		bpInfo.put(BP_FIRST_NAME_1, processValue(firstName1));
		bpInfo.put(BP_FIRST_NAME_2, processValue(firstName2));
		bpInfo.put(BP_LAST_NAME_1, processValue(lastName1));
		bpInfo.put(BP_LAST_NAME_2, processValue(lastName2));
		bpInfo.put(EM_START_DATE,startDate);
		bpInfo.put(EM_END_DATE,endDate);
		//	Return
		return bpInfo;
	}
	
	/**
	 * Get Employee by Partner Id and Date Start
	 * @param ctx
	 * @param partnerId
	 * @param dateStart
	 * @param trxName
	 * @return
	 */
	private MHREmployee getByPartnerIdAndStartDate(int partnerId , Timestamp dateStart, String trxName) {
		StringBuilder whereClause = new StringBuilder();
		whereClause.append(MHREmployee.COLUMNNAME_C_BPartner_ID).append("=? AND ");
		whereClause.append(MHREmployee.COLUMNNAME_StartDate).append(" <= ?");
		return new Query(Env.getCtx(), MHREmployee.Table_Name , whereClause.toString(),trxName)
				.setClient_ID()
				.setParameters(partnerId , dateStart)
				.setOrderBy(MHREmployee.COLUMNNAME_StartDate + " DESC, " + MHREmployee.COLUMNNAME_EndDate + " DESC")
				.first();
	}
	
	public String processValue(String value) {
		if(Util.isEmpty(value)) {
			return value;
		}
		//	
		return value.replaceAll("[+^:&áàäéèëíìïóòöúùñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ$,;*/áéíóúÁÉÍÓÚñÑ¿¡]", "").trim();
	}
	
	@Override
	public String getFileName() {
		return fileName;
	}
}

