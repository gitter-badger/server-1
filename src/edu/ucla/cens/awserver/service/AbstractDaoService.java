package edu.ucla.cens.awserver.service;

import edu.ucla.cens.awserver.dao.Dao;

/**
 * Provides hooks for subclasses needing access to a DAO.
 * 
 * @author selsky
 */
public abstract class AbstractDaoService implements Service {
	private Dao _dao;
	
	/**
	 * Creates an instance of this class using the supplied DAO.
	 * 
	 * @param dao
	 * @throws IllegalArgumentException if the passed-in DAO is null
	 */
	public AbstractDaoService(Dao dao) {
		if(null == dao) {
			throw new IllegalArgumentException("Cannot set a null DAO");
		}
		_dao = dao;
	}
	
	protected Dao getDao() {
		return _dao;
	}
}
