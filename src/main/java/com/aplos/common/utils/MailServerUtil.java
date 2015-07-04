package com.aplos.common.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.codec.digest.DigestUtils;

public class MailServerUtil {
	public static final String MAIL_SERVER = "";
	private final static String host = "";
	private final static String db = "";
	private final static String dbUsername = "";
	private final static String dbPassword = "";

	private static Connection conn = connect();

	public static void addAccount(String username, String password, String domain) throws Exception {

		long domainId = findDomain(domain);

		final String email = username + "@" + domain;
		if ( addressExists(email) ) {
			throw new Exception("Account already exists: " + email);
		}

		long accountid = insertAccount(domainId, email, password);;

		insertInbox(accountid);

	}

	private static void insertInbox(long accountid) throws Exception {
		final String addFolderSql = "insert into hm_imapfolders(folderaccountid," +
		" folderparentid, foldername, folderissubscribed," +
		" foldercreationtime, foldercurrentuid) " +
		"values ('" + accountid + "', -1, 'INBOX', 1, now(), 0)";

		try {
			conn.createStatement().executeUpdate( addFolderSql );
		} catch (SQLException e) {
			throw new Exception ("Could not add inbox folder: " + e.getMessage());
		}
	}

	private static long insertAccount(long domainId, String email, String password) throws Exception {
		// Password is salted sha256 stored as salt + hash.

		// TODO Should be random string for salt
		String salt = "9d4cc8";
		String encPass = salt + DigestUtils.shaHex( salt + password );

		// Add address to table
		final String addAccountSql =
			"insert into hm_accounts(" +
				"accountdomainid, " +
				"accountadminlevel," +
				"accountaddress, " +
				"accountpassword, " +
				"accountactive, " +
				"accountisad, " +
				"accountaddomain, " +
				"accountadusername, " +
				"accountmaxsize, " +
				"accountvacationmessageon, " +
				"accountvacationmessage, " +
				"accountvacationsubject, " +
				"accountpwencryption, " +
				"accountforwardenabled, " +
				"accountforwardaddress, " +
				"accountforwardkeeporiginal, " +
				"accountenablesignature, " +
				"accountsignatureplaintext, " +
				"accountsignaturehtml, " +
				"accountlastlogontime, " +
				"accountvacationexpires, " +
				"accountvacationexpiredate, " +
				"accountpersonfirstname, " +
				"accountpersonlastname" +

			") values (" +
				"'" + domainId + "' ," + 	// accountdomainid, " +
				0 + "," +			// accountadminlevel," +
				"'" + email + "'," +		// accountaddress, " +
				"'" + encPass + "'," +		// accountpassword, " +
				1 + "," +	// accountactive, " +
				0 + "," +	// accountisad, " +
				"''" + "," +	// accountaddomain, " +
				"''" + "," +	// accountadusername, " +
				0 + "," +	// accountmaxsize, " +
				0 + "," +	// accountvactionmessageon, " +
				"''" + "," +	// accountvacationmessage, " +
				"''" + "," +	// accountvacationsubject, " +
				3 + "," +	// accountpwencryption (3 == MD5)
				0 + "," +	// accountforwardenabled, " +
				"''" + "," +	// accountforwardaddress, " +
				0 + "," +	// accountforwardkeeporiginal, " +
				0 + "," +	// accountenablesignature, " +
				"''" + "," +	// accountsignatureplaintext, " +
				"''" + "," +	// accountsignaturehtml, " +
				"'0000-00-00'" + "," +	// accountlastlogintime, " +
				0 + "," +	// accountvacationexpires, " +
				"'2008-02-10'" + "," +	// accountvacationexpiredate, " +
				"''" + "," +	// accountpersonfirstname, " +
				"''" + 	// accountpersonlastname" +

			")";

		// Insert the new account then query it back to get the generated id
		try {
			conn.createStatement().executeUpdate( addAccountSql );
		} catch (SQLException e) {
			throw new Exception ("Could not add account: " + e.getMessage());
		}

		ResultSet results = conn.createStatement().executeQuery( "select accountid from hm_accounts where accountaddress='" + email + "'" );
		if (results.next()) {
			long accountid = results.getLong( "accountid" );
			return accountid;
		} else {
			throw new Exception("Inserted account but couldn't query it back.");
		}
	}

	private static long findDomain(String domain) throws Exception {
		long domainId = -1;
		ResultSet results = conn.createStatement().executeQuery( "select count(domainid) as `count`, domainid from hm_domains where domainname='" + domain + "'" );
		results.next();
		int count = results.getInt( "count" );
		domainId = results.getLong( "domainId" );

		if (count != 1 || domainId == -1) {
			throw new Exception("Could not find mail domain: " + domain);
		} else {
			return domainId;
		}
	}

	private static boolean addressExists(String email) throws SQLException {
		Statement s;
		s = conn.createStatement();
		ResultSet results = s.executeQuery( "select count(accountid) as `count` from hm_accounts where accountaddress='" + email + "'" );
		results.next();
		int count = results.getInt( "count" );
		return (count != 0);
	}

	private static Connection connect() {
        try {
			return DriverManager.getConnection (host + MAIL_SERVER + "/" + db, dbUsername, dbPassword);
		} catch( SQLException e ) {
			throw new RuntimeException( e );
		}
	}

}
