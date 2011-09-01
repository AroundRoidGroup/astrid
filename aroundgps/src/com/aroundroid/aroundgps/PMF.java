package com.aroundroid.aroundgps;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

/***
 * holds a persistence manager instance for the servlets 
 * @author Tomer
 *
 */
public final class PMF {
    private static final PersistenceManagerFactory pmfInstance =
        JDOHelper.getPersistenceManagerFactory("transactions-optional");

    private PMF() {
    	//empty constructor
    }

    /***
     * @return the persistence manager instance
     */
    public static PersistenceManagerFactory get() {
        return pmfInstance;
    }
}