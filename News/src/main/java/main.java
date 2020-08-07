import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Map;
import java.util.Scanner;


import org.json.*;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import objects.PublicObjects;
import threads.Multicast;


public class main implements PublicObjects{

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, JSONException{
		Multicast mr = new Multicast();

		System.out.println("Type it:\n0 - To leave the multicast group;\n1 - To send a news item;\n2 - To classify a news item as fake;\n3 - To return to the multicast group;\n4 - To see the public key map\n5 - To see the reputation map");
		Scanner menu_scan = new Scanner(System.in);
		Scanner news_scan = new Scanner(System.in);
		while(true) {
			int i = menu_scan.nextInt();
			switch (i) {
				case 0:
					mr.leave_group();
					public_key_map.clear();
					break;
				case 1:
					//byte[] challenge = new byte[256];
					//ThreadLocalRandom.current().nextBytes(challenge);
					String challenge = news_scan.next();
					mr.unicast.send_sign_to_multicast(challenge);
					break;
				case 2:
					mr.fake_news_alert();
					break;
				case 3:
					mr.return_group();
					break;
				case 4:
					System.out.println(public_key_map);
					break;
				case 5:
					for(Map.Entry<Entry<Integer, String>, Integer> pair: news.entrySet()) {
						Integer port = pair.getKey().getKey();	
						String aux = pair.getKey().getValue();
						if(aux!="Fake") {
							if(news.get(new SimpleEntry(port, "Total")) != 0) {
								System.out.println("The port " + port + " has " + news.get(new SimpleEntry(port, "Fake")) + " votes to fake news and has sent "+ news.get(new SimpleEntry(port, "Total")) + " messages.");
							}
							else {
								System.out.println("The port " + port + " hasn't sent news yet");
							}
						}
					}
					
					break;
			}
		}
	}
}
