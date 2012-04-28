package org.ohmage.request.user;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.ohmage.annotator.Annotator.ErrorCode;
import org.ohmage.exception.ServiceException;
import org.ohmage.exception.ValidationException;
import org.ohmage.request.InputKeys;
import org.ohmage.request.Request;
import org.ohmage.service.UserServices;
import org.ohmage.validator.UserValidators;

/**
 * <p>Creates a new user via self-registration. The user's account is disabled
 * and must be enabled through /user/activation. A link will be sent to the 
 * user-supplied email address.</p>
 * <table border="1">
 *   <tr>
 *     <td>Parameter Name</td>
 *     <td>Description</td>
 *     <td>Required</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#USER}</td>
 *     <td>The username for the new user.</td>
 *     <td>true</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#PASSWORD}</td>
 *     <td>The password for the new user.</td>
 *     <td>true</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#EMAIL_ADDRESS}</td>
 *     <td>The user's email address.</td>
 *     <td>true</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#CAPTCHA_CHALLENGE}</td>
 *     <td>The challenge string generated by the ReCaptcha server.</td>
 *     <td>true</td>
 *   </tr>
 *   <tr>
 *     <td>{@value org.ohmage.request.InputKeys#CAPTCHA_RESPONSE}</td>
 *     <td>The user's "answer" to the captcha.</td>
 *     <td>true</td>
 *   </tr>
 * </table>
 * 
 * @author John Jenkins
 */
public class UserRegistrationRequest extends Request {
	private static final Logger LOGGER = 
			Logger.getLogger(UserRegistrationRequest.class);
	
	private final String username;
	private final String password;
	private final String emailAddress;
	private final String captchaChallenge;
	private final String captchaResponse;
	
	private final String remoteAddr;
	
	/**
	 * Creates a user registration request.
	 * 
	 * @param httpRequest The HttpServletRequest that contains the required 
	 * 					  parameters for creating this request.
	 */
	public UserRegistrationRequest(final HttpServletRequest httpRequest) {
		super(httpRequest);
		
		String tUsername = null;
		String tPassword = null;
		String tEmailAddress = null;
		String tCaptchaChallenge = null;
		String tCaptchaResponse = null;
		String tRemoteAddr = null;
		
		if(! isFailed()) {
			LOGGER.info("Creating a user registration request.");
			
			try {
				String[] t;
				t = getParameterValues(InputKeys.USERNAME);
				if(t.length > 1) {
					throw new ValidationException(
							ErrorCode.USER_INVALID_USERNAME,
							"Multiple username parameters were given: " +
								InputKeys.USERNAME);
				}
				else if(t.length == 1) {
					tUsername = UserValidators.validateUsername(t[0]);
					
					if(tUsername == null) {
						throw new ValidationException(
								ErrorCode.USER_INVALID_USERNAME,
								"The username is invalid: " + t[0]);
					}
				}
				else {
					throw new ValidationException(
							ErrorCode.USER_INVALID_USERNAME,
							"Missing the required username for the new user: " + 
								InputKeys.USERNAME);
				}
				
				t = getParameterValues(InputKeys.PASSWORD);
				if(t.length > 1) {
					throw new ValidationException(
							ErrorCode.USER_INVALID_PASSWORD,
							"Multiple password parameters were given: " +
								InputKeys.PASSWORD);
				}
				else if(t.length == 1) {
					tPassword = 
							UserValidators.validatePlaintextPassword(t[0]);
					
					if(tPassword == null) {
						throw new ValidationException(
								ErrorCode.USER_INVALID_PASSWORD,
								"The password is invalid: " + t[0]);
					}
				}
				else {
					throw new ValidationException(
							ErrorCode.USER_INVALID_PASSWORD,
							"Missing the required password for the new user: " + 
								InputKeys.PASSWORD);
				}
				
				t = getParameterValues(InputKeys.EMAIL_ADDRESS);
				if(t.length > 1) {
					throw new ValidationException(
							ErrorCode.USER_INVALID_EMAIL_ADDRESS,
							"Multiple email address parameters were given: " +
								InputKeys.EMAIL_ADDRESS);
				}
				else if(t.length == 1) {
					tEmailAddress = UserValidators.validateEmailAddress(t[0]);
					
					if(tEmailAddress == null) {
						throw new ValidationException(
								ErrorCode.USER_INVALID_EMAIL_ADDRESS,
								"The email address is invalid: " + t[0]);
					}
				}
				else {
					throw new ValidationException(
							ErrorCode.USER_INVALID_EMAIL_ADDRESS,
							"The email address is missing: " + 
								InputKeys.EMAIL_ADDRESS);
				}
				
				t = getParameterValues(InputKeys.CAPTCHA_CHALLENGE);
				if(t.length > 1) {
					throw new ValidationException(
							ErrorCode.SERVER_INVALID_CAPTCHA,
							"Multiple captcha challenge keys were given: " +
								InputKeys.CAPTCHA_CHALLENGE);
				}
				else if(t.length == 1) {
					tCaptchaChallenge = t[0];
				}
				else {
					throw new ValidationException(
							ErrorCode.SERVER_INVALID_CAPTCHA,
							"The captcha response key was missing: " +
								InputKeys.CAPTCHA_CHALLENGE);
				}
				
				t = getParameterValues(InputKeys.CAPTCHA_RESPONSE);
				if(t.length > 1) {
					throw new ValidationException(
							ErrorCode.SERVER_INVALID_CAPTCHA,
							"Multiple captcha response keys were given: " +
								InputKeys.CAPTCHA_RESPONSE);
				}
				else if(t.length == 1) {
					tCaptchaResponse = t[0];
				}
				else {
					throw new ValidationException(
							ErrorCode.SERVER_INVALID_CAPTCHA,
							"The captcha challenge key was missing: " +
								InputKeys.CAPTCHA_RESPONSE);
				}
				
				tRemoteAddr = httpRequest.getRemoteAddr();
			}
			catch(ValidationException e) {
				e.failRequest(this);
				e.logException(LOGGER);
			}
		}
		
		username = tUsername;
		password = tPassword;
		emailAddress = tEmailAddress;
		captchaChallenge = tCaptchaChallenge;
		captchaResponse = tCaptchaResponse;
		remoteAddr = tRemoteAddr;
	}

	/**
	 * Verifies the captcha, ensures the user doesn't already exist, creates 
	 * the user and a registration entry, and sends an email to the user.
	 */
	@Override
	public void service() {
		LOGGER.info("Servicing a user registration request.");
		
		try {
			// FIXME: Verify that self-registration is allowed.
			
			LOGGER.info("Verifying the captcha information.");
			UserServices.instance().verifyCaptcha(
					remoteAddr, 
					captchaChallenge, 
					captchaResponse);
			
			LOGGER.info("Verifying that the username isn't already taken.");
			UserServices.instance().checkUserExistance(username, false);
			
			LOGGER.info("Creating the confirmation session.");
			UserServices.instance().createUserRegistration(username, password, emailAddress);
		}
		catch(ServiceException e) {
			e.failRequest(this);
			e.logException(LOGGER);
		}
	}

	/**
	 * Responds to the registration request. This just indicates that the 
	 * server succeeded and then the user must go to their email to activate 
	 * the account.
	 */
	@Override
	public void respond(
			final HttpServletRequest httpRequest,
			final HttpServletResponse httpResponse) {
		
		LOGGER.info("Responding to a user registration request.");
		
		super.respond(httpRequest, httpResponse, new JSONObject());
	}
}