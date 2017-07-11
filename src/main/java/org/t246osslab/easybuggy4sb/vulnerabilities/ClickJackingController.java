package org.t246osslab.easybuggy4sb.vulnerabilities;

import java.io.IOException;
import java.util.Locale;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.owasp.esapi.ESAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ClickJackingController {

	private static final Logger log = LoggerFactory.getLogger(ClickJackingController.class);

	@Autowired
	MessageSource msg;

	@Autowired
	LdapTemplate ldapTemplate;
	
	@RequestMapping(value = "/admins/clickjacking", method = RequestMethod.GET)
	public ModelAndView doGet(ModelAndView mav, HttpServletRequest req, HttpServletResponse res, Locale locale) {
		mav.setViewName("clickjacking");
		mav.addObject("title", msg.getMessage("section.change.mail", null, locale));
		return mav;
	}

	@RequestMapping(value = "/admins/clickjacking", method = RequestMethod.POST)
	protected ModelAndView doPost(ModelAndView mav, HttpServletRequest req, HttpServletResponse res, Locale locale)
			throws IOException {
        mav.setViewName("clickjacking");
        mav.addObject("title", msg.getMessage("section.change.mail", null, locale));
        
		HttpSession session = req.getSession();
		if (session == null) {
			res.sendRedirect("/");
			return null;
		}
		String userid = (String) session.getAttribute("userid");
		String mail = StringUtils.trim(req.getParameter("mail"));
		if (!StringUtils.isBlank(mail) && isValidEmailAddress(mail)) {
			try {
				ModificationItem item = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
						new BasicAttribute("mail", mail));
				ldapTemplate.modifyAttributes(
						"uid=" + ESAPI.encoder().encodeForLDAP(userid.trim()) + ",ou=people,dc=t246osslab,dc=org",
						new ModificationItem[] { item });
				mav.addObject("mail", mail);

			} catch (Exception e) {
				log.error("Exception occurs: ", e);
				mav.addObject("errmsg", msg.getMessage("msg.mail.change.failed", null, locale));
				return doGet(mav, req, res, locale);
			}
		} else {
			mav.addObject("errmsg", msg.getMessage("msg.mail.format.is.invalid", null, locale));
			return doGet(mav, req, res, locale);
		}
		return mav;
	}

	public boolean isValidEmailAddress(String email) {
		boolean result = true;
		try {
			InternetAddress emailAddr = new InternetAddress(email);
			emailAddr.validate();
		} catch (AddressException e) {
			log.debug("Mail address is invalid: " + email, e);
			result = false;
		}
		return result;
	}
}
