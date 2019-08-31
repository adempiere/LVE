package org.erpya.lve.attendance.imp;

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
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.AbstractBatchImport;
import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

import java.util.Properties;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;

/**
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 * 		<a href="https://github.com/adempiere/adempiere/issues/1295">
 * 		@see FR [ 1295 ] File Loader can be improvement from connection</a>
 */
public class TopPontoImporter extends AbstractBatchImport {
	
	/**
	 * Default constructor
	 * @param ctx
	 */
	public TopPontoImporter(Properties ctx) {
		this.ctx = ctx;
	}
	
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(TopPontoImporter.class);
	
	/**	Database connection	*/
	private Database database;
	/**	Employee			*/
	private HashMap<Integer, String> employeeMap = new HashMap<Integer, String>();
	/**	Leave Type			*/
	private HashMap<Integer, String> leaveTypeMap = new HashMap<Integer, String>();
	/**	Movements			*/
	private HashMap<String, EmployeeMovement> movementMap = new HashMap<String, EmployeeMovement>();
	private Properties ctx;
	
	@Override
	public String testConnetion() {
		String error = null;
		try {
			String topPontoDB = MSysConfig.getValue("TOPPONTO_DB_NAME", Env.getAD_Client_ID(ctx));
			if(topPontoDB != null) {
				database = DatabaseBuilder.open(new File(topPontoDB));
			} else {
				throw new AdempiereException("@AD_SysConfig_ID@ TOPPONTO_DB_NAME @NotFound@");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return error;
	}
	
	@Override
	public List<String> getData() {
		if(database == null) {
			String error = testConnetion();
			if(error != null) {
				return null;
			}
		}
		//	Create List
		List<String> data = null;
		//	verify for null
		if(database != null) {
			data = new ArrayList<String>();
			try {
				Table leaveType = database.getTable("MotivosAbono");
				Table employee = database.getTable("Funcionarios");
				Table movements = database.getTable("Bilhetes");
				//	For Leaves
				for(Row row : leaveType) {
					leaveTypeMap.put(row.getInt("CodMotivo"), row.getString("Descricao"));
				}
				//	For Employees
				for(Row row : employee) {
					employeeMap.put(row.getInt("CodFunc"), row.getString("Matricula"));
				}
				//	For Movements
				for(Row row : movements) {
					int employeeId = row.getInt("CodFunc");
					int leaveTypeId = row.getInt("CodMotivo");
					Date leaveDate = row.getDate("Data");
					Date leaveTime = row.getDate("Ausencia_D");
					if(leaveDate == null
							|| leaveTime == null) {
						continue;
					}
					String key = employeeId + "|" + leaveTypeId;
					//	Get
					Calendar date = Calendar.getInstance();
					date.setTime(leaveTime);
					//	Get Hour
					double hours = date.get(Calendar.HOUR);
					//	Get Minute
					double minutes = date.get(Calendar.MINUTE) / 60f;
					//	
					EmployeeMovement movement = movementMap.get(employeeId + "|" + leaveTypeId);
					if(movement != null) {
						movement.addQty(hours + minutes);
					} else {
						movement = new EmployeeMovement(employeeMap.get(employeeId), leaveTypeId, hours + minutes);
					}
					//	Set to hash map
					movementMap.put(key, movement);
				}
				//	Add to Data
				for(Entry<String, EmployeeMovement> entry : movementMap.entrySet()) {
					data.add(entry.getValue().toString());
					log.fine(entry.getValue().toString());
				}
			} catch (IOException e) {
				log.severe(e.getLocalizedMessage());
			}
		}
		return data;
	}
	
	/**
	 *	Class for movements
	 */
	public class EmployeeMovement {
		
		/**	Employee Value	*/
		private String employeeValue;
		/**	Leave Type		*/
		private int leaveType;
		/**	Quantity		*/
		private double movementQty;
		
		/**
		 * Default constructor
		 * @param employeeValue
		 * @param leaveType
		 * @param movementQty
		 */
		public EmployeeMovement(String employeeValue, int leaveType, double movementQty) {
			this.employeeValue = employeeValue;
			this.leaveType = leaveType;
			this.movementQty = movementQty;
		}
		
		/**
		 * Add Movement Quantity
		 * @param movementQty
		 */
		public void addQty(double movementQty) {
			this.movementQty += movementQty;
		}
		
		@Override
		public String toString() {
			return employeeValue + "|" + leaveType + "|" + leaveTypeMap.get(leaveType) + "|" + movementQty;
		}
	}
	
}	//	GenericBatchImport