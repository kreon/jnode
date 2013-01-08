package jnode.robot;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.j256.ormlite.dao.GenericRawResults;

import jnode.dto.*;
import jnode.ftn.FtnTools;
import jnode.ftn.types.FtnMessage;
import jnode.orm.ORMManager;

/**
 * 
 * @author kreon
 * 
 */
public class AreaFix implements IRobot {
	private static final Pattern help = Pattern.compile("^%HELP$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern list = Pattern.compile("^%LIST$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern query = Pattern.compile("^%QUERY$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern add = Pattern.compile("^%?\\+?(\\S+)$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern rem = Pattern.compile("^%?\\-(\\S+)$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern rescan = Pattern.compile(
			"^%RESCAN (\\S+) (\\d+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern add_rescan = Pattern.compile(
			"^%?\\+?(\\S+) /r=(\\d+)$", Pattern.CASE_INSENSITIVE);

	@Override
	public void execute(FtnMessage fmsg) throws Exception {
		Link link = null;
		{
			List<Link> links = ORMManager.INSTANSE.getLinkDAO().getAnd(
					"ftn_address", "=", fmsg.getFromAddr().toString());
			if (links.isEmpty()) {
				FtnTools.writeReply(fmsg, "Access denied",
						"You are not in links of origin");
				return;
			}
			link = links.get(0);
		}
		if (!FtnTools.getOptionBooleanDefTrue(link, LinkOption.BOOLEAN_AREAFIX)) {
			FtnTools.writeReply(fmsg, "You are not welcome",
					"Sorry, AreaFix is off for you");
			return;
		}
		if (!link.getPaketPassword().equals(fmsg.getSubject())) {
			FtnTools.writeReply(fmsg, "Access denied", "Wrong password");
			return;
		}

		StringBuilder reply = new StringBuilder();
		for (String line : fmsg.getText().split("\n")) {
			line = line.toLowerCase();
			if (help.matcher(line).matches()) {
				FtnTools.writeReply(fmsg, "AreaFix help", help());
			} else if (list.matcher(line).matches()) {
				FtnTools.writeReply(fmsg, "AreaFix list", list(link));
			} else if (query.matcher(line).matches()) {
				FtnTools.writeReply(fmsg, "AreaFix query", query(link));
			} else {
				Matcher m = rem.matcher(line);
				if (m.matches()) {
					String area = m.group(1);
					reply.append(rem(link, area));
					continue;
				}
				m = add.matcher(line);
				if (m.matches()) {
					String area = m.group(1);
					reply.append(add(link, area));
					continue;
				}
				m = rescan.matcher(line);
				if (m.matches()) {
					String area = m.group(1);
					int num = Integer.valueOf(m.group(2));
					reply.append(rescan(link, area, num));
					continue;
				}
				m = add_rescan.matcher(line);
				if (m.matches()) {
					String area = m.group(1);
					int num = Integer.valueOf(m.group(2));
					reply.append(add(link, area));
					reply.append(rescan(link, area, num));
					continue;
				}
			}
		}
		if (reply.length() > 0) {
			FtnTools.writeReply(fmsg, "AreaFix reply", reply.toString());
		}
	}

	/**
	 * Отправляем %HELP
	 * 
	 * @return
	 */
	private String help() {
		return "Available commands:\n" + "%HELP - this message\n"
				+ "%LIST - list of available areas\n"
				+ "%QUERY - list of subscribed areas\n"
				+ "+echo.area - subscribe echo.area\n"
				+ "-echo.area - unsibscribe echo.area\n"
				+ "+echo.area /r=N - subscribe and rescan N messages\n"
				+ "%RESCAN echo.area N - rescan N messages";

	}

	/**
	 * Отправляем %LIST
	 * 
	 * @param link
	 * @return
	 * @throws SQLException
	 */
	private String list(Link link) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("Legend: * - subscribed\n\n========== List of echoareas ==========\n");
		String[] groups = FtnTools.getOptionStringArray(link,
				LinkOption.SARRAY_LINK_GROUPS);
		List<Echoarea> areas = ORMManager.INSTANSE.getEchoareaDAO()
				.getOrderAnd("name", true);
		for (Echoarea area : areas) {
			boolean denied = true;
			if (!"".equals(area.getGroup())) {
				for (String group : groups) {
					if (area.getGroup().equals(group)) {
						denied = false;
						break;
					}
				}
			} else {
				denied = false;
			}
			if (denied) {
				continue;
			}
			Subscription sub = ORMManager.INSTANSE.getSubscriptionDAO()
					.getFirstAnd("echoarea_id", "=", area.getId(), "link_id",
							"=", link.getId());

			if (sub != null) {
				sb.append("* ");
			} else {
				sb.append("  ");
			}
			sb.append(area.getName());
			if (area.getDescription().length() > 0) {
				sb.append(" - ");
				sb.append(area.getDescription());
			}
			sb.append('\n');
		}
		sb.append("========== List of echoareas ==========\n");
		return sb.toString();

	}

	/**
	 * Отправляем %QUERY
	 * 
	 * @param link
	 * @return
	 * @throws SQLException
	 */
	private String query(Link link) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("========== List of subscribed areas ==========\n");
		GenericRawResults<String[]> echoes = ORMManager.INSTANSE
				.getEchoareaDAO()
				.getRaw(String
						.format("SELECT a.name, a.description from echoarea a"
								+ " LEFT JOIN subscription s on (a.id=s.echoarea_id)"
								+ " WHERE s.link_id=%d ORDER BY a.name",
								link.getId()));
		for (String[] echo : echoes.getResults()) {
			sb.append("* ");
			sb.append(echo[0]);
			if (echo[1].length() > 0) {
				sb.append(" - ");
				sb.append(echo[1]);
			}
			sb.append('\n');
		}
		sb.append("========== List of subscribed areas ==========\n");
		return sb.toString();

	}

	private String add(Link link, String area) throws SQLException {
		StringBuilder sb = new StringBuilder();
		String like = area.replace("*", "%");
		String[] grps = FtnTools.getOptionStringArray(link,
				LinkOption.SARRAY_LINK_GROUPS);
		List<Echoarea> areas = ORMManager.INSTANSE.getEchoareaDAO().getAnd(
				"name", "~", like);
		if (areas.isEmpty()) {
			sb.append(area + " not found");
		} else {
			for (Echoarea earea : areas) {
				sb.append(earea.getName());
				Subscription sub = ORMManager.INSTANSE.getSubscriptionDAO()
						.getFirstAnd("echoarea_id", "=", earea.getId(),
								"link_id", "=", link.getId());
				if (sub != null) {
					sb.append(" already subscribed");
				} else {
					boolean denied = true;
					if (!"".equals(earea.getGroup())) {
						for (String group : grps) {
							if (earea.getGroup().equals(group)) {
								denied = false;
								break;
							}
						}
					} else {
						denied = false;
					}
					if (denied) {
						sb.append(" access denied");
					} else {
						sub = new Subscription();
						sub.setArea(earea);
						sub.setLink(link);
						ORMManager.INSTANSE.getSubscriptionDAO().save(sub);
						sb.append(" subscribed");
					}
				}
				sb.append('\n');
			}
		}
		sb.append('\n');
		return sb.toString();
	}

	private String rem(Link link, String area) throws SQLException {
		StringBuilder sb = new StringBuilder();
		String like = area.replace("*", "%");
		List<Echoarea> areas = ORMManager.INSTANSE.getEchoareaDAO().getAnd(
				"name", "~", like);
		if (areas.isEmpty()) {
			sb.append(area);
			sb.append(" not found");
		} else {
			for (Echoarea earea : areas) {
				sb.append(earea.getName());
				Subscription sub = ORMManager.INSTANSE.getSubscriptionDAO()
						.getFirstAnd("echoarea_id", "=", earea.getId(),
								"link_id", "=", link.getId());
				if (sub == null) {
					sb.append(" is not subscribed");
				} else {
					ORMManager.INSTANSE.getSubscriptionDAO().delete("link_id",
							"=", link, "echoarea_id", "=", earea);
					sb.append(" unsubscribed");
				}
				sb.append('\n');
			}
		}
		sb.append('\n');
		return sb.toString();
	}

	private String rescan(Link link, String area, int num) throws SQLException {
		StringBuilder sb = new StringBuilder();
		String like = area.replace("*", "%");
		List<Echoarea> areas = ORMManager.INSTANSE.getEchoareaDAO().getAnd(
				"name", "~", like);
		if (areas.isEmpty()) {
			sb.append(area);
			sb.append(" not found");
		} else {
			for (Echoarea earea : areas) {
				sb.append(earea.getName());
				Subscription sub = ORMManager.INSTANSE.getSubscriptionDAO()
						.getFirstAnd("echoarea_id", "=", earea.getId(),
								"link_id", "=", link.getId());
				if (sub == null) {
					sb.append(" is not subscribed");
				} else {
					List<Echomail> mails = ORMManager.INSTANSE.getEchomailDAO()
							.getOrderLimitAnd(num, "id", false, "echoarea_id",
									"=", earea);
					for (Echomail mail : mails) {
						ORMManager.INSTANSE.getEchomailAwaitingDAO().save(
								new EchomailAwaiting(link, mail));
					}
					sb.append(" rescanned " + mails.size() + " messages");
				}
				sb.append('\n');
			}
		}
		sb.append('\n');
		return sb.toString();
	}
}
