package jnode.dto;

import java.util.Calendar;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Планировщик (жалкая замена cron :-) )
 * 
 * @author Manjago
 *
 */
@DatabaseTable(tableName = "schedules")
public class Schedule {
	public static enum Type {
		DAILY, WEEKLY, MONTHLY, ANNUALLY;
	}

	@DatabaseField(dataType = DataType.ENUM_STRING, canBeNull = false, columnName = "type", defaultValue = "DAILY")
	private Type type;
	@DatabaseField(columnName = "details", defaultValue = "0")
	private Integer details;
	@DatabaseField(columnName = "jscript_id", foreign = true, canBeNull = false, uniqueIndexName = "lsched_idx")
	private Jscript jscript;

	public boolean isNeedExec(Calendar calendar) {

		if (calendar == null || getType() == null || getDetails() == null) {
			return false;
		}

		switch (getType()) {
		case DAILY:
			return true;
		case ANNUALLY:
			return checkDetails(calendar.get(Calendar.DAY_OF_YEAR));	
		case MONTHLY:
			return checkDetails(calendar.get(Calendar.DAY_OF_MONTH));
		case WEEKLY:
			return checkDetails(calendar.get(Calendar.DAY_OF_WEEK));

		default:
			return false;
		}
	}

	private boolean checkDetails(int fromCalendar){
		return getDetails() != null && getDetails().equals(fromCalendar);
	}
	
	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Integer getDetails() {
		return details;
	}

	public void setDetails(Integer details) {
		this.details = details;
	}

	public Jscript getJscript() {
		return jscript;
	}

	public void setJscript(Jscript jscript) {
		this.jscript = jscript;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Schedule [");
		if (type != null) {
			builder.append("type=");
			builder.append(type);
			builder.append(", ");
		}
		if (details != null) {
			builder.append("details=");
			builder.append(details);
			builder.append(", ");
		}
		if (jscript != null) {
			builder.append("jscript=");
			builder.append(jscript.getId());
		}
		builder.append("]");
		return builder.toString();
	}

}
