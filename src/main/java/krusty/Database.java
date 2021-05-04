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

		String sql = "SELECT Recipes.cookie, Recipes.ingType AS raw_material, Recipes.amount, Ingredients.unit" +
				"FROM Recipes INNER JOIN Ingredients ON Recipes.ingType = Ingredients.ingType" +
				"ORDER BY Recipes.cookie DESC";

		try(PreparedStatement ps = conn.prepareStatement(sql)){
			ResultSet rs = ps.executeQuery();
			return Jsonizer.toJson(rs, "recipes");
		} catch(SQLException e){
			e.printStackTrace();
			return "0";
		}
	}

	public String getPallets(Request req, Response res) {

		String sql = "SELECT palletLabel AS id, cookie, packingDate AS production_date, Orders.customer, blocked" +
				"FROM Pallets INNER JOIN Orders ON Pallets.palletLabel = Orders.palletLabel" +
				"ORDER BY production_date DESC";

		ArrayList<String> queryValues = new ArrayList<>();

		String producedAfterDate = req.queryParams("from");
		String producedBeforeDate = req.queryParams("to");
		String selectCookie = req.queryParams("cookie");
		String isBlocked = req.queryParams("blocked");

		if(producedAfterDate != null){
			sql += "WHERE production_date > ?";
			queryValues.add(producedAfterDate);
		}if(producedBeforeDate != null){
			sql += "WHERE production_date < ?";
			queryValues.add(producedBeforeDate);
		}if(selectCookie != null){
			sql += "WHERE cookie = ?";
			queryValues.add(selectCookie);
		}if(isBlocked != null){
			sql += "WHERE blocked = ?";
			queryValues.add(isBlocked);
		}

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
		return "ok";
	}

	public String reset(Request req, Response res){
		String createSchema = null;
		String initialData = null;
		try{
			Statement s = conn.createStatement();
			s.addBatch("set foreign_key_checks = 0;");
			s.addBatch("TRUNCATE TABLE Recipes");
			s.addBatch("TRUNCATE TABLE Ingredients");
			s.addBatch("TRUNCATE TABLE Cookies");
			s.addBatch("TRUNCATE TABLE Pallets");
			s.addBatch("TRUNCATE TABLE PalletCounter");
			s.addBatch("TRUNCATE TABLE Orders");
			s.addBatch("TRUNCATE TABLE Customers");
			s.addBatch("set foreign_key_checks = 1;");
			s.executeBatch();

			initialData = IOUtils.toString(new FileInputStream("src/main/resources/public/initial-data.sql"));
		}catch(Exception e){
			e.printStackTrace();
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
		if(req.queryParams("cookie") != null){
			String cookie = req.queryParams("cookie");
			return  createPallet(cookie);
		} else{
			return anythingToJson("unknown cookie", "status");
		}
	}

	protected String createPallet(String cookie){
		try{
			String sqlInsert = "INSERT INTO Pallets (cookie, curLoc, packingDate, blocked) values(?, 'Factory', CURDATE(), false)";
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
		return "0";
	}
}
