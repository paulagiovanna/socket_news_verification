package threads;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;

import javax.crypto.Cipher;

import objects.PublicObjects;

public class Multicast extends Thread implements PublicObjects{
	
	MulticastSocket receive_port;
	public Unicast unicast;
	int my_port, last_news_port;
	Boolean in_group = false;
	Boolean first_message = true;

	public Multicast() throws NoSuchAlgorithmException {		
		try {
			unicast = new Unicast();
			this.receive_port = new MulticastSocket(8080);
			//Coloca a porta no grupo do multicast.
			this.receive_port.joinGroup(InetAddress.getByName("224.0.0.1"));
			this.in_group = true;
		}
		catch(Exception e) {
			System.out.println("Error in Multicast method:");
			e.printStackTrace();
		}
		
		this.unicast.send_public_key_to_multicast_group();
		this.unicast.start();
		this.start();
		
	}
	
	@SuppressWarnings("static-access")
	public void run(){
		try {			
			this.last_news_port = 0;
			//deixa a porta sempre aberta para receber mensagens do grupo.
			while(true) {
				//cria a variavel que posteriormente vai receber a mensagem.
				//Por ser um datagrampacket de recebimento voce nao preenche ao criar. Ele vai ser preenchido posteriormente com as informações que chegaram.
				DatagramPacket dp = new DatagramPacket(new byte[1024], 1024);
				
				//se algo for recebido na porta 'receive_port' ele aloca o pacote na variavel anteriormente criada.
				this.receive_port.receive(dp);
				
				String data = new String(dp.getData());				
				if(first_message){
					System.out.println("Running Process: " + dp.getPort());
					PublicKey publick  = load_public_key(data);
					this.my_port = dp.getPort();
					
					this.public_key_map.put(this.my_port, publick);
					first_message = false;
					
					this.news.put(new SimpleEntry(dp.getPort(), "Total"), 0);
					this.news.put(new SimpleEntry(dp.getPort(), "Fake"), 0);
				}
				
				else {						
					if (!this.public_key_map.containsKey(dp.getPort())){
						System.out.println("\n" + dp.getPort() + " just arrived at group!");
						PublicKey publick  = load_public_key(data);
						this.public_key_map.put(dp.getPort(), publick);
						this.unicast.send_to_unicast(Base64.getMimeEncoder().encodeToString(this.public_key_map.get(this.my_port).getEncoded()), dp.getAddress(), dp.getPort());
						
						this.news.put(new SimpleEntry(dp.getPort(), "Total"), 0);
						this.news.put(new SimpleEntry(dp.getPort(), "Fake"), 0);
					}
					
					else {
						if(data.contains("I am leaving the multicast group")) {
							this.public_key_map.remove(dp.getPort());
						}
						else if(data.contains("FAKE NEWS")) {
							if (this.last_news_port != 0) {
								this.news.replace(new SimpleEntry(last_news_port, "Fake"), this.news.get(new SimpleEntry(last_news_port, "Fake")) + 1);
							}
							else {
								System.out.println("Has no news item to classify!");
							}
						}
						else{
							
							Signature clientSig = Signature.getInstance("RSA");
						    clientSig.initVerify(this.public_key_map.get(dp.getPort()));
						    clientSig.update(data.getBytes());

						    if (clientSig.verify(data.getBytes())) {
						        //Mensagem corretamente assinada
						        System.out.println("A Mensagem recebida foi assinada corretamente.");
						     } else {
						         //Mensagem não pode ser validada
						        System.out.println("A Mensagem recebida NÃO pode ser validada.");
						    }
							this.last_news_port = dp.getPort();
							this.news.replace(new SimpleEntry(last_news_port, "Total"), this.news.get(new SimpleEntry(last_news_port, "Total")) + 1);
						}
					}
						
					System.out.println("\n" + dp.getPort() + " says by multicast:\n" + data);
				}
			}
		}
		//Caso ocorra algum erro ele vai ser pego pelo catch.
		catch(Exception e){
			System.out.println("Error in run method:");
			//Printa o erro.
			e.printStackTrace();
		}
	}
	
	public static PublicKey load_public_key(String datastring) throws GeneralSecurityException, IOException{
		byte[] data = Base64.getMimeDecoder().decode((datastring.getBytes()));
		X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
		KeyFactory fact = KeyFactory.getInstance("RSA");
		return fact.generatePublic(spec);
	}
	
	public void leave_group() {
		if(in_group) {
			try {
				String message = "I am leaving the multicast group";
				this.unicast.send_to_multicast(message.getBytes());
				this.receive_port.leaveGroup(InetAddress.getByName("224.0.0.1"));
			} catch (IOException e) {
				System.out.println("Error in leave_group method:");
				e.printStackTrace();
			}

			this.in_group = false;
		}
		else {
			System.out.println("You aren't in multicast group!");
		}
	}
	
	public void return_group(){
		if(in_group) {
			System.out.println("You are already in that multicast group!");
		}
		else {
			try {
				this.receive_port.joinGroup(InetAddress.getByName("224.0.0.1"));
				this.first_message = true;
				this.unicast.send_public_key_to_multicast_group();
			} catch (IOException e) {
				System.out.println("Error in leave_group method:");
				e.printStackTrace();
			}
			this.in_group = true;
		}
	}

	public void fake_news_alert() {
		String message = "FAKE NEWS";
		this.unicast.send_to_multicast(message.getBytes());
	}
}