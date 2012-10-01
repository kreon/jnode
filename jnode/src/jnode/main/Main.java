package jnode.main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jnode.ftn.types.FtnAddress;
import jnode.logger.Logger;
import jnode.main.threads.PollQueue;
import jnode.main.threads.TimerPoll;
import jnode.main.threads.Server;
//import jnode.main.threads.TosserQueue;
import jnode.orm.ORMManager;

import com.j256.ormlite.logger.LocalLog;

/**
 * 
 * @author kreon
 * 
 */
public class Main {
	private static final Logger logger = Logger.getLogger(Main.class);
	private static final Hashtable<String, String> settings = new Hashtable<String, String>();
	public static final SystemInfo info = new SystemInfo();

	public static enum Settings {
		JDBC_URL("jdbc.url"), JDBC_USER("jdbc.user"), JDBC_PASS("jdbc.pass"), POLL_DELAY(
				"poll.delay"), POLL_PERIOD("poll.period"), INFO_SYSOP(
				"info.sysop"), INFO_LOCATION("info.location"), INFO_STATIONNAME(
				"info.stationname"), INFO_NDL("info.ndl"), INFO_ADDRESS(
				"info.address"), BINKD_BIND("binkp.bind"), BINKD_PORT(
				"binkp.port"), BINKD_INBOUND("binkp.inbound"), BINKD_CLIENT(
				"binkp.client"), BINKD_SERVER("binkp.server"), LOG_LEVEL(
				"log.level"), NODELIST_PATH("nodelist.path"), NODELIST_INDEX(
				"nodelist.index"), FILEECHO_ENABLE("fileecho.enable"), FILEECHO_PATH(
				"fileecho.path");
		private String cfgline;

		private Settings(String cfgline) {
			this.cfgline = cfgline;
		}

		public String getCfgline() {
			return this.cfgline;
		}
	}

	public static class SystemInfo {
		private String sysop;
		private String location;
		private String stationName;
		private FtnAddress address;
		private String NDL;
		private final String version = "jNode/0.4.6c";

		public String getSysop() {
			return sysop;
		}

		public String getLocation() {
			return location;
		}

		public String getStationName() {
			return stationName;
		}

		public FtnAddress getAddress() {
			return address;
		}

		public String getNDL() {
			return NDL;
		}

		public String getVersion() {
			return version;
		}

	}

	public static String getProperty(String propertyName, String defaultValue) {
		String idx = settings.get(propertyName);
		if (idx == null) {
			idx = defaultValue;
		}
		return idx;
	}

	/**
	 * Папочка для складывания входящих файликов
	 * 
	 * @return
	 */
	public static String getInbound() {
		return getProperty(Settings.BINKD_INBOUND.cfgline,
				System.getProperty("java.io.tmpdir"));
	}

	/**
	 * Папочка для поиска нодлиста
	 * 
	 * @return
	 */
	public static String getNodelistPath() {
		return getProperty(Settings.NODELIST_PATH.cfgline, "NODELIST");
	}

	public static String getNodelistIdx() {
		return getProperty(Settings.NODELIST_INDEX.cfgline, "nodelist.idx");
	}

	public static boolean isFileechoEnable() {
		String idx = settings.get(Settings.FILEECHO_ENABLE.cfgline);
		if (idx == null) {
			return false;
		}
		return true;
	}

	public static String getFileechoPath() {
		return getProperty(Settings.FILEECHO_PATH.cfgline, getInbound());
	}

	private Main(String configFile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(configFile));
			String line;
			Pattern config = Pattern
					.compile("^(\\S+)\\s*=?\\s*([^\r\n]+)[\r\n]*$");
			while ((line = br.readLine()) != null) {
				if (!line.startsWith("#")) {
					Matcher m = config.matcher(line);
					if (m.matches()) {
						settings.put(m.group(1), m.group(2));
					}
				}
			}
			br.close();
			info.sysop = settings.get(Settings.INFO_SYSOP.cfgline);
			info.location = settings.get(Settings.INFO_LOCATION.cfgline);
			info.stationName = settings.get(Settings.INFO_STATIONNAME.cfgline);
			info.NDL = settings.get(Settings.INFO_NDL.cfgline);
			String addressline = settings.get(Settings.INFO_ADDRESS.cfgline);
			if (info.sysop == null || info.location == null
					|| info.stationName == null || info.NDL == null
					|| addressline == null) {
				throw new Exception("Не заданы основные параметры системы");
			}
			try {
				info.address = new FtnAddress(addressline);
			} catch (NumberFormatException e) {
				throw new Exception(addressline + " не является FTN-адресом");
			}
		} catch (Exception e) {
			logger.l1("Не удалось настроить систему - выходим", e);
			System.exit(-1);
		}
	}

	public static void main(String[] args) {
		System.setProperty(LocalLog.LOCAL_LOG_LEVEL_PROPERTY, "INFO");
		if (args.length == 0) {
			System.out.println("Первым аргументом должен быть путь к конфигу");
			System.exit(-1);
		} else {
			new Main(args[0]);
			try {
				ORMManager.INSTANSE.start(Main.settings);
			} catch (Exception e) {
				e.printStackTrace();
				logger.l1("Не удалось инициализировать СУБД, выходим", e);
				System.exit(-1);
			}
			int delay = 0;
			int period = 600;
			int port = 24554;
			int loglevel = Logger.LOG_L4;
			try {
				delay = Integer.valueOf(settings
						.get(Settings.POLL_DELAY.cfgline));
				period = Integer.valueOf(settings
						.get(Settings.POLL_PERIOD.cfgline));
			} catch (RuntimeException e) {
				logger.l3("Не указаны poll.delay/poll.period, используем 0/600");
			}
			try {
				port = Integer.valueOf(settings
						.get(Settings.BINKD_PORT.cfgline));
			} catch (RuntimeException e) {
				logger.l3("Не указан binkd.port, используем 24554");
			}
			try {
				loglevel = Integer.valueOf(settings
						.get(Settings.LOG_LEVEL.cfgline));
			} catch (RuntimeException e) {
				logger.l3("Не указан log.level, используем " + loglevel);
			}
			Logger.Loglevel = loglevel;

			logger.l1(Main.info.version + " запускается");
			if (settings.get(Settings.BINKD_SERVER.cfgline) != null) {
				Thread server = new Server(
						settings.get(Settings.BINKD_BIND.cfgline), port);
				server.start();
				server = null;
			}
			if (settings.get(Settings.BINKD_CLIENT.cfgline) != null) {
				logger.l4("Запускается client ( раз в " + period + " секунд )");
				new Timer().schedule(new TimerPoll(), delay * 1000,
						period * 1000);
			}
			logger.l4("Запускается PollQueue");
			new Timer().schedule(new PollerTask(), 10000, 10000);
			// logger.debug("Запускается TossQueue");
			// new Timer().schedule(new TosserTask(), 10000, 10000);
			logger.l1("jNode запущен и работает");
		}
	}

	// private static final class TosserTask extends TimerTask {
	//
	// @Override
	// public void run() {
	// TosserQueue.INSTANSE.toss();
	// }
	//
	// }

	private static final class PollerTask extends TimerTask {

		@Override
		public void run() {
			logger.l4("Всего потоков: " + Thread.activeCount());
			PollQueue.INSTANSE.poll();
		}

	}
}
