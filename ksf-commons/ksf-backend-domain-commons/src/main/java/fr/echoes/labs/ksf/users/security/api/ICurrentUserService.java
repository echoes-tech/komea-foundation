package fr.echoes.labs.ksf.users.security.api;

public interface ICurrentUserService {
	
	/**
	 * Returns the current user login.
	 *
	 * @return the current user login
	 */
	String  getCurrentUserLogin();

}
