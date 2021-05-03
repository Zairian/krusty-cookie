package krusty;

import spark.Request;
import spark.Response;

import javax.xml.transform.Result;
import java.sql.*;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import static krusty.Jsonizer.toJson;

public class Database {
	/**
	 * Modify it to fit your environment and then use this string when connecting to your database!
	 */
	private static final String jdbcString = "jdbc:mysql://puccini.cs.lth.se:3306/db03";
	private Connection conn = null;
	// For use with MySQL or PostgreSQL
	private static final String jdbcUsername = "db03";
	private static final String jdbcPassword = "xra597jn";

	public void connect() {
		try{
			conn = DriverManager.getConnection(
					jdbcString, jdbcUsername, jdbcPassword
			);
		}catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
		}
	}

	// TODO: Implement and change output in all methods below!

	public String getCustomers(Request req, Response res) {
		String sql = "SELECT customerName AS name, address FROM Customers";
		try(PreparedStatement ps = conn.prepareStatement(sql)){
			ResultSet rs = ps.executeQuery();
			return Jsonizer.toJson(rs, "customers");
		} catch(SQLException e){
			e.printStackTrace();
			return "0";
		}
	}

	public String getRawMaterials(Request req, Response res) {

		String sql = "SELECT ingType AS name, amount, unit FROM Ingredients";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();
			return Jsonizer.toJson(rs,"raw-materials");
		} catch(SQLException e) {
			e.printStackTrace();
		}

		return "0";
	}

	public String getCookies(Request req, Response res) {

		String sql = "SELECT cookieName AS name FROM Cookies";
		try(PreparedStatement ps = conn.prepareStatement(sql)){
			ResultSet rs = ps.executeQuery();
			return Jsonizer.toJson(rs, "cookies");
		} catch(SQLException e){
			e.printStackTrace();
			return "0";
		}
	}

	public String getRecipes(Request req, Response res) {
		return "{}";
	}

	public String getPallets(Request req, Response res) {
		return "{\"pallets\":[]}";
	}

	public String reset(Request req, Response res) {
		return "{}";
	}

	public String createPallet(Request req, Response res) {
		return "{}";
	}
}
