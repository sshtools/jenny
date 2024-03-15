package com.sshtools.jenny.api;

public class Words {

	public static String identifier(String val) {
		return spaceOnCaseChange(val).replaceAll("[^A-Za-z0-9\\s]+", "").replace(' ', '-').toLowerCase();
	}
	
	public static String english(String val) {
		return capitalizeFirst(spaceOnCaseChange(val, true));
	}

	public static String capitalizeFirst(String text) {
		return text == null || text.length() == 0 ? null
				: (text.length() == 1 ? text : (Character.toUpperCase(text.charAt(0)) + text.substring(1)));
	}
		
	public static String spaceOnCaseChange(String fieldName) {
		return spaceOnCaseChange(fieldName, false);
	}

	public static String spaceOnCaseChange(String fieldName, boolean capitals) {
		if (fieldName == null || fieldName.length() == 0)
			return fieldName;
		char[] strChr = fieldName.toCharArray();
		boolean upper = Character.isUpperCase(strChr[0]);
		StringBuilder b = new StringBuilder();
		b.append(Character.toUpperCase(strChr[0]));
		for (int i = 1; i < strChr.length; i++) {
			boolean nu = Character.isUpperCase(strChr[i]);
			if (upper != nu && nu) {
				b.append(' ');
			}
			upper = nu;
			if (capitals)
				b.append(strChr[i]);
			else
				b.append(Character.toLowerCase(strChr[i]));
		}
		return b.toString();
	}
}
