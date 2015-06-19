package org.jnode.httpd.routes.get;

import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.jnode.httpd.dto.EchoareaCSV;

import com.j256.ormlite.dao.GenericRawResults;

import jnode.dto.Echoarea;
import jnode.orm.ORMManager;
import spark.Request;
import spark.Response;
import spark.Route;

public class EchoareaCSVRoute extends Route {
	private static final long MAX_CACHE_TIME = 3600000;
	private long latest = 0;

	public EchoareaCSVRoute() {
		super("/echoarea.csv");
	}

	@Override
	public Object handle(Request req, Response resp) {
		long now = new Date().getTime();
		List<EchoareaCSV> list = null;
		if (now - latest > MAX_CACHE_TIME) {
			try {
				list = new LinkedList<EchoareaCSV>();
				ORMManager.get(EchoareaCSV.class).delete();
				GenericRawResults<String[]> results = ORMManager
						.get(Echoarea.class)
						.getRaw("SELECT e.name,e.description,(SELECT count(id) FROM echomail "
								+ "WHERE echoarea_id=e.id) AS num,(SELECT max(date) FROM echomail "
								+ "WHERE echoarea_id=e.id) AS latest FROM echoarea e ORDER BY e.name;");
				latest = now;
				for (String[] row : results.getResults()) {
					EchoareaCSV csv = new EchoareaCSV();
					csv.setName(row[0]);
					csv.setDescription(row[1]);
					csv.setNum(new Long(row[2]));
					csv.setLatest(new Long(row[3]));
					ORMManager.get(EchoareaCSV.class).save(csv);
					list.add(csv);
				}
			} catch (SQLException e) {

			}
		} else {
			list = ORMManager.get(EchoareaCSV.class).getOrderAnd("name", true);
		}
		StringBuilder sb = new StringBuilder();
		for (EchoareaCSV csv : list) {
			sb.append(csv.getName() + "," + csv.getLatest() + ","
					+ csv.getNum() + "," + csv.getDescription() + "\r\n");
		}
		resp.type("text/plain");
		return sb.toString();
	}
}
