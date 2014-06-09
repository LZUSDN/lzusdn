package com.lamoop.competition;

public class StringComparer {

	private static int MAX_ED = 3;
	private static int MAX_HMD = 20;
	private static MinED mined = new MinED();
	private static ImagePHash imgHash = new ImagePHash();

	public static boolean compare(String str1, String str2) {
		int med = mined.editDistance(str1, str2);
		if( med < MAX_ED && med != 0 ){
			return true;
		} /*else{
			int hmd = imgHash.hmDistance(str1, str2);
			if(hmd < MAX_HMD && hmd != 0){
				return true;
			}
		}*/
		return false;
	}

}
