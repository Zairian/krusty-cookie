package krusty;

import spark.Request;
import spark.Response;

import javax.xml.transform.Result;
import java.io.*;
import java.sql.*;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import static krusty.Jsonizer.anythingToJson;
import static krusty.Jsonizer.toJson;
import spark.utils.IOUtils;

public class Database {
	/**
	 * Modify it to fit your environment and then use this string when connecting to your database!
	 */
	private static final String jdbcString = "jdbc:mysql://puccini.cs.lth.se:3306/db03?rewriteBatchedStatements=true&allowMultiQueries=true";
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
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VorError: " + e.getErrorCode());
			return anythingToJson("error", "status");
		}
	}

	public String getRawMaterials(Request req, Response res) {

		String sql = "SELECT ingType AS name, amount, unit FROM Ingredients";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();
			return Jsonizer.toJson(rs,"raw-materials");
		} catch(SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VorError: " + e.getErrorCode());
			return anythingToJson("error", "status");
		}
	}

	public String getCookies(Request req, Response res) {

		String sql = "SELECT cookieName AS name FROM Cookies";
		try(PreparedStatement ps = conn.prepareStatement(sql)){
			ResultSet rs = ps.executeQuery();
			return Jsonizer.toJson(rs, "cookies");
		} catch(SQLException e){
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VorError: " + e.getErrorCode());
			return anythingToJson("error", "status");
		}
	}

	public String getRecipes(Request req, Response res) {

		String sql = "SELECT Recipes.cookie, Recipes.ingType AS raw_material, Recipes.amount, Ingredients.unit" +
				"FROM Recipes INNER JOIN Ingredients ON Recipes.ingType = Ingredients.ingType" +
				"ORDER BY Recipes.cookie DESC";

		try(PreparedStatement ps = conn.prepareStatement(sql)){
			ResultSet rs = ps.executeQuery();
			return Jsonizer.toJson(rs, "recipes");
		} catch(SQLException e){
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VorError: " + e.getErrorCode());
			return anythingToJson("error", "status");
		}
	}

	public String getPallets(Request req, Response res) {

		String sql = "SELECT Pallets.palletLabel AS id, cookie, packingDate AS production_date, Orders.customer, IF(blocked, 'yes', 'no') AS blocked" +
				" FROM Pallets LEFT OUTER JOIN Orders ON Pallets.palletLabel = Orders.palletLabel ";

		ArrayList<String> queryValues = new ArrayList<>();

		//Produced after date
		if(req.queryParams("from") != null){
			if(!queryValues.isEmpty()){
				sql += "AND packingDate > ? ";
			} else{
				sql += "WHERE packingDate > ? ";
			}
			queryValues.add(req.queryParams("from"));
		}
		//Produced before date
		if(req.queryParams("to") != null){
			if(!queryValues.isEmpty()){
				sql += "AND packingDate < ? ";
			} else{
				sql += "WHERE packingDate < ? ";
			}
			queryValues.add(req.queryParams("to"));
		}
		//Selected cookie
		if(req.queryParams("cookie") != null){
			if(!queryValues.isEmpty()){
				sql += "AND cookie = ? ";
			} else{
				sql += "WHERE cookie = ? ";
			}
			queryValues.add(req.queryParams("cookie"));
		}
		//Blocked pallets
		if(req.queryParams("blocked") != null){
			if(!queryValues.isEmpty()){
				sql += "AND IF(blocked, 'yes', 'no') = ? ";
			} else{
				sql += "WHERE IF(blocked, 'yes', 'no') = ? ";
			}
			queryValues.add(req.queryParams("blocked"));
		}
		sql += "ORDER BY packingDate DESC";
		try(PreparedStatement ps = conn.prepareStatement(sql)){
			for(int i = 0; i < queryValues.size(); i++){
				ps.setString(i + 1, queryValues.get(i));
			}
			ResultSet rs = ps.executeQuery();
			return Jsonizer.toJson(rs, "pallets");
		} catch(SQLException e){
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VendorError: " + e.getErrorCode());
			
		}

		return anythingToJson("error", "status");
	}

	public String reset(Request req, Response res){
		String initialData = null;
		try{
			Statement s = conn.createStatement();
			s.addBatch("set foreign_key_checks = 0");
			s.addBatch("TRUNCATE TABLE Recipes");
			s.addBatch("TRUNCATE TABLE Ingredients");
			s.addBatch("TRUNCATE TABLE Cookies");
			s.addBatch("TRUNCATE TABLE Pallets");
			s.addBatch("TRUNCATE TABLE PalletCounter");
			s.addBatch("TRUNCATE TABLE Orders");
			s.addBatch("TRUNCATE TABLE Customers");
			s.addBatch("set foreign_key_checks = 1");
			s.executeBatch();

		}catch(SQLException ex){
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
		}
		try{
			initialData = IOUtils.toString(new FileInputStream("src/main/resources/public/initial-data.sql"));
		} catch(Exception ex){
			ex.printStackTrace();
		}
		try{

			PreparedStatement ps = conn.prepareStatement(initialData);
			ps.execute();
		}catch(SQLException ex){
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
		}

		return anythingToJson("ok", "status");
	}

	public String createPallet(Request req, Response res) {
		if (req.queryParams("cookie") != null) {
			String cookie = req.queryParams("cookie");
			return createPallet(cookie);
		} else {
			return anythingToJson("error", "status");
		}
	}

	protected String createPallet(String cookie){
		try{
			String sqlInsert = "INSERT INTO Pallets (cookie, curLoc, packingDate, blocked) values(?, 'Factory', NOW(), false)";
			PreparedStatement ps = conn.prepareStatement(sqlInsert);
			ps.setString(1, cookie);
			ps.execute();

			String sqlIdentity = "SELECT LAST_INSERT_ID()";
			ps = conn.prepareStatement(sqlIdentity);
			ResultSet rs = ps.executeQuery();

			String sqlIngredients = "UPDATE Ingredients INNER JOIN Recipes ON Ingredients.ingType = Recipes.ingType SET Ingredients.amount = Ingredients.amount - (54*Recipes.amount) WHERE cookie = ?";
			ps = conn.prepareStatement(sqlIngredients);
			ps.setString(1, cookie);
			ps.execute();

			return anythingToJson("ok", "status") + toJson(rs, "id");
		}catch(SQLException ex) {
			if(ex.getSQLState().startsWith("23")){
				System.out.println("SQLException: " + ex.getMessage());
				System.out.println("SQLState: " + ex.getSQLState());
				System.out.println("VendorError: " + ex.getErrorCode());
				return anythingToJson("unknown cookie", "status");
			} else{
				System.out.println("SQLException: " + ex.getMessage());
				System.out.println("SQLState: " + ex.getSQLState());
				System.out.println("VendorError: " + ex.getErrorCode());

				return anythingToJson("error", "status");
			}
		}
	}
}