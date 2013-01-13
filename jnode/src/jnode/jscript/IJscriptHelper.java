package jnode.jscript;

public interface IJscriptHelper {
	public interface Version {
		int getMajor();

		int getMinor();
	}

	Version getVersion();

	void writeFileToEchoarea(String echoArea, String subject, String filename);
}
