/*******************************************************************************
 * Copyright 2012 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.query.impl;

import java.util.List;

import javax.sql.DataSource;

import org.ohmage.domain.Clazz;
import org.ohmage.domain.Document;
import org.ohmage.exception.DataAccessException;
import org.ohmage.query.IUserClassDocumentQueries;
import org.springframework.jdbc.core.SingleColumnRowMapper;

/**
 * This class contains all of the functionality for reading and writing 
 * information specific to user-class-document relationships.
 * 
 * @author John Jenkins
 */
public final class UserClassDocumentQueries extends Query implements IUserClassDocumentQueries {
	// Check if the user is a supervisor in any campaign with which the 
	// document is associated.
	private static final String SQL_EXISTS_USER_IS_PRIVILEGED_IN_ANY_CLASS_ASSOCIATED_WITH_DOCUMENT = 
		"SELECT EXISTS(" +
			"SELECT c.urn " +
			"FROM user u, class c, user_class uc, user_class_role ucr, " +
				"document d, document_class_role dcr " +
			// Switch on the username
			"WHERE u.username = ? " +
			// and the document's ID.
			"AND d.uuid = ? " +
			// Ensure that they are a supervisor in the campaign.
			"AND u.id = uc.user_id " +
			"AND c.id = uc.class_id " +
			"AND ucr.id = uc.user_class_role_id " +
			"AND ucr.role = '" + Clazz.Role.PRIVILEGED + "' " +
			// Ensure that the campaign is associated with the document.
			"AND d.id = dcr.document_id " +
			"AND c.id = dcr.class_id" +
		")";
	
	// Retrieves the list of documents visible to a user in a class.
	private static final String SQL_GET_DOCUMENTS_SPECIFIC_TO_CLASS_FOR_REQUESTING_USER = 
		"SELECT distinct(d.uuid) " +
		"FROM user u, class c, user_class uc, user_class_role ucr, " +
			"document d, document_role dr, document_privacy_state dps, document_class_role dclr " +
		"WHERE u.username = ? " +
		"AND c.urn = ? " +
		"AND dclr.document_id = d.id " +
		"AND dclr.document_role_id = dr.id " + 
		"AND dclr.class_id = c.id " +
		"AND dclr.class_id = uc.class_id " +
		"AND uc.user_id = u.id " +
		"AND uc.user_class_role_id = ucr.id " +
		"AND d.privacy_state_id = dps.id " +
		"AND (" +
			"(dps.privacy_state = '" + Document.PrivacyState.SHARED + "')" +
			" OR " +
			"(ucr.role = '" + Clazz.Role.PRIVILEGED + "')" +
			" OR " +
			"(dr.role = '" + Document.Role.OWNER + "')" +
		")";
	
	/**
	 * Creates this object.
	 * 
	 * @param dataSource The DataSource to use to query the database.
	 */
	private UserClassDocumentQueries(DataSource dataSource) {
		super(dataSource);
	}
	
	/**
	 * Gathers the unique identifiers for all of the documents associated with
	 * a class.
	 * 
	 * @param username The username of the requesting user.
	 * 
	 * @param classId The class' unique identifier.
	 * 
	 * @return A list of the documents associated with a class. The list may be
	 * 		   empty but never null.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	public List<String> getVisibleDocumentsToUserInClass(String username, String classId) throws DataAccessException {
		try {
			return getJdbcTemplate().query(
					SQL_GET_DOCUMENTS_SPECIFIC_TO_CLASS_FOR_REQUESTING_USER, 
					new Object[] { username, classId },
					new SingleColumnRowMapper<String>());
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException("Error executing SQL '" + SQL_GET_DOCUMENTS_SPECIFIC_TO_CLASS_FOR_REQUESTING_USER + " with parameters: " +
					username + ", " + classId, e);
		}
	}
	
	/**
	 * Retrieves whether or not the user is privileged any of the classes 
	 * associated with the document.
	 * 
	 * @param username The username of the user.
	 * 
	 * @param documentId The unique identifier of the document.
	 * 
	 * @return Returns true if the user is privileged in any class that is 
	 * 		   associated with the campaign.
	 */
	public Boolean getUserIsPrivilegedInAnyClassAssociatedWithDocument(String username, String documentId) throws DataAccessException {
		try {
			return getJdbcTemplate().queryForObject(
					SQL_EXISTS_USER_IS_PRIVILEGED_IN_ANY_CLASS_ASSOCIATED_WITH_DOCUMENT, 
					new Object[] { username, documentId }, 
					Boolean.class);
		}
		catch(org.springframework.dao.DataAccessException e) {
			errorExecutingSql(SQL_EXISTS_USER_IS_PRIVILEGED_IN_ANY_CLASS_ASSOCIATED_WITH_DOCUMENT, e, username, documentId);
			return null;
		}
	}
}
