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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.compiere.model.MBPartner;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.eevolution.model.MHREmployee;
import org.spin.model.X_RV_HR_ProcessDetail;
import org.spin.util.AbstractPayrollReportExport;

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
	private final String		PAYROLL_CONSTANT	= "N";
	/**	Separator								*/
	private final String 		SEPARATOR 			= ",";
	/**	Date Format								*/
	private SimpleDateFormat 	m_DateFormat 		= null;
	/**	Current Amount							*/
	private BigDecimal 			m_CurrentAmt		= null;
	/**	Current Process Report Line				*/
	private X_RV_HR_ProcessDetail 	m_Current_Pdl 	= null;
	/**	File Writer								*/
	private FileWriter 				m_FileWriter	= null;
	/**	Number Lines							*/
	private int 					m_NoLines 		= 0;
	/** Name File								*/
	private String 					m_NameFile		= "Temp"; 
	
	
	@Override
	public boolean exportToFile(File file) {
		//	Date Format
		m_DateFormat = new SimpleDateFormat("ddMMyyyy");
		//	Current Business Partner
		int m_Current_BPartner_ID = 0;
		if (getDetail() == null || getDetail().isEmpty())
			return false;
		X_RV_HR_ProcessDetail pdl = getDetail().get(0);
		try {
			//	Set new File Name
			StringBuffer pathName = new StringBuffer();
			pathName
			    .append(PAYROLL_CONSTANT)
				//	Payroll Account
				.append(MOrgInfo.get(getCtx(), pdl.getAD_Org_ID(), null).get_ValueAsString(ColumnsAdded.COLUMNNAME_BANAVIHCode))
				//	Accounting Date in format MM YYYY
				.append(new SimpleDateFormat("MMyyyy").format(pdl.getDateAcct()));
			
			m_NameFile = pathName.toString();
			
		} catch (Exception e) {
			s_log.log(Level.WARNING, "Could not delete ", e);
		}
		try {
			//	
			m_FileWriter = new FileWriter(file);
			//  write header
			m_NoLines ++;
			//  write lines
			Map<Integer, List<X_RV_HR_ProcessDetail>> det = getDetail().stream().collect(Collectors.groupingBy(X_RV_HR_ProcessDetail::getC_BPartner_ID));
			
			for (Map.Entry<Integer, List<X_RV_HR_ProcessDetail>> detail : det.entrySet()) {
				if (detail == null)
					continue;
				//	Verify Current Business Partner and Month
				if(m_Current_BPartner_ID != detail.getKey()) {
					writeLine();
					m_Current_Pdl = detail.getValue().get(0);
					m_Current_BPartner_ID = detail.getKey();
					m_CurrentAmt = detail.getValue().stream().map(x -> x.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
				} 
			}   
			//  write last line
			writeLine();
			//	Close
			m_FileWriter.flush();
			m_FileWriter.close();
			
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
		if(m_Current_Pdl == null)
			return;
		//	Process Business Partner
		Map<Integer,String> bpInfo = processBPartner(m_Current_Pdl.getC_BPartner_ID(), m_Current_Pdl.get_TrxName());
		//	Line
		if(bpInfo == null)
			return;
		StringBuffer line = new StringBuffer();
		//	Amount
		if(m_CurrentAmt == null)
			m_CurrentAmt = Env.ZERO;
		
		String currentAmt = m_CurrentAmt.toString().replace(",", ".").replace(".", "");
		if(currentAmt.length() > 11)
			currentAmt = currentAmt.substring(0, 24);
		
		//	New Line
		if(m_NoLines > 1)
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
			.append(currentAmt)
			.append(SEPARATOR)
			//	Employee Start Date
			.append(bpInfo.get(EM_START_DATE))
			.append(SEPARATOR)
			//	Employee End Date
			.append(bpInfo.get(EM_END_DATE));
		//	Write Line
		m_FileWriter.write(line.toString());
		m_NoLines ++;
	}
	
	/**
	 * Process Business Partner
	 * @author <a href="mailto:yamelsenih@gmail.com">Yamel Senih</a> 16/08/2014, 12:27:09
	 * @param p_C_BPartner_ID
	 * @param p_TrxName
	 * @return String []
	 */
	private Map<Integer,String>  processBPartner(int p_C_BPartner_ID, String p_TrxName) {
		Map<Integer,String> bpInfo = new HashMap<Integer, String>();

		//	Get Business Partner
		MBPartner bpartner = MBPartner.get(Env.getCtx(), p_C_BPartner_ID);
		//	Get Name
		String name = bpartner.getName();
		String name2 = bpartner.getName2();
		//	Valid Null
		if(name.isEmpty())
			name = "";
		if(name2.isEmpty())
			name2 = "";
		
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
		MHREmployee employee = MHREmployee.getActiveEmployee(Env.getCtx(), 
				bpartner.getC_BPartner_ID(), p_TrxName);
		//	Valid Employee
		if(employee == null)
			return null;
		//	Get Start Date
		String startDate = m_DateFormat.format(employee.getStartDate());
		String endDate = "";
		//	Get End Date
		if(employee.getEndDate() != null)
			endDate = m_DateFormat.format(employee.getEndDate());

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
	
	public String processValue(String value) {
		if(Util.isEmpty(value)) {
			return value;
		}
		//	
		return value.replaceAll("[+^:&áàäéèëíìïóòöúùñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ$,;*/áéíóúÁÉÍÓÚñÑ¿¡]", "");
	}
	
	@Override
	public String getFileName() {
		return m_NameFile;
	}
}

