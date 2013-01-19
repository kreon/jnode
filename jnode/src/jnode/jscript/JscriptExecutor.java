package jnode.jscript;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import jnode.dto.Schedule;
import jnode.logger.Logger;
import jnode.main.Main;
import jnode.orm.ORMManager;

/**
 * Запускатель пользовательских скриптов по расписанию
 * 
 * @author Manjago
 * 
 */
public class JscriptExecutor implements Runnable {

	public static final String HELPER = "helper";
	private static final long MILLISEC_IN_DAY = 86400000L;
	private static final Logger logger = Logger
			.getLogger(JscriptExecutor.class);

	public JscriptExecutor() {
		Calendar calendar = Calendar.getInstance(Locale.US);
		calendar.set(Calendar.DAY_OF_YEAR,
				calendar.get(Calendar.DAY_OF_YEAR) + 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 1);
		calendar.set(Calendar.SECOND, 0);
		Date showDate = calendar.getTime();
		Calendar now = Calendar.getInstance(Locale.US);
		long initialDelay = calendar.getTimeInMillis() - now.getTimeInMillis();
		if (initialDelay < 0) {
			initialDelay = 0;
		}

		logger.l3("First jscriptExecutor will run at " + showDate
				+ " and every 24h after");
		new ScheduledThreadPoolExecutor(1).scheduleAtFixedRate(this,
				initialDelay, MILLISEC_IN_DAY, TimeUnit.MILLISECONDS);
	}

	@Override
	public void run() {
		if (!Main.isJscriptEnable()) {
			return;
		}

		ScriptEngine engine = new ScriptEngineManager()
				.getEngineByName("javascript");

		Calendar now = Calendar.getInstance(Locale.US);
		IJscriptHelper helper = new JscriptHelper();

		List<Schedule> items = ORMManager.INSTANSE.getScheduleDAO().getAll();
		logger.l5(MessageFormat.format("{0} items in queue", items.size()));

		for (Schedule item : items) {
			if (item.isNeedExec(now)) {
				Bindings bindings = new SimpleBindings();
				bindings.put(HELPER, helper);

				try {
					executeScript(engine, item, bindings);
				} catch (ScriptException e) {
					logger.l2(MessageFormat.format("fail script {0} execution",
							item.getJscript().getId()), e);
				} catch (Exception e) {
					logger.l2(MessageFormat.format(
							"unexpected fail script {0} execution", item
									.getJscript().getId()), e);
				}
				logger.l5(MessageFormat.format("executed script {0}", item
						.getJscript().getId()));
			}
		}

	}

	private static void executeScript(ScriptEngine engine, Schedule item,
			Bindings bindings) throws ScriptException {
		if (item.getJscript() != null && item.getJscript().getId() != null) {

			String content = ORMManager.INSTANSE.getJscriptDAO()
					.getById(item.getJscript().getId()).getContent();
			if (content != null) {
				engine.eval(content, bindings);
			}

		}
	}

}
