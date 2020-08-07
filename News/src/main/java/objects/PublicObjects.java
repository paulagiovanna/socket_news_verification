package objects;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public interface PublicObjects {
	Map<Integer, PublicKey> public_key_map = new HashMap<Integer, PublicKey>();
	Map<Entry<Integer, String>, Integer> news = new HashMap<Entry<Integer, String>, Integer>();
}
