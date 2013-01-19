package jnode.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jnode.ftn.types.FtnAddress;
import jnode.jscript.JscriptExecutor;
import jnode.logger.Logger;
import jnode.main.threads.PollQueue;
import jnode.main.threads.TimerPoll;
import jnode.main.threads.Server;
import jnode.main.threads.TosserQueue;
//import jnode.main.threads.TosserQueue;
import jnode.orm.ORMManager;
import jnode.stat.threads.StatPoster;

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
				"fileecho.path"), STAT_ENABLE("stat.enable"), STAT_ECHOAREA(
				"stat.area"), JSCRIPT_ENABLE("jscript.enable");
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
		private final String version = "jNode/0.5.5beta8";

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
	 * @return папочку для складывания входящих файлов
	 */
	public static String getInbound() {
		return getProperty(Settings.BINKD_INBOUND.cfgline, null);
	}

	/**
	 * Папочка для поиска нодлиста
	 * 
	 * @return папочку для поиска нодлиста
	 */
	public static String getNodelistPath() {
		return getProperty(Settings.NODELIST_PATH.cfgline, "NODELIST");
	}

	public static String getNodelistIdx() {
		return getProperty(Settings.NODELIST_INDEX.cfgline, "nodelist.idx");
	}

	public static boolean isFileechoEnable() {
		String idx = settings.get(Settings.FILEECHO_ENABLE.cfgline);
        return idx != null;
    }

	public static String getFileechoPath() {
		return getProperty(Settings.FILEECHO_PATH.cfgline, getInbound());
	}

	public static String getTechArea() {
		return Main.getProperty(Main.Settings.STAT_ECHOAREA.getCfgline(),
				"jnode.local.stat");
	}

	public static boolean isStatisticEnable() {
		String idx = settings.get(Settings.STAT_ENABLE.cfgline);
        return idx != null;
    }

	public static boolean isJscriptEnable() {
		return settings.get(Settings.JSCRIPT_ENABLE.cfgline) != null;
	}

    Main() {

    }

    public Main(String configFile) {
        this();
        try {
            readConfig(configFile);
        } catch (Exception e) {
            logger.l1("Configuration check failed, exiting", e);
            System.exit(-1);
        }
    }

    void readConfig(String configFile) throws Exception {
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
            throw new Exception("You MUST send info.* in config");
        }
        try {
            info.address = new FtnAddress(addressline);
        } catch (NumberFormatException e) {
            throw new Exception(addressline + " is not valid FTN-address");
        }
    }

    public static void main(String[] args) {
		System.setProperty(LocalLog.LOCAL_LOG_LEVEL_PROPERTY, "INFO");
		if (args.length == 0) {
			System.out.println("Usage: $0 <config-file>");
			System.exit(-1);
		} else {
			new Main(args[0]);
			try {
				ORMManager.INSTANSE.start();
			} catch (Exception e) {
				logger.l1("Database init failed, exiting", e);
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
				logger.l3("Falling to default poll.delay/poll.period: 0/600");
			}
			try {
				port = Integer.valueOf(settings
						.get(Settings.BINKD_PORT.cfgline));
			} catch (RuntimeException e) {
				logger.l3("Falling to default binkd.port: 24554");
			}
			try {
				loglevel = Integer.valueOf(settings
						.get(Settings.LOG_LEVEL.cfgline));
			} catch (RuntimeException e) {
				logger.l3("Falling to default log.level: " + loglevel);
			}
			Logger.Loglevel = loglevel;
			if (getInbound() == null) {
				logger.l1("Inbound is not set - exiting");
				System.exit(-1);
			} else if ((!new File(getInbound()).exists())) {
				logger.l1("Inbound not exists - exiting");
				System.exit(-1);
			} else if (!new File(getInbound()).isDirectory()) {
				logger.l1("Inbound not a directory - exiting");
				System.exit(-1);
			} else if (!new File(getInbound()).canWrite()) {
				logger.l1("Inbound not writeable - exiting");
				System.exit(-1);
			}
			logger.l1(Main.info.version + " starting");
			if (settings.get(Settings.BINKD_SERVER.cfgline) != null) {
				Thread server = new Server(
						settings.get(Settings.BINKD_BIND.cfgline), port);
				server.start();
			}
			if (settings.get(Settings.BINKD_CLIENT.cfgline) != null) {
				logger.l4("Started client ( period " + period + " seconds )");
				new Timer().schedule(new TimerPoll(), delay * 1000,
						period * 1000);
			}
			logger.l4("Started PollQueue");
			new Timer().schedule(new PollerTask(), 10000, 10000);
			logger.l4("Started TossQueue");
			new Timer().schedule(new TosserTask(), 10000, 10000);
			new StatPoster();
			logger.l4("Started StatPoster");
			if (isJscriptEnable()){
				new JscriptExecutor();
				logger.l4("Started JscriptExecutor");
			}
			logger.l1("jNode is working now");
		}
	}

	private static final class TosserTask extends TimerTask {

		@Override
		public void run() {
			try {
				TosserQueue.INSTANSE.toss();
			} catch (RuntimeException e) {
				logger.l1("Error while tossing", e);
			}
		}

	}

	private static final class PollerTask extends TimerTask {

		@Override
		public void run() {
			PollQueue.INSTANSE.poll();
		}

	}
}
